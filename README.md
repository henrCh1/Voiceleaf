<div align="center">

# 🍃 Voiceleaf

**口述，即成文。**

把每天随口说出的想法与复盘，轻轻改成书面文字，自动归档进你的 Obsidian。

<p>
  <img alt="Platform" src="https://img.shields.io/badge/platform-Android-3DDC84?logo=android&logoColor=white">
  <img alt="Kotlin" src="https://img.shields.io/badge/Kotlin-2.0-7F52FF?logo=kotlin&logoColor=white">
  <img alt="Compose" src="https://img.shields.io/badge/Jetpack%20Compose-Material3-4285F4?logo=jetpackcompose&logoColor=white">
  <img alt="minSdk" src="https://img.shields.io/badge/minSdk-26-blue">
  <img alt="Version" src="https://img.shields.io/badge/version-1.1.0-2F855A">
  <img alt="License" src="https://img.shields.io/badge/license-MIT-green">
</p>

**简体中文** · <a href="./README.en.md">English</a>

</div>

---

## 💡 初衷

我想用「说」的方式记录每天的想法和复盘，而不是低头打字。

但语音转出来的文字太口语、太碎，直接存进笔记里很难看；可如果交给 AI「写成一篇文章」，又会丢掉我原本的语气和真实的想法。

所以 Voiceleaf 只做一件事：**把口语轻改成书面，但绝不重写。** 去掉「嗯、然后、那个」这些口头禅，合并重复、理顺语句、在该分段的地方分段——仅此而已。原意、顺序、判断、情绪，全部保留。成稿每周汇成一个 Markdown，通过坚果云同步进 Obsidian，手机和电脑两头都能看。

---

## ✨ 特性

- 🎙️ **口述优先** —— 拿起手机，用输入法的语音键说出来即可，也支持打字。
- ✍️ **最小改写润色** —— DeepSeek 把口语改成通顺书面段落：去口头禅、合并啰嗦、自动分段，但不改原意、不改顺序、不加评论、不编造。
- 💾 **本地优先，不丢内容** —— 输入实时存草稿；每条先存进本地数据库再润色，崩溃/断网都不丢。
- 🗓️ **每周一个 Markdown** —— 按 ISO 周自动归档，时间标签独占一行，阅读清爽。
- ☁️ **一键同步 Obsidian** —— 经坚果云 WebDAV 推送，电脑端 Obsidian 自动同步，手机上无需安装 Obsidian。
- 📚 **历史管理** —— 按周、按天浏览全部记录；可逐条**删除 / 编辑**（连润色好的成稿也能改），单周可**重新同步**。
- 🔁 **打开即自动补传** —— 跨周后再打开 App，会自动把上周还没同步的内容推上去，不用记得手动点。
- 🧩 **可换模型** —— 默认 DeepSeek，开箱即用；也可在「高级设置」里填任意 OpenAI 兼容接口。
- 🔒 **隐私自持** —— 无账号、无服务器，数据与密钥都只存在你自己的手机上。

---

## 🔄 工作流程

```
🎙️ 口述/打字  ──►  ✍️ 逐条润色  ──►  🗓️ 按周渲染成 Markdown  ──►  ☁️ 坚果云 WebDAV  ──►  📖 电脑端 Obsidian
   (本地实时存草稿)     (DeepSeek)        (一周一个文件)            (一键同步)            (自动出现)
```

---

## 🚀 快速开始

> 需要 JDK 17（Android Studio 自带的 JBR 即可）与 Android SDK（Platform 34）。

**用 Android Studio：**
1. `File → Open`，选择本项目根目录。
2. 等待 Gradle Sync（首次会下载 Gradle 与依赖）。
3. 若 `local.properties` 不存在，新建一行指向你的 SDK：`sdk.dir=你的\\Android\\Sdk`。
4. 连真机或开模拟器，点 ▶ Run。

**用命令行：**
```bash
# 编译
./gradlew :app:assembleDebug
# 手机插 USB、开 USB 调试后，直接编译并安装
./gradlew installDebug
```

---

## ⚙️ 首次配置（在 App 的「设置」里填）

| 字段 | 怎么拿 |
|---|---|
| DeepSeek API Key | 到 [platform.deepseek.com](https://platform.deepseek.com) 注册、充值少量额度、创建 API Key |
| 坚果云登录邮箱 | 你的坚果云账号邮箱 |
| 坚果云应用密码 | 坚果云网页端 → 账户信息 → 安全选项 → 第三方应用管理 → 添加应用，生成的密码（**不是登录密码**） |
| 目标文件夹路径 | Obsidian 库在坚果云里的相对路径，如 `Obsidian Vault/笔记` |

每一项旁边都有「测试连接」按钮，填完点一下确认无误。

> 全程走国内可直连服务，DeepSeek 与坚果云在国内手机网络下无需代理。**使用时需联网**（润色在云端、成稿上传到云）。

---

## 📖 日常用法

1. 打开 App，在输入框口述（键盘上点麦克风）或打字，内容**实时自动存草稿**。
2. 点「**完成**」：这一条被提交并立刻润色（失败会标红，可「重试润色」）。
3. 攒到想发布时点「**同步**」：把有改动的每一周各渲染成一个 Markdown，覆盖写到坚果云。
4. 电脑端 Obsidian 下次同步即可看到，如 `2026-W24 周复盘 (0608-0614).md`。

---

## 🧩 高级设置（自定义模型）

默认使用 DeepSeek，只需填一个 API Key。如果你想用别的模型或自己的 Token：

- 打开「设置 → 高级设置」里的开关，填 **请求地址（完整 URL）+ API Key + 模型名称** 即可。
- 开关同时决定优先级：两边都填了的话，关＝用 DeepSeek，开＝用自定义。
- 自定义模式下只发送 `model + messages`，温度、思考等参数交给该模型自己的默认值。

---

## 📐 重要行为约定

- **当周文件由 App 管理**，这一周内请勿在 Obsidian 手改它（下次同步会整文件覆盖）。周一过后该周封档，App 不再触碰，可随意编辑历史。
- 同步时，某周只要还有**未润色成功**的条目，整周会被跳过、不上传，避免把口语原文发到 Obsidian。（想强制发原文可在设置里打开开关，默认关。）
- 删除是**软删除**：删掉已同步条目后，下次同步会重渲染该周（不含它）并清理。

---

## 🛠️ 技术栈

Kotlin · Jetpack Compose (Material3) · Room · DataStore · OkHttp · Coroutines。无服务器、无账号体系。

`minSdk 26 / targetSdk 34 / compileSdk 34` · AGP 8.5.2 · Gradle 8.9 · Kotlin 2.0.20 · KSP。

---

## 🗂️ 代码结构

```
app/src/main/java/com/v2j/app/
├── data/        Room 实体(Entry/Draft)、DAO、数据库、SettingsRepository(DataStore)
├── net/         DeepSeekClient(润色, 支持自定义 provider)、WebDavClient(坚果云上传)
├── sync/        WeekMath(ISO 周/天计算)、WeekRenderer(周 Markdown 渲染, 纯函数)
├── ui/          MainScreen、SettingsScreen、HistoryScreen、theme
├── JournalRepository.kt   录入/草稿/软删除 + 同步编排(单飞、全润才发)
├── MainViewModel.kt       UI 状态、防抖自动存草稿
├── MainActivity.kt        三屏导航(主页/设置/历史)
└── V2JApp / AppContainer  手动依赖容器 + 启动崩溃恢复(POLISHING→RAW)
```

---

## 🔒 隐私

无账号、无后端。所有记录存在手机本地数据库，API Key 与坚果云密码存在本机的 DataStore；只有「润色」和「同步」两步会联网，分别直连 DeepSeek 与坚果云。

---

## 🗺️ 路线图

- [x] 打开 App 自动补传上周（v1.1.0 已实现）
- [ ] WorkManager 定时后台同步（不开 App 也能推）
- [ ] 当周文件在 Obsidian 手改后的合并
- [ ] Room 数据导出备份（防卸载/换机丢失）
- [ ] 语音直录模式（不看屏，拿起就说）

---

## 🆕 更新日志

最新版本 **v1.1.0**：历史记录支持单周重新同步、单条删除与编辑，打开 App 自动补传上周。完整记录见 [CHANGELOG](./CHANGELOG.md)。

---

## 📄 License

[MIT](./LICENSE) © 2026 henrCh1
