# Build and installation

For an assistant-led setup, start with the [copyable prompt in the README](../README.md#set-it-up-with-an-ai-assistant). This guide covers the same process manually.

## 1. Build the APK

Requires macOS or Linux, JDK 17 or newer, Android SDK command-line tools, Bash, Git, `zip`, and `openssl`. The build has been checked with JDK 21. On Windows, build in a Linux environment such as WSL; use ADB on whichever host can reach your TV.

```sh
git clone https://github.com/amlfarhad/batcomputer-tv.git
cd batcomputer-tv
```

Install the SDK components using Android Studio's SDK Manager or the command-line SDK tools. Set the paths to your own installations:

```sh
export ANDROID_SDK_ROOT="/path/to/your/android-sdk"
export JAVA_HOME="/path/to/your/jdk"
export PATH="$JAVA_HOME/bin:$ANDROID_SDK_ROOT/cmdline-tools/latest/bin:$ANDROID_SDK_ROOT/platform-tools:$PATH"
sdkmanager "platforms;android-35" "build-tools;35.0.0" "platform-tools"
bash scripts/test.sh
bash scripts/build.sh
```

Output: `dist/Batcomputer-TV-0.1.0.apk`.

The first build generates a signing key under `.local/signing/`, excluded from Git. Keep this directory privately backed up and reuse it for future builds. Android updates require the same signing key. Independent checkouts and CI runs generate different keys unless that directory is preserved. Do not commit or share it.

## 2. Connect to the TV

Keep the TV and computer on the same local network. Enable Developer options on the TV (usually by selecting **Build** seven times under **About**), then enable the debugging option the device provides. Accept the computer authorization prompt on the TV.

There are two common network connection methods. Use the one your device exposes; Android version alone does not guarantee a manufacturer includes it.

### Wireless debugging with pairing

If Developer options has **Wireless debugging**, open its pairing-code screen. Replace the placeholders below with the addresses and ports displayed by your TV. The pairing port and connection port may differ.

```sh
adb pair TV_ADDRESS:PAIRING_PORT
adb connect TV_ADDRESS:ADB_PORT
adb devices -l
```

Enter the pairing code when ADB prompts for it.

### Network ADB without pairing

Some TVs expose network ADB through **Network debugging**, or through their manufacturer-specific **USB debugging** setting. If that device explicitly exposes ADB on port 5555:

```sh
adb connect TV_ADDRESS:5555
adb devices -l
```

Do not assume every USB-debugging toggle enables network ADB. If the TV exposes neither network method, check the manufacturer's connection procedure; a USB connection may be needed. See [Android's official ADB connection guide](https://developer.android.com/tools/adb#wireless) for pairing and troubleshooting.

The target must appear with status `device`. If it says `unauthorized`, accept the authorization prompt on the TV. Use its exact serial from `adb devices -l` in the commands below, especially if more than one device is connected.

## 3. Install and open

```sh
adb -s DEVICE_SERIAL install -r dist/Batcomputer-TV-0.1.0.apk
adb -s DEVICE_SERIAL shell am start -n io.github.amlfarhad.batcomputertv/.MainActivity
```

You can also open **Batcomputer TV** from your apps. Check that you can navigate, open apps, use Search, and reach Settings before changing the default Home app.

For later updates, rebuild with your existing `.local/signing/` directory and repeat the install command. If Android reports a signature mismatch, locate the original signing key first; uninstalling the app deletes its saved favorites and settings.

## 4. Make it your Home launcher

Where supported, use Android's Home chooser or the **Default apps → Home app** setting. Menu names vary by manufacturer. Before changing it, note the original Home app and how to select it again in Android Settings. Batcomputer TV's Settings page opens system Settings directly.

Some TV firmware hides or restricts Home selection. There is no universal replacement command for every TV. An assistant should inspect your device's available Home activities and supported settings before proposing a device-specific change. Disabling a system launcher is a separate decision, not an installation requirement. This repository does not include system-package disabling or launcher recovery scripts.

The launcher does not replace Google account services, casting, voice search, the package installer, or your installed apps.

## 5. Optional system screensaver

The launcher's own idle screen works without special access. You can also select **Terminal** in Android's screensaver settings if your device exposes that menu.

To allow the launcher's **Idle screen** setting to select the system screensaver automatically:

```sh
adb -s DEVICE_SERIAL shell pm grant io.github.amlfarhad.batcomputertv android.permission.WRITE_SECURE_SETTINGS
```

This optional permission allows secure settings writes. The app uses it only for `screensaver_components`, selecting Terminal or Google Ambient while Batcomputer TV is the default Home. Google Ambient must be installed for that option. Without this grant, the setting controls the Home idle screen without changing the system screensaver.

To revoke that permission later:

```sh
adb -s DEVICE_SERIAL shell pm revoke io.github.amlfarhad.batcomputertv android.permission.WRITE_SECURE_SETTINGS
```

Revoking permission stops future writes; it does not restore a previously selected screensaver. Select your preferred screensaver in system Settings before revoking if you want to change it back.

## Remote controls

| Action | Control |
| --- | --- |
| Select an app or setting | Up / Down |
| Open an app; cycle a setting | OK |
| Favorite, pin, or open app info | Hold OK on an app |
| Reach navigation from a list or Settings | Left / Right |
| Move between Quiet Console grid columns | Left / Right; outer edge reaches navigation |
| Move through bottom tabs | Left / Right, then OK; Up returns to content |
| Move through File Browser sidebar | Up / Down, then OK; Right returns to content |
| Return to apps | Back |

## Source layout

```text
app/src/main/java/   Native launcher, navigation, drawing, and screensaver
app/src/main/assets/ Emblem and JetBrains Mono
app/src/main/res/    Theme and launcher artwork
tests/              Pure Java navigation and selection checks
scripts/            SDK build and test entry points
docs/images/        Edited app-list screenshot and development idle screenshot
```

The public package ID is `io.github.amlfarhad.batcomputertv`. Unit checks cover navigation and selection logic, not every manufacturer's Home behavior.
