# Skry Master Plan

**Status:** Active  
**Updated:** 2026-07-28  
**Decisions locked:** Phased MVP · English-first · OCR+rules · No self-training · Phase 4 reserved for optional offline vision · App-private encrypted vault · Minimalist Professional Dark UI

---

## 1. Product Specs

| Item | Decision |
|------|----------|
| App Name | Skry |
| Tagline | Your AI-powered privacy sentinel for your local gallery |
| Stack | Kotlin, Jetpack Compose, Material 3, Coroutines, Room, MVVM, WorkManager |
| Market | Global; **English UI + English OCR first** |
| Document detection | **OCR + rules** (no self-training); vision classification reserved for Phase 5 |
| Network | **Never add `INTERNET`**; ML Kit Bundled models only |
| Vault | App-private `EncryptedFile` + Android Keystore + Biometric |
| Permissions | `READ_MEDIA_IMAGES` only (`minSdk 33`) |
| License intent | MIT / open source friendly |

### Corrections vs earlier draft plan

- Do **not** require `DeviceIdle + Charging` for all scans (too rare) → manual Scan Now + gentle background constraints
- Do **not** use deprecated RenderScript
- No public API for OEM “system secure folder” → App Vault only
- Memes / precise handheld-ID vision → deferred to Phase 5 (optional pre-trained TFLite, no self-train)

---

## 2. Architecture

```
MediaStore → MediaRepository → Room (PhotoEntity)
                ↓
         WorkManager / ScanNow
                ↓
    PrivacyScanner | QualityEngine | VaultService
                ↓
     Dashboard / Risk / Clean / Vault (Compose UI)
```

### Package layout

```
com.yishenghuang.skry/
  data/          # Room, MediaRepository, DAOs
  domain/        # scanners, pHash, Luhn, vault
  worker/        # FullScanWorker
  ui/
    theme/       # Color, Type, Theme, AppDimensions, modifiers
    dashboard/
    risk/
    cleaner/
    vault/
    components/  # SkryCard, HairlineBorder, EmptyState, ProgressRing
  MainActivity.kt
```

---

## 3. Visual System (hard constraints)

### Design tokens

| Token | Value | Use |
|-------|-------|-----|
| Background | `#0D0D0D` | True deep dark |
| Surface / Glass | `#1A1A1A` | Bento / list cards |
| Primary | `#6366F1` Electric Indigo | Primary actions, progress ring |
| Accent | `#94A3B8` Slate | Secondary text/icons |
| Risk | `#EF4444` muted Crimson | Risk counts / alerts only |
| Hairline | `#FFFFFF1A` | 1dp stroke; **no elevation shadows** |
| Card radius | 28dp | Main cards |
| Button radius | 16dp | Buttons |
| Thumb radius | 12dp | Thumbnails |
| Spacing | 8 / 16 / 24 / 32 | Strict 8dp grid |

### Four polish rules

1. **No Shadows** — hairline borders + color layers only
2. **Restricted Palette** — 90% mono; color only for Primary Action / Risk
3. **Micro-animations** — press scale `0.98f` + spring
4. **Empty States** — minimalist outline illustration + low-contrast grey, airy layout

### Theme files

- `Color.kt` — full token set; **disable dynamic color**
- `Type.kt` — clean Sans; headlines slight negative letter-spacing (~-0.02em)
- `Theme.kt` — **forced dark only** (flagship dark product)
- `AppDimensions.kt` — all spacing/radii/strokes; no magic numbers in screens
- Shared modifiers — `hairlineBorder()`, `pressScale()`, `glassSurface()`

### Dashboard (Bento)

- Header: large title **Gallery Health** + thin-stroke circular progress (Indigo → Violet gradient)
- Grid: large Privacy Audit card (High Risk count in bold crimson) + small Duplicates + small Blurry Assets
- Glass: `#1A1A1A` + subtle blur; no shadows; macOS Control Center layered flat look

### Risk Explorer

- Wide cards; left thumbnail 12dp radius
- Frosted blur simulation over sensitive regions in UI preview
- Small Shield overlay (restrained, not cheap glow spam)
- Monochromatic tags (light grey on dark grey)
- Icon-only Quick Action (Trash / Archive); card lightens on press

---

## 4. Phased Delivery

### Phase 0 — Design system + shell (current)

- Replace default purple theme with Skry tokens
- Navigation: Dashboard / Risk / Clean / Vault
- Shared components: SkryCard, MonochromeTag, EmptyState, press scale
- Dashboard Bento with mock data
- Risk Explorer list with mock data

**Done when:** Screenshots look flagship-ready without backend.

### Phase 1 — MediaStore + Room

**Deps:** Room, KSP, ViewModel, Navigation, WorkManager

**PhotoEntity fields:**

- `id`, `uri`, `displayName`, `dateAdded`, `size`, `width`, `height`
- `mimeType`, `isScreenshot`
- `pHash`, `scanStatus` (PENDING / DONE / ERROR)
- `findingsJson`
- `isBlurry`, `isLowQuality`, `isOverExposed`, `isUnderExposed`
- `qualityScore`
- `exifHasGps`, optional lat/lng markers
- `vaultStatus` (NONE / MOVED / REDACTED)

**MediaRepository:** ContentResolver scan, incremental by MediaStore id / dateAdded  
**Permission:** `READ_MEDIA_IMAGES` + runtime flow

### Phase 2 — Privacy Audit

**Dep:** `com.google.mlkit:text-recognition` **Bundled**

| Finding | Logic |
|---------|--------|
| Passport / ID | Keywords + MRZ `^[A-Z0-9<]{30,44}$` |
| Credit Card | 13–19 digits + Luhn |
| SSN-like | `\d{3}-\d{2}-\d{4}` |
| Sensitive screenshot | password / account / verification / OTP / phone + nearby digits |
| GPS leak | Exif GPS present → LOCATION_EXIF |
| Document heuristic | ID keywords; handheld → POSSIBLE_ID_PHOTO low confidence |

**Scan:** batches of 20; `RequiresBatteryNotLow` (+ optional charging); Dashboard **Scan Now** foreground job with progress

### Phase 3 — Smart Cleaner

- pHash 8×8; Hamming grouping; best quality starred, rest suggested delete
- Laplacian variance blur (~threshold 100)
- Exposure via mean luminance
- Screenshot path/name; expired OTP/payment heuristics by age
- Long screenshot aspect ratio > ~3
- **Memes: skip** until Phase 5
- Delete via `MediaStore.createDeleteRequest`

### Phase 4 — Safety Vault

- Permanent mosaic from OCR boxes → new file / vault export
- `EncryptedFile` + Keystore under `files/vault/`
- Biometric unlock
- Vault tab UI

### Phase 5 (reserved) — Optional vision, no self-training

- Only if a ready, redistributable offline TFLite classifier exists
- Targets: handheld ID, hard document shots, memes
- Do not promise vision accuracy in marketing before this ships

---

## 5. UI build order

1. Theme + AppDimensions  
2. Shared components  
3. Dashboard Bento (mock)  
4. Risk Explorer (mock)  
5. Wire Phase 1 data  
6. Privacy findings  
7. Cleaner + Vault  

---

## 6. Dependencies by phase

- Phase 0: Compose BOM; optional material-icons-extended  
- Phase 1: Room + KSP, Navigation, ViewModel, WorkManager  
- Phase 2: ML Kit text-recognition bundled  
- Phase 3: none heavy  
- Phase 4: security-crypto, Biometric  

**Manifest:** `READ_MEDIA_IMAGES` only. Never `INTERNET`.

---

## 7. Testing

- Unit: Luhn, pHash Hamming, keyword rules, expired-screenshot heuristics  
- Instrumented: permission deny, delete request intents  
- Manual: English passport/card/OTP samples in private testdata (never commit real PII)

---

## 8. Trust policy (README later)

1. Zero Network — not a single byte sent out  
2. Local Intelligence — PII on-device only  
3. Full Transparency — open source intent (MIT)

---

## 9. Execution log

| Date | Action |
|------|--------|
| 2026-07-28 | Plan filed; Phase 0 + Phase 1 implementation started |
| 2026-07-28 | Phase 0 done: theme, Bento dashboard, Risk/Clean/Vault shells |
| 2026-07-28 | Phase 3 done: QualityAnalyzer, duplicates/blur/expired cleaner + system delete |
| 2026-07-28 | Phase 4 done: mosaic redaction, EncryptedFile vault, biometric unlock UI |
| 2026-07-29 | UI polish: unified copy/headers, adaptive icon, animated splash backdrop |
