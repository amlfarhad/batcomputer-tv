# Batcomputer TV

**What if your Android TV was the Batcomputer?**

A native Android TV launcher with near-black surfaces, muted red accents, warm phosphor lettering, and a shaded bat emblem when the screen goes idle.

No ads. No recommendation feed. Just your apps.

![File Browser layout with Stremio, Apple TV, Plex, and Paramount+](docs/images/home.png)

## The interface

- **Three layouts:** Command Line, File Browser, and Quiet Console, with matching Search and Settings.
- **Your favorites:** hold OK on an app to favorite it or pin it to the top.
- **Your installed apps:** local search, sideloaded app discovery, and direct access to Play Store and Android Settings.
- **The terminal finish:** scanlines, grain, phosphor-style text, and short focus transitions. Texture is optional.
- **The idle screen:** a shaded bat emblem, clock, and date, also available as an Android screensaver.

![Terminal idle screen](docs/images/idle.png)

The app-list screenshot is edited to show a smaller selection of apps. A fresh install discovers your own apps; favorites start empty. The emblem is cropped from a supplied reference. No generated artwork is used.

## Set it up with an AI assistant

Use a coding assistant that can run commands on your computer. Paste this prompt; it can handle the build and installation while guiding you through the few steps that need the TV remote.

```text
Help me install Batcomputer TV on my Android TV / Google TV device:
https://github.com/amlfarhad/batcomputer-tv

Clone the repository and read README.md and docs/INSTALL.md first.
Check my computer's build tools, then build the APK from source using
this repo's scripts. Keep the local signing key private and preserve it
for future updates. Do not change the app's source or design.

Guide me through enabling debugging on the TV and connecting ADB over
my local network. Ask for my device model or IP address when needed;
inspect the device's Android version and supported connection method.
Install on the specific device I authorize, open the app, and check it
with me before making it the default Home launcher.

For default Home, inspect what this device supports rather than guessing.
Record the original Home component and give me a way to restore it first.
Do not disable or uninstall system packages without explaining the exact
change and getting my approval. Preserve my accounts, apps, casting,
voice search, and Ambient Mode. Explain optional screensaver integration
separately. If a step requires my remote, tell me exactly what to do.
```

You need a computer and TV on the same local network, plus access to the TV's Developer options. Wireless debugging and default Home selection vary by device. An assistant can guide those steps; a chat window without computer access cannot install the APK for you.

## Prefer to install it yourself?

Follow the **[build and installation guide](docs/INSTALL.md)** for prerequisites, wireless ADB, installation, Home selection, and optional screensaver setup. **[Remote controls](docs/INSTALL.md#remote-controls)** are there too.

Requires Android TV 8.0 / API 26 or newer. Some firmware restricts third-party Home selection; installation alone does not guarantee the Home button can be reassigned.

## Under the hood

Java and Android's native Canvas APIs. No WebView, bundled web runtime, third-party application framework, analytics, or Internet permission. App discovery runs off the UI thread and the catalog is cached locally. Decorative effects do not need a continuous animation loop.

The source is standalone, with no launcher-specific switching, recovery scripts, or dependency on another launcher. The development build was used on an ONN Android TV stick; behavior on other firmware may differ.

## Credits

An unofficial fan project by [Amal Farhad](https://github.com/amlfarhad). Batman artwork and marks belong to their respective rights holders; this project is not affiliated with DC or Warner Bros. JetBrains Mono is included under the SIL Open Font License. See [third-party notices](THIRD_PARTY_NOTICES.md).
