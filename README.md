# Skry

**On-device gallery privacy sentinel for Android.**

Skry audits your photo library locally — OCR + rules for privacy leaks, smart cleanup for duplicates and blur, and an encrypted vault for redacted copies. Nothing leaves the device. There is no `INTERNET` permission.

<p align="center">
  <img src="docs/screenshots/01_home.png" width="220" alt="Gallery Health dashboard" />
  <img src="docs/screenshots/02_risk.png" width="220" alt="Risk Explorer" />
  <img src="docs/screenshots/05_risk_detail.png" width="220" alt="Risk detail review" />
</p>
<p align="center">
  <img src="docs/screenshots/03_clean.png" width="220" alt="Smart Cleaner" />
  <img src="docs/screenshots/04_vault.png" width="220" alt="Safety Vault" />
</p>

> Screenshots above have gallery previews mosaicked for privacy.

---

## Features

| Area | What you get |
|------|----------------|
| **Gallery Health** | Score ring · tap to scan / resume · live risk & cleaner counts |
| **Privacy Audit** | On-device ML Kit OCR + rules (passport, ID, cards, phone, email, EXIF GPS, secrets, …) |
| **Risk Explorer** | Batch confirm / clear · category filter · Review / Confirmed / Cleared |
| **Smart Cleaner** | Near-duplicates (pHash) · blurry · expired / long screenshots · system delete prompt |
| **Safety Vault** | Mosaic sensitive regions · `EncryptedFile` + Keystore · biometric unlock |

## Trust model

1. **Zero network** — `INTERNET` is stripped from the manifest  
2. **Local intelligence** — Bundled ML Kit text recognition; no cloud upload  
3. **App vault only** — Encrypted files under app-private storage (not OEM Secure Folder)

## Stack

Kotlin · Jetpack Compose · Material 3 · Room · WorkManager · ML Kit Text Recognition (bundled) · AndroidX Security Crypto · Biometric

`minSdk 33` · English UI + Latin OCR first

## Build

```bash
./gradlew :app:assembleDebug
```

Open in Android Studio (JBR / JDK 17+). Grant `READ_MEDIA_IMAGES`, then tap the health ring to scan.

## Architecture (short)

```
MediaStore → MediaRepository → Room
                 ↓
        FullScanWorker / Scan now
                 ↓
   PrivacyScanner · QualityAnalyzer · VaultService
                 ↓
     Home · Risk · Clean · Vault (Compose)
```

See [`docs/SKRY_MASTER_PLAN.md`](docs/SKRY_MASTER_PLAN.md) for phased delivery notes.

## License

MIT — see [LICENSE](LICENSE).
