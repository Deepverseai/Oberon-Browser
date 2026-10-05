# Oberon Browser

> **Ultra-Lightweight (3–5 MB) Pure Native Android Browser with Embedded Agent Automation Bridge & Dual-Mode Live Preview**

Oberon Browser is an ultra-fast, native Android browser engineered for modern web browsing, local developer previews, and autonomous AI pair-programming automation via Antigravity CLI and Model Context Protocol (MCP).

---

## 🌟 Key Capabilities

### 1. ⚡ Ultra-Lightweight & Pure Native
- **Size:** 3.0 – 4.5 MB compiled APK (vs 120MB+ Chrome / Firefox).
- **Core:** 100% Native Kotlin/Java utilizing the pre-installed Chromium-based `Android System WebView`. Zero Cordova, Ionic, React Native, or bloated web wrappers.
- **Tab Virtualization:** Background tabs automatically pause rendering buffers and JavaScript timers when open tabs exceed 3 to prevent Low Memory Killer (OOM) crashes.

### 2. 🤖 Localhost Agent Automation Bridge (`127.0.0.1:8765`)
- Embedded high-performance, low-latency background server listening strictly on `127.0.0.1:8765`.
- Secured via mandatory `X-Agent-Token` header.
- Allows AI agents (Antigravity CLI, Cursor, Claude Code) to:
  - Navigate URLs (`open_url`)
  - Click elements with animated visual feedback (`click_element`)
  - Fill forms & inputs (`type_text`)
  - Read rendered DOM text and HTML (`read_page`, `get_html`)
  - Capture real-time screenshots (`screenshot`)
  - Run end-to-end live testing audits (`audit_preview`)
- **Auto-Wake Feature:** Automatically launches Oberon Browser on your phone screen via Android Activity Manager (`am start`) if the browser is currently closed.

### 3. 📱 Dual-Mode Live Preview (Web vs App)
- **Website Preview:** Standard responsive mobile viewport.
- **App Preview:** Reconfigures viewport to a standard simulated native device frame (390 × 844 dp) with touch emulation, letting developers preview full-stack web apps (React, Next.js, Vite, Vue, FastAPI) as native mobile apps.

### 4. 🎨 Rich Premium Neutral Aesthetic (Understated Luxury)
- Designed following high-end neutral design principles:
  - Deep Obsidian (`#090A0B`) & Warm Charcoal (`#131416`)
  - Frosted Platinum (`#F3F4F6`) typography
  - Subtle Sage (`#6EE7B7`) accents & indicators
  - Zero cheap neon lights or saturated gamer gradients.

### 5. 🛡️ Complete Daily-Driver Suite
- **Smart Omnibox:** Instant classification between URLs and search queries, voice search, and multi-engine live suggestions (Google, DuckDuckGo, Bing, Brave).
- **Site Security:** Dynamic SSL padlock (🔒 Secure, ⚠️ Insecure, ❌ Error) with real-time certificate inspection.
- **Anti-Bot Camouflage:** Custom User-Agent mimicking standard Google Chrome on Android (strips `; wv` WebView signatures).
- **Host-Based AdBlocker:** High-speed in-memory interceptor blocking ad networks and tracking beacons.
- **Native Downloads:** Integrated with Android `DownloadManager`, `POST_NOTIFICATIONS` runtime permission, and secure file opening/sharing via `FileProvider`.
- **Utilities:** Find in page, Save to PDF (`PrintManager`), Desktop Mode toggle, and zero-trace Incognito mode.

---

## 🏗️ Architecture

```
oberon-browser/
├── app/
│   ├── build.gradle.kts
│   ├── proguard-rules.pro
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/antigravity/oberon/
│       │   ├── MainActivity.kt
│       │   ├── agent/            # Embedded 127.0.0.1:8765 server & JSON protocol
│       │   ├── browser/          # WebViewClient, WebChromeClient, SSL & File chooser
│       │   ├── db/               # SQLite Bookmarks & History database
│       │   ├── downloads/        # Android DownloadManager & FileProvider UI
│       │   ├── features/         # AdBlocker, Find in Page, PDF Exporter, Desktop Mode
│       │   ├── home/             # Speed Dial Homepage with dev ports (3000, 5173, 8000)
│       │   ├── preview/          # Dual-Mode Preview manager & Virtual Cursor overlay
│       │   ├── search/           # Omnibox Classifier, Search Engines & Live Suggest
│       │   └── tabs/             # Multi-tab lifecycle manager & Virtualization
│       └── res/                  # Rich Neutral XML layouts, styles, and drawables
├── mcp_servers/
│   └── oberon_browser_mcp/       # Antigravity Python MCP Bridge & Live Auditor
├── .github/workflows/
│   └── build-apk.yml             # Cloud CI/CD compiling & packaging APKs
└── tests/                        # Comprehensive unit & E2E verification test suite
```

---

## 🚀 Building & GitHub Actions CI/CD

To compile APKs without burdening local mobile environments, this repository uses automated cloud builds on Ubuntu runners:

1. Push commits to `main`:
   ```bash
   git push origin main
   ```
2. GitHub Actions automatically executes `.github/workflows/build-apk.yml`:
   - Sets up Temurin JDK 21
   - Executes `./gradlew assembleDebug assembleRelease`
   - Applies ProGuard / R8 code shrinking and resource optimization
   - Compresses the outputs into `oberon-browser-apks.zip`
   - Publishes the artifact to the GitHub workflow run.
3. Download the zipped APKs from the **Actions** tab or via `python3 scripts/download_release_apk.py`.

---

## 🧪 Running Tests

Run the test suite locally:
```bash
python3 -m unittest discover -s tests -p "test_*.py"
```

---

## 📄 License
Apache License 2.0 - Built by Deepverse AI.
