package com.v2j.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.v2j.app.BuildConfig
import com.v2j.app.JournalRepository
import com.v2j.app.data.SettingsRepository
import kotlinx.coroutines.launch

private data class TestResult(val ok: Boolean, val msg: String)

@Composable
fun SettingsScreen(
    settings: SettingsRepository,
    repo: JournalRepository,
    onBack: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }

    var loaded by remember { mutableStateOf(false) }
    var key by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var appPw by remember { mutableStateOf("") }
    var path by remember { mutableStateOf("") }
    var allowRaw by remember { mutableStateOf(false) }
    var useCustom by remember { mutableStateOf(false) }
    var customUrl by remember { mutableStateOf("") }
    var customKey by remember { mutableStateOf("") }
    var customModel by remember { mutableStateOf("") }

    var dsTesting by remember { mutableStateOf(false) }
    var dsResult by remember { mutableStateOf<TestResult?>(null) }
    var wdTesting by remember { mutableStateOf(false) }
    var wdResult by remember { mutableStateOf<TestResult?>(null) }
    var cpTesting by remember { mutableStateOf(false) }
    var cpResult by remember { mutableStateOf<TestResult?>(null) }

    LaunchedEffect(Unit) {
        val s = settings.get()
        key = s.deepseekKey
        email = s.nutstoreEmail
        appPw = s.nutstoreAppPassword
        path = s.webdavBasePath
        allowRaw = s.allowRawPublish
        useCustom = s.useCustomProvider
        customUrl = s.customBaseUrl
        customKey = s.customApiKey
        customModel = s.customModel
        loaded = true
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = {
            SnackbarHost(snackbar) { data ->
                Snackbar(
                    modifier = Modifier.padding(16.dp),
                    shape = MaterialTheme.shapes.medium,
                    containerColor = MaterialTheme.colorScheme.inverseSurface,
                    contentColor = MaterialTheme.colorScheme.inverseOnSurface,
                ) { Text(data.visuals.message, style = MaterialTheme.typography.bodyMedium) }
            }
        },
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(start = 8.dp, end = 16.dp, top = 14.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "返回",
                        tint = MaterialTheme.colorScheme.onBackground,
                    )
                }
                Spacer(Modifier.width(2.dp))
                Text(
                    "设置",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                )
            }

            Column(
                Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                SectionCard(
                    title = "润色（DeepSeek）",
                    desc = "把口语整理成书面文字，只做最小改动。",
                ) {
                    Field(value = key, onChange = { key = it }, label = "DeepSeek API Key", password = true)
                    ConnTest(testing = dsTesting, result = dsResult) {
                        val k = key.trim()
                        if (k.isEmpty()) {
                            dsResult = TestResult(false, "请先填 DeepSeek Key")
                        } else {
                            scope.launch {
                                dsTesting = true; dsResult = null
                                val r = repo.testDeepSeek(k)
                                dsTesting = false
                                dsResult = if (r.isSuccess) TestResult(true, "连接正常 ✓")
                                else TestResult(false, r.exceptionOrNull()?.message ?: "失败")
                            }
                        }
                    }
                }

                SectionCard(
                    title = "同步到坚果云",
                    desc = "成稿上传到这里，电脑端 Obsidian 自动同步。密码要用坚果云生成的「应用密码」，不是登录密码。",
                ) {
                    Field(value = email, onChange = { email = it }, label = "坚果云登录邮箱")
                    Field(value = appPw, onChange = { appPw = it }, label = "坚果云应用密码", password = true)
                    Field(value = path, onChange = { path = it }, label = "目标文件夹路径，如 obsidian/笔记")
                    ConnTest(testing = wdTesting, result = wdResult) {
                        val em = email.trim()
                        val pw = appPw.trim()
                        val p = path.trim().trim('/')
                        if (em.isEmpty() || pw.isEmpty()) {
                            wdResult = TestResult(false, "请先填邮箱和应用密码")
                        } else {
                            scope.launch {
                                wdTesting = true; wdResult = null
                                val r = repo.testWebDav(p, em, pw)
                                wdTesting = false
                                wdResult = if (r.isSuccess) TestResult(true, r.getOrNull() ?: "连接正常 ✓")
                                else TestResult(false, r.exceptionOrNull()?.message ?: "失败")
                            }
                        }
                    }
                }

                SectionCard(title = "选项") {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Switch(checked = allowRaw, onCheckedChange = { allowRaw = it })
                        Spacer(Modifier.width(12.dp))
                        Text(
                            "允许把未润色的原文也发布（默认关）",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }

                SectionCard(
                    title = "高级设置（自定义模型）",
                    desc = "默认用 DeepSeek。打开下面的开关后，改用你自己的 OpenAI 兼容接口；温度、思考等参数交给该模型的默认值。两边都填了的话，由这个开关决定用哪个。",
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Switch(checked = useCustom, onCheckedChange = { useCustom = it })
                        Spacer(Modifier.width(12.dp))
                        Text(
                            if (useCustom) "正在使用：自定义模型" else "正在使用：DeepSeek 默认",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                    if (useCustom) {
                        Field(
                            value = customUrl,
                            onChange = { customUrl = it },
                            label = "请求地址，如 https://api.openai.com/v1/chat/completions",
                        )
                        Field(value = customKey, onChange = { customKey = it }, label = "API Key", password = true)
                        Field(value = customModel, onChange = { customModel = it }, label = "模型名称，如 gpt-4o-mini")
                        ConnTest(testing = cpTesting, result = cpResult) {
                            val u = customUrl.trim()
                            val k = customKey.trim()
                            val m = customModel.trim()
                            if (u.isEmpty() || k.isEmpty() || m.isEmpty()) {
                                cpResult = TestResult(false, "请先填完整：地址、API Key、模型名称")
                            } else {
                                scope.launch {
                                    cpTesting = true; cpResult = null
                                    val r = repo.testCustomProvider(u, k, m)
                                    cpTesting = false
                                    cpResult = if (r.isSuccess) TestResult(true, "连接正常 ✓")
                                    else TestResult(false, r.exceptionOrNull()?.message ?: "失败")
                                }
                            }
                        }
                    }
                }

                Button(
                    onClick = {
                        scope.launch {
                            settings.save(
                                SettingsRepository.Settings(
                                    deepseekKey = key,
                                    nutstoreEmail = email,
                                    nutstoreAppPassword = appPw,
                                    webdavBasePath = path,
                                    allowRawPublish = allowRaw,
                                    useCustomProvider = useCustom,
                                    customBaseUrl = customUrl,
                                    customApiKey = customKey,
                                    customModel = customModel,
                                )
                            )
                            snackbar.showSnackbar("已保存")
                        }
                    },
                    enabled = loaded,
                    shape = CircleShape,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                ) { Text("保存", style = MaterialTheme.typography.labelLarge) }

                Spacer(Modifier.height(8.dp))
                Text(
                    "Voiceleaf v${BuildConfig.VERSION_NAME}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 12.dp),
                )
            }
        }
    }
}

@Composable
private fun ConnTest(testing: Boolean, result: TestResult?, onTest: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        OutlinedButton(onClick = onTest, enabled = !testing, shape = CircleShape) {
            if (testing) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                Spacer(Modifier.width(8.dp))
                Text("测试中…")
            } else {
                Text("测试连接")
            }
        }
        if (result != null) {
            Text(
                result.msg,
                style = MaterialTheme.typography.bodySmall,
                color = if (result.ok) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
            )
        }
    }
}

@Composable
private fun SectionCard(
    title: String,
    desc: String? = null,
    content: @Composable () -> Unit,
) {
    Column {
        Text(
            title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp),
        )
        Surface(
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 1.dp,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) { content() }
        }
        if (desc != null) {
            Text(
                desc,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp, end = 4.dp, top = 8.dp),
            )
        }
    }
}

@Composable
private fun Field(
    value: String,
    onChange: (String) -> Unit,
    label: String,
    password: Boolean = false,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        singleLine = true,
        shape = MaterialTheme.shapes.medium,
        visualTransformation = if (password) PasswordVisualTransformation() else VisualTransformation.None,
        modifier = Modifier.fillMaxWidth(),
    )
}
