# 更新日志 / Changelog

本项目版本遵循语义化版本（SemVer）。
This project adheres to [Semantic Versioning](https://semver.org/).

---

## [1.1.0] - 2026-06-15 — 历史管理 + 自动补传

### 新增 / Added
- **打开 App 自动补传**：跨周后再次打开 App，会自动把上周（及更早）还没同步的内容推上去；当周仍由你手动控制。不再需要记得手动点同步。
  _Auto catch-up on launch: past un-synced weeks are pushed automatically when you open the app; the current week stays manual._
- **历史「单周同步」**：历史记录里每个周标题旁可单独「重新同步」该周（强制重发，可覆盖云端丢失/改动的旧文件）。
  _Per-week re-sync in History (force re-upload, even if the week is locally clean)._
- **历史「单条删除」**：历史记录里可逐条删除。
  _Per-entry delete in History._
- **编辑已润色内容**：首页「本周记录」和历史记录里，每条都能编辑润色好的成稿；原始口述（rawText）保留留底，归属周不变。
  _Edit the polished text of any entry (home + history); the original transcript and its week are preserved._

### 修复 / Fixed
- 进程长时间不重启、跨过周界后，主屏「本周记录」仍停留在旧周的问题。
  _Home "this week" list could stick to the old week if the process lived across a week boundary._

---

## [1.0.0] - 2026-06-09 — 首个发布 / Initial release

- 口述/打字 → DeepSeek **最小改写润色**（去口语、合并啰嗦、自然分段，不改原意/顺序）→ 按 ISO 周渲染成 Markdown → 坚果云 WebDAV → 电脑端 Obsidian。
- **本地优先**：实时存草稿、防丢；每条先入库再润色；崩溃恢复。
- **全润才发**：一周内有未润色成功的条目则整周跳过，避免口语原文外泄。
- **软删除**：删已同步条目后重渲染该周。
- 列表**最新在上**；MD **时间标签独占一行**。
- **高级设置**：可自定义任意 OpenAI 兼容模型接口（默认 DeepSeek）。
- 各项**「测试连接」**；**历史回看**；**森林绿 Logo**。
