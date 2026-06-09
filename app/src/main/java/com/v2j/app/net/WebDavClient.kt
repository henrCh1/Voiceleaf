package com.v2j.app.net

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Credentials
import okhttp3.HttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

/**
 * Uploads one weekly Markdown file to Nutstore (坚果云) via WebDAV, overwriting in place.
 *
 * Nutstore does NOT auto-create parent directories on PUT (it returns 409 AncestorsNotFound),
 * so we MKCOL each ancestor folder top-down first. URL path segments are added one by one so
 * OkHttp encodes spaces / Chinese / parentheses correctly — never string-concatenate the URL.
 */
class WebDavClient(private val client: OkHttpClient) {

    private fun davUrl(segments: List<String>): HttpUrl {
        val b = HttpUrl.Builder().scheme("https").host("dav.jianguoyun.com").addPathSegment("dav")
        segments.forEach { b.addPathSegment(it) }
        return b.build()
    }

    /** Create every ancestor directory in basePath (idempotent). */
    private fun ensureDirectories(segments: List<String>, cred: String): Result<Unit> {
        for (i in segments.indices) {
            val url = davUrl(segments.subList(0, i + 1))
            val req = Request.Builder()
                .url(url)
                .header("Authorization", cred)
                .method("MKCOL", null)
                .build()
            client.newCall(req).execute().use { resp ->
                when (resp.code) {
                    401 -> return Result.failure(IOException("坚果云认证失败(401)：邮箱或应用密码不对"))
                    403 -> return Result.failure(IOException("坚果云拒绝(403)：检查路径是否允许写入"))
                    // 201 created; 405/301/200/207 already exists -> fine. Others: let the PUT surface it.
                }
            }
        }
        return Result.success(Unit)
    }

    suspend fun put(
        basePath: String,
        fileName: String,
        content: String,
        email: String,
        appPassword: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val cred = Credentials.basic(email, appPassword)
            val segments = basePath.split('/').filter { it.isNotBlank() }

            ensureDirectories(segments, cred).onFailure { return@withContext Result.failure(it) }

            val url = davUrl(segments + fileName)
            val request = Request.Builder()
                .url(url)
                .header("Authorization", cred)
                .put(content.toByteArray(Charsets.UTF_8).toRequestBody("text/markdown; charset=utf-8".toMediaType()))
                .build()

            client.newCall(request).execute().use { resp ->
                if (resp.isSuccessful) {
                    Result.success(Unit)
                } else {
                    Result.failure(IOException("WebDAV PUT ${resp.code}: ${resp.body?.string().orEmpty().take(200)}"))
                }
            }
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }

    /** Settings test: verify auth, then whether the target folder already exists. */
    suspend fun test(
        basePath: String,
        email: String,
        appPassword: String
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val cred = Credentials.basic(email, appPassword)
            val propfind =
                "<?xml version=\"1.0\"?><d:propfind xmlns:d=\"DAV:\"><d:prop><d:resourcetype/></d:prop></d:propfind>"
                    .toRequestBody("application/xml; charset=utf-8".toMediaType())

            // 1) Auth check on the dav root.
            val rootReq = Request.Builder()
                .url(davUrl(emptyList()))
                .header("Authorization", cred)
                .header("Depth", "0")
                .method("PROPFIND", propfind)
                .build()
            client.newCall(rootReq).execute().use { resp ->
                if (resp.code == 401) return@withContext Result.failure(IOException("账号或应用密码不对（401）"))
                if (!(resp.isSuccessful || resp.code == 207)) {
                    return@withContext Result.failure(IOException("坚果云连接失败：HTTP ${resp.code}"))
                }
            }

            // 2) Does the target folder exist?
            val segments = basePath.split('/').filter { it.isNotBlank() }
            val pathReq = Request.Builder()
                .url(davUrl(segments))
                .header("Authorization", cred)
                .header("Depth", "0")
                .method("PROPFIND", propfind)
                .build()
            client.newCall(pathReq).execute().use { resp ->
                when {
                    resp.code == 207 || resp.isSuccessful -> Result.success("连接正常，目标文件夹已存在 ✓")
                    resp.code == 404 -> Result.success("账号正常；目标文件夹暂不存在，同步时会自动创建 ✓")
                    else -> Result.failure(IOException("路径检查失败：HTTP ${resp.code}"))
                }
            }
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }
}
