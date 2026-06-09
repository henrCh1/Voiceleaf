package com.v2j.app.net

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

/**
 * One polish provider's connection info.
 *  - DeepSeek default: url/model fixed, [deepseekDefaults] = true so we add temperature + thinking.
 *  - Custom (高级设置): user-supplied url/key/model, [deepseekDefaults] = false so we send only
 *    model + messages and let that model's own defaults decide temperature / thinking.
 */
data class PolishConfig(
    val url: String,
    val apiKey: String,
    val model: String,
    val deepseekDefaults: Boolean,
) {
    companion object {
        const val DEEPSEEK_URL = "https://api.deepseek.com/chat/completions"
        const val DEEPSEEK_MODEL = "deepseek-v4-flash"

        fun deepSeek(apiKey: String) = PolishConfig(
            url = DEEPSEEK_URL,
            apiKey = apiKey,
            model = DEEPSEEK_MODEL,
            deepseekDefaults = true,
        )
    }
}

/**
 * Polishes one spoken note via an OpenAI-compatible chat-completions endpoint.
 *  - Default provider: deepseek-v4-flash (deepseek-chat is deprecated 2026-07-24 15:59 UTC),
 *    thinking OFF (rewriting/formatting → non-thinking, avoids 2-5x tokens & over-editing),
 *    temperature 1.0 (DeepSeek's official "data cleaning" value), China-direct.
 *  - Custom provider: only model + messages are sent; that model's defaults govern everything else.
 */
class DeepSeekClient(private val client: OkHttpClient) {

    suspend fun polish(rawText: String, config: PolishConfig): String = withContext(Dispatchers.IO) {
        val body = JSONObject().apply {
            put("model", config.model)
            if (config.deepseekDefaults) {
                put("temperature", 1.0)
                put("thinking", JSONObject().put("type", "disabled"))
            }
            put("messages", JSONArray().apply {
                put(JSONObject().put("role", "system").put("content", SYSTEM_PROMPT))
                put(JSONObject().put("role", "user").put("content", rawText))
            })
        }

        val request = Request.Builder()
            .url(config.url)
            .addHeader("Authorization", "Bearer ${config.apiKey}")
            .post(body.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
            .build()

        client.newCall(request).execute().use { resp ->
            val text = resp.body?.string().orEmpty()
            if (!resp.isSuccessful) {
                throw IOException("HTTP ${resp.code}: ${text.take(300)}")
            }
            val content = JSONObject(text)
                .getJSONArray("choices")
                .getJSONObject(0)
                .getJSONObject("message")
                .getString("content")
                .trim()
            if (content.isEmpty()) throw IOException("模型返回为空")
            content
        }
    }

    /** Lightweight credential / connectivity check for the Settings screen. */
    suspend fun ping(config: PolishConfig): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val body = JSONObject().apply {
                put("model", config.model)
                put("max_tokens", 1)
                put("messages", JSONArray().put(JSONObject().put("role", "user").put("content", "ping")))
            }
            val request = Request.Builder()
                .url(config.url)
                .addHeader("Authorization", "Bearer ${config.apiKey}")
                .post(body.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
                .build()
            client.newCall(request).execute().use { resp ->
                val text = resp.body?.string().orEmpty()
                when {
                    resp.isSuccessful -> Result.success(Unit)
                    resp.code == 401 -> Result.failure(IOException("API Key 无效（401）"))
                    resp.code == 402 -> Result.failure(IOException("余额不足（402）"))
                    resp.code == 404 -> Result.failure(IOException("地址或模型不对（404），检查请求地址和模型名称"))
                    else -> Result.failure(IOException("失败：HTTP ${resp.code} ${text.take(120)}"))
                }
            }
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }

    companion object {
        // Keep this in sync with the spec (§7.2). Restrained rewrite: merge fragments + paragraph,
        // but never change meaning / order / facts, and never add headings, lists or commentary.
        const val SYSTEM_PROMPT = """你是一个中文口述笔记的润色助手。用户会给你一段口述内容（通常来自手机语音输入法），偏口语、可能有重复和啰嗦。

你的任务：把它整理成书面表达的日记/复盘文字。在不改变原意和先后顺序的前提下，让它从“录音转写”变成“通顺、自然的书面段落”，但保持克制，不要过度加工。

必须做：
- 去掉语气词、口头禅、无意义填充（嗯、啊、然后、就是、那个、其实、反正等）
- 删掉重复和自我更正，同一个意思只保留一次
- 把断断续续、细碎的口语合并、衔接成连贯的书面句子，读起来像写出来的文字，而不是录音稿
- 当一段话里其实讲了几件不同的事、或几个不同的点时，在自然的转折处分段（段落之间空一行）
- 保留原文的人物、事件、判断、结论、情绪方向，以及明显个人化的说法

绝不能做：
- 不要改变原意，不要打乱内容原本的先后顺序
- 不要加小标题，不要分点列举（可以分段，但不要用 1. 2. 3. 或 - 这种列表）
- 不要加入你自己的总结、评论、升华或建议
- 不要扩写、不要编造原文没有的事实
- 不要用“首先/其次/最后”这类报告腔，也不要写成励志鸡汤

输出要求：
- 只输出润色后的正文，不要任何解释、前言或后语
- 不要加 markdown 标题或代码块；分段只用一个空行
- 中文为主；原文里的英文术语/缩写保留不译
- 长度与原文大体相当，合并啰嗦后略短没关系，但不要明显扩写"""
    }
}
