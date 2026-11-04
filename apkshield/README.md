# ApkShield

On-device Android APK hardening tool. Installable APK, fully offline, no PC or server required.

## What it does

Loads a target APK on the phone, applies practical protection layers, and writes a working protected APK to `Download/ApkShield/`.

## Protection layers

1. **String literal encryption** — CONST_STRING instructions in every DEX are rewritten to encrypted payloads via the runtime `StringVault` (injected into the target).
2. **Identifier renaming** — user-package classes are renamed to short random names; `android/`, `androidx/`, `kotlin/` and ApkShield runtime packages are preserved.
3. **Signature integrity guard** — SHA-256 of the patched DEX set is written as `assets/apkshield_digest.bin`, checked at target-app startup by the injected `IntegrityApplication`.
4. **Anti-instrumentation** — `assets/apkshield_native.bin` carries an active guard descriptor for ptrace/frida/tracer/debugger/emulator checks at startup.
5. **Asset / raw resource encryption** — every `assets/**` and `res/raw/**` payload is AES-256-GCM encrypted with a per-build key stored as `assets/apkshield_asset.key` (skipped: `.dex`, `.arsc`, ApkShield metadata).
6. **Re-sign** — the output APK is signed with an APK Signature Scheme v1+v2+v3 keystore bundled inside ApkShield itself.

All layers are toggleable in the UI.

## Build

```
cd ApkShield
gradle assembleRelease
```

or open in Android Studio and Run.

`minSdk 26`, `targetSdk 34`, `compileSdk 34`, Kotlin 2.0, JDK 17.

## One-time setup (bundled keystore)

The signer expects a PKCS12 keystore at `app/src/main/resources/apkshield.p12` with password `apkshield` and alias `apkshield`.

Generate it once:

```
keytool -genkeypair \
  -keystore app/src/main/resources/apkshield.p12 \
  -storetype PKCS12 \
  -storepass apkshield \
  -keypass apkshield \
  -alias apkshield \
  -keyalg RSA -keysize 2048 -validity 10000 \
  -dname "CN=ApkShield"
```

Keep `apkshield.p12` private. Anyone who can sign with it can produce APKs your protected output will trust.

## Runtime requirements

ApkShield's output signer uses `apksig` (bundled). Target APKs produced here are signed with the ApkShield certificate unless you swap in your own keystore and cert before shipping.

## What's not included

- Custom DEX loader / encrypted DEX container (would require a native VM, out of scope for the practical preset).
- Control-flow flattening (breaks more apps than it protects; not in the practical preset).
- Resource `.arsc` encryption (framework reads `.arsc` directly; encrypting it bricks the app).

## Output

`Download/ApkShield/protected_<timestamp>_<original>.apk`

The app displays the output path in the live log and via toast when protection completes.
