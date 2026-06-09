<div align="center">

# 🍃 Voiceleaf

**Speak it. Keep it written.**

Turn the thoughts and reviews you say out loud each day into clean written text — automatically filed into your Obsidian vault.

<p>
  <img alt="Platform" src="https://img.shields.io/badge/platform-Android-3DDC84?logo=android&logoColor=white">
  <img alt="Kotlin" src="https://img.shields.io/badge/Kotlin-2.0-7F52FF?logo=kotlin&logoColor=white">
  <img alt="Compose" src="https://img.shields.io/badge/Jetpack%20Compose-Material3-4285F4?logo=jetpackcompose&logoColor=white">
  <img alt="minSdk" src="https://img.shields.io/badge/minSdk-26-blue">
  <img alt="License" src="https://img.shields.io/badge/license-MIT-green">
</p>

<a href="./README.md">简体中文</a> · **English**

</div>

---

## 💡 Why

I wanted to capture my daily thoughts and reviews by *speaking*, not by typing with my head down.

But raw speech-to-text is messy and fragmented — ugly to keep in notes. And if you ask an AI to "write it into an article," it strips away your real voice and what you actually meant.

So Voiceleaf does exactly one thing: **gently turn speech into written prose — never a rewrite.** It drops fillers ("um", "and then", "like"), merges repetition, smooths sentences, and breaks paragraphs where it makes sense — that's all. Your meaning, order, judgments and emotions stay intact. Finished notes are gathered into one Markdown file per week and synced into Obsidian via Nutstore, readable on both phone and computer.

---

## ✨ Features

- 🎙️ **Speech-first** — pick up your phone and dictate with the keyboard's mic; typing works too.
- ✍️ **Minimal-edit polishing** — DeepSeek turns speech into fluent written paragraphs: removes fillers, merges redundancy, auto-paragraphs — without changing meaning, order, adding commentary, or fabricating.
- 💾 **Local-first, never lose a word** — drafts autosave as you type; every entry is stored locally *before* polishing, surviving crashes and offline.
- 🗓️ **One Markdown per week** — auto-filed by ISO week, with time tags on their own line for clean reading.
- ☁️ **One-tap Obsidian sync** — pushed via Nutstore WebDAV; your desktop Obsidian syncs it automatically. No Obsidian needed on the phone.
- 📚 **History view** — browse all entries by week and by day.
- 🧩 **Bring your own model** — DeepSeek by default, ready out of the box; or point it at any OpenAI-compatible endpoint in Advanced Settings.
- 🔒 **Privacy by design** — no account, no server. Your data and keys live only on your own phone.

---

## 🔄 How it works

```
🎙️ Speak / Type  ──►  ✍️ Polish each entry  ──►  🗓️ Render per week  ──►  ☁️ Nutstore WebDAV  ──►  📖 Obsidian on desktop
   (draft saved live)      (DeepSeek)             (one file per week)       (one tap)               (shows up automatically)
```

---

## 🚀 Getting started

> Requires JDK 17 (the JBR bundled with Android Studio is fine) and the Android SDK (Platform 34).

**With Android Studio:**
1. `File → Open` and select this project's root.
2. Wait for Gradle Sync (first run downloads Gradle and dependencies).
3. If `local.properties` is missing, create it with one line pointing to your SDK: `sdk.dir=/path/to/Android/Sdk`.
4. Connect a device or start an emulator, then hit ▶ Run.

**From the command line:**
```bash
# build
./gradlew :app:assembleDebug
# plug in via USB with USB debugging on, then build + install
./gradlew installDebug
```

---

## ⚙️ First-time setup (in the app's Settings)

| Field | How to get it |
|---|---|
| DeepSeek API Key | Sign up at [platform.deepseek.com](https://platform.deepseek.com), add a small credit, create an API key |
| Nutstore login email | Your Nutstore (坚果云) account email |
| Nutstore app password | Nutstore web → Account → Security → Third-party app management → Add app; use the generated password (**not** your login password) |
| Target folder path | Your Obsidian vault's relative path inside Nutstore, e.g. `Obsidian Vault/notes` |

Each field has a **Test connection** button — tap it after filling in to confirm.

> Everything runs over services directly reachable in mainland China (no proxy needed). **An internet connection is required** when in use (polishing runs in the cloud; finished notes upload to the cloud).

---

## 📖 Daily use

1. Open the app, dictate (tap the mic on your keyboard) or type — your input **autosaves as a draft**.
2. Tap **Done**: the entry is committed and polished immediately (failures are marked red with a **Retry** option).
3. When ready to publish, tap **Sync**: every changed week is rendered into a Markdown file and written to Nutstore.
4. Your desktop Obsidian picks it up on the next sync, e.g. `2026-W24 周复盘 (0608-0614).md`.

---

## 🧩 Advanced settings (custom model)

DeepSeek is the default and only needs an API key. To use a different model or your own token:

- Turn on the switch under **Settings → Advanced**, and fill in **Request URL (full) + API Key + Model name**.
- The switch also decides priority: if both are filled, OFF uses DeepSeek, ON uses your custom endpoint.
- In custom mode only `model + messages` are sent; temperature, thinking, etc. are left to that model's own defaults.

---

## 📐 Behavior notes

- **The current week's file is managed by the app** — don't edit it by hand in Obsidian during that week (the next sync overwrites the whole file). After the week ends it's sealed; the app never touches it again, so past weeks are free to edit.
- On sync, a week with any **not-yet-polished** entry is skipped entirely, so raw speech never leaks to Obsidian. (You can force-publish raw text with a switch in Settings; off by default.)
- Deletion is a **soft delete**: removing an already-synced entry re-renders that week (without it) on the next sync and purges it.

---

## 🛠️ Tech stack

Kotlin · Jetpack Compose (Material3) · Room · DataStore · OkHttp · Coroutines. Serverless, no accounts.

`minSdk 26 / targetSdk 34 / compileSdk 34` · AGP 8.5.2 · Gradle 8.9 · Kotlin 2.0.20 · KSP.

---

## 🗂️ Project structure

```
app/src/main/java/com/v2j/app/
├── data/        Room entities (Entry/Draft), DAOs, database, SettingsRepository (DataStore)
├── net/         DeepSeekClient (polish, custom-provider aware), WebDavClient (Nutstore upload)
├── sync/        WeekMath (ISO week/day math), WeekRenderer (weekly Markdown, pure function)
├── ui/          MainScreen, SettingsScreen, HistoryScreen, theme
├── JournalRepository.kt   entry/draft/soft-delete + sync orchestration (single-flight)
├── MainViewModel.kt       UI state, debounced draft autosave
├── MainActivity.kt        three-screen nav (Main / Settings / History)
└── V2JApp / AppContainer  manual DI container + crash recovery (POLISHING→RAW)
```

---

## 🔒 Privacy

No account, no backend. All entries live in a local database on your phone; your API key and Nutstore password live in on-device DataStore. Only **Polish** and **Sync** touch the network — connecting directly to DeepSeek and Nutstore respectively.

---

## 🗺️ Roadmap

- [ ] Automatic weekly sync & sealing via WorkManager
- [ ] Background re-polishing of failed entries
- [ ] Merge hand-edits made to the current week's file in Obsidian
- [ ] Export/back up the Room database (survive uninstall / device change)
- [ ] Eyes-free dictation mode (pick up and talk)

---

## 📄 License

[MIT](./LICENSE) © 2026 henrCh1
