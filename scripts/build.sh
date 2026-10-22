#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."

SDK_ROOT="${ANDROID_SDK_ROOT:-${ANDROID_HOME:-}}"
if [[ -z "$SDK_ROOT" ]]; then
  echo "Set ANDROID_SDK_ROOT or ANDROID_HOME to your Android SDK directory." >&2
  exit 1
fi
if [[ -n "${JAVA_HOME:-}" ]]; then export PATH="$JAVA_HOME/bin:$PATH"; fi
TOOLS="$SDK_ROOT/build-tools/35.0.0"
PLATFORM="$SDK_ROOT/platforms/android-35/android.jar"
for tool in javac java keytool zip openssl; do
  command -v "$tool" >/dev/null || { echo "Missing tool: $tool" >&2; exit 1; }
done
for tool in aapt2 d8 zipalign apksigner; do
  [[ -x "$TOOLS/$tool" ]] || { echo "Install Android build-tools 35.0.0 (missing $tool)." >&2; exit 1; }
done
[[ -f "$PLATFORM" ]] || { echo "Install Android SDK platform 35." >&2; exit 1; }

WORK="$PWD/build/apk"
OUTPUT="$PWD/dist/Batcomputer-TV-0.1.0.apk"
mkdir -p "$WORK" dist
# Clear only this script's generated outputs, never local signing credentials.
rm -rf "$WORK/classes" "$WORK/generated" "$WORK/dex"
mkdir -p "$WORK/classes" "$WORK/generated" "$WORK/dex"
"$TOOLS/aapt2" compile --dir app/src/main/res -o "$WORK/resources.zip"
"$TOOLS/aapt2" link -I "$PLATFORM" --manifest app/src/main/AndroidManifest.xml \
  --java "$WORK/generated" -A app/src/main/assets --min-sdk-version 26 \
  --target-sdk-version 35 --auto-add-overlay -o "$WORK/unsigned.apk" "$WORK/resources.zip"
# Relative paths keep javac/d8 argument files valid in checkouts containing spaces.
find app/src/main/java build/apk/generated -name '*.java' -print > "$WORK/sources.list"
javac --release 8 -encoding UTF-8 -classpath "$PLATFORM" -d "$WORK/classes" @"$WORK/sources.list"
find build/apk/classes -name '*.class' -print > "$WORK/classes.list"
"$TOOLS/d8" --release --lib "$PLATFORM" --min-api 26 --output "$WORK/dex" @"$WORK/classes.list"
(cd "$WORK/dex" && zip -q -u ../unsigned.apk classes*.dex)
"$TOOLS/zipalign" -f -p 4 "$WORK/unsigned.apk" "$WORK/aligned.apk"

# A persistent, private key for your own builds. Never committed or uploaded.
SIGNING="$PWD/.local/signing"
mkdir -p "$SIGNING"
chmod 700 "$SIGNING"
if [[ ! -f "$SIGNING/release.p12" ]]; then
  (umask 077; openssl rand -hex 32 > "$SIGNING/password")
  keytool -genkeypair -keystore "$SIGNING/release.p12" -alias batcomputer \
    -storepass:file "$SIGNING/password" -keypass:file "$SIGNING/password" \
    -dname 'CN=Batcomputer TV Local Build' -keyalg RSA -keysize 2048 \
    -validity 10000 -storetype PKCS12 > "$WORK/signing.log" 2>&1
  chmod 600 "$SIGNING/release.p12"
fi
[[ -f "$SIGNING/password" ]] || { echo "Missing local signing password file." >&2; exit 1; }
"$TOOLS/apksigner" sign --ks "$SIGNING/release.p12" --ks-key-alias batcomputer \
  --ks-pass "file:$SIGNING/password" --out "$OUTPUT" "$WORK/aligned.apk"
"$TOOLS/apksigner" verify --verbose "$OUTPUT" > "$WORK/signature.txt"
echo "Built: dist/Batcomputer-TV-0.1.0.apk"
