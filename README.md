# 🤖 Jarvis AI Assistant (Android Native APK)

A full-fledged, voice-activated AI personal assistant for Android powered by **Google Gemini 2.0 Flash / 1.5 Flash**, equipped with **Continuous Long-term Memory**, **Phone Automation (Accessibility Service)**, **App Launcher**, **YouTube Player**, and an **Animated Anime Companion Avatar**.

Inspired by [Aryan-0001/Jarvis-AI](https://github.com/Aryan-0001/Jarvis-AI) and [open-jarvis/OpenJarvis](https://github.com/open-jarvis/openjarvis).

---

## ✨ Key Features

1. 🎙️ **Voice First (Hindi, Hinglish & English)**:
   - Full Speech-to-Text (STT) and natural Text-to-Speech (TTS).
   - Real-time animated neon audio spectrum visualizer.
   - Talks naturally to you with charismatic personality, addressing you as "Boss" / "Sir".

2. 🧠 **Continuous Long-Term Memory (OpenJarvis Core)**:
   - Built-in SQLite / Room Database that permanently remembers everything you say.
   - Learns your habits, preferences, favorite games (Minecraft), music taste, and conversational style.
   - Auto-extracts personal facts from normal conversation without needing special commands.
   - Interactive Memory Core viewer to inspect or edit what Jarvis knows about you.

3. 📱 **Complete Phone Control & App Launching**:
   - **Open Games & Apps**: *"Jarvis open Minecraft"*, *"Open ChatGPT"*, *"Camera khol do"*, *"WhatsApp open karo"*.
   - **YouTube Playback**: *"YouTube par Arijit Singh ka gana chalao"*, *"Play Minecraft gameplay on YouTube"*.
   - **Image Generation**: *"Make an image of a futuristic neon city"* (Opens ChatGPT with prompt preloaded).
   - **System Navigation**: Go Home, Press Back, Open Recents, Pull down Notifications, or Lock device via Android Accessibility Service.

4. 🕶️ **Anime Companion Avatar UI**:
   - Features your custom anime character (red hoodie, sunglasses, sneakers).
   - Glowing circular Arc Reactor HUD rings with dynamic animations.
   - Reactive states: IDLE, LISTENING (cyan glow), THINKING (amber pulse), SPEAKING (neon red wave).

5. ⚡ **Automated Cloud APK Build (GitHub Actions)**:
   - Zero storage required on your phone! GitHub's cloud servers build and sign the `.apk` file automatically.
   - Direct 1-click download link from GitHub Actions / Releases tab.

---

## 🚀 How to Build & Download Your APK (In 3 Simple Steps)

### Step 1: Push This Project to Your GitHub
Inside Termux, run:
```bash
cd ~/projects/jarvis-assistant
git init
git add .
git commit -m "Initial commit: Jarvis AI Assistant"
git branch -M main
```

Now create a new repository on [GitHub](https://github.com/new) named `jarvis-assistant` and push:
```bash
git remote add origin https://github.com/<YOUR_GITHUB_USERNAME>/jarvis-assistant.git
git push -u origin main
```

### Step 2: Download the APK
1. Open your repository on GitHub.
2. Click on the **Actions** tab (or **Releases**).
3. You will see the **Build Jarvis AI Android APK** workflow running.
4. Once completed (approx. 2-3 minutes), click on the run or Release to download **`Jarvis-AI-Assistant-Debug.apk`** straight to your phone!

### Step 3: Install & Grant Permissions on Android
1. Open the downloaded `.apk` file on your phone and tap **Install**.
2. When you open Jarvis:
   - Grant **Microphone** permission so Jarvis can hear you.
   - Tap the **Settings (gear icon)** and enter your free **Google Gemini API Key** (Get free key from: [aistudio.google.com](https://aistudio.google.com/)).
   - Tap the **Accessibility icon** to enable Jarvis in Android Accessibility Settings for full phone navigation control.

---

## 🗣️ Voice Commands Cheat Sheet

| Command (Hindi / Hinglish / English) | What Jarvis Does |
|---|---|
| *"Jarvis, open Minecraft"* | Launches Minecraft PE directly |
| *"YouTube par Believer gana chalao"* | Opens YouTube and plays 'Believer' |
| *"Open ChatGPT make an image of a cyber warrior"* | Opens ChatGPT with prompt |
| *"Yaad rakhna mera favorite color red hai"* | Permanently saves fact into Long-term Memory |
| *"Main kaun hu aur meri kya preferences hain?"* | Recalls and speaks your personal profile |
| *"Phone ki home screen par jao"* | Presses Home button via Accessibility |
| *"Notifications open karo"* | Pulls down system notifications |
| *"Camera khol do"* | Launches Camera app |

---

## 🛠️ Project Structure
```
jarvis-assistant/
├── .github/workflows/build-apk.yml    # Automated GitHub Actions APK Compiler
├── app/
│   ├── src/main/
│   │   ├── AndroidManifest.xml        # Permissions, Accessibility & Query packages
│   │   ├── java/com/jarvis/assistant/
│   │   │   ├── MainActivity.kt        # Main Voice UI & Coordinator
│   │   │   ├── ai/
│   │   │   │   ├── GeminiClient.kt    # Gemini 2.0 / 1.5 Flash REST client
│   │   │   │   ├── PromptTemplates.kt # Persona & Memory prompt builder
│   │   │   │   ├── AppLauncherTool.kt # Launches Minecraft, apps & games
│   │   │   │   ├── YouTubeTool.kt     # YouTube search & playback
│   │   │   │   └── ImageGenTool.kt    # Image generator launcher
│   │   │   ├── memory/
│   │   │   │   ├── MemoryEntity.kt    # Room memory entity
│   │   │   │   ├── MemoryDao.kt       # Room DAO queries
│   │   │   │   ├── MemoryDatabase.kt  # Local SQLite database
│   │   │   │   └── MemoryManager.kt   # Dynamic fact learning & memory injection
│   │   │   ├── services/
│   │   │   │   ├── JarvisAccessibilityService.kt # Full phone automation
│   │   │   │   └── JarvisForegroundService.kt    # Background service
│   │   │   ├── ui/
│   │   │   │   ├── AvatarView.kt      # Anime character & HUD animation
│   │   │   │   ├── VisualizerView.kt  # Neon audio wave visualizer
│   │   │   │   └── MemoryAdapter.kt   # Memory inspector list adapter
│   │   │   └── voice/
│   │   │       ├── VoiceRecognizer.kt # Android SpeechRecognizer
│   │   │       └── VoiceSpeaker.kt    # Android TextToSpeech
│   │   └── res/
│   │       ├── drawable/avatar_anime.png # Your custom anime boy avatar
│   │       └── layout/activity_main.xml  # Futuristic sci-fi layout
└── build.gradle.kts                   # Root gradle configuration
```
