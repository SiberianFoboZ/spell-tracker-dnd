<div align="center">

# 🪄 Spell Tracker

**Android app for tracking D&D 5e spell slots per PHB rules**

[![Release](https://img.shields.io/badge/release-v2.5.4-7c3aed?style=flat-square&logo=github)](https://github.com/SiberianFoboZ/spell-tracker-dnd/releases/tag/v2.5.4)
[![License](https://img.shields.io/badge/license-MIT-22c55e?style=flat-square)](LICENSE)
[![Android](https://img.shields.io/badge/Android-7.0--16-3DDC84?style=flat-square&logo=android&logoColor=white)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0.21-7F52FF?style=flat-square&logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Compose](https://img.shields.io/badge/Jetpack%20Compose-2024.10-4285F4?style=flat-square&logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)

</div>

---

## What is this

**Spell Tracker** is a minimalist helper for D&D 5e casters: it shows the current
state of spell slots, pact magic, and arcanums on a single screen. No accounts, no
analytics, no internet — only local data that survives app restarts.

Current version: **v2.5.4** — slot-stretching fix: cells stretch only in width,
height stays fixed.

---

## 🆕 What's new in v2.5.0

### 👥 Multi-character support
- **Swipe left** on the home screen → character list. Switching preserves the **full state** of every character: classes, slots (regular / pact / arcanum), custom slots, prepared spells.
- **Long-press** a character row → an edit dialog: name (text persists across reopens) + delete. Auto-focus and keyboard appear the moment the dialog opens.
- Each character's data is stored as a separate blob in `SharedPreferences`. On first launch, the existing state migrates into "Character 1".

### 🔮 Dynamic slots (Stage 21)
- `total 1..5` → **1 row** of regular slots (48dp)
- `total 6..10` → **2 rows** of reduced slots (38dp, ≈80%)
- `total 11..20` → **numeric range** `remaining / total` (counter subtracts, doesn't add up)
- `CustomSlot.total` raised from 10 to **20**
- New die type `★` for **ultimate abilities** without a numeric volume

### 🔐 Release APK signing
- Personal `keystore` (RSA 2048, 10000-day validity) replaces the debug key — no more "signed with debug key" warning on install.
- **GitHub Actions** on `push` of a `v*` tag decodes the keystore from Secrets, signs the APK, verifies the signature with `apksigner`, and attaches the artefact to the GitHub Release.

### 🎬 Smooth transitions
- All screens now switch with a **slide + fade** animation (300ms) — no more abrupt jumps.

---

## ✨ Features

### 📚 Classes
- **9 D&D 5e classes** (PHB + XGE): Bard, Cleric, Druid, Paladin, Ranger, Sorcerer, Warlock, Wizard, Artificer
- **Multiclass**: add any number of classes, each with their own level
- The slot count is computed **automatically** from class and level (no manual entry like "I have 3 first-level")

### 🔮 Spell slots
- **9 spell levels** for each caster
- Tap a slot → mark as used
- Restore with **"Long rest"** / **"Short rest"** buttons at the bottom

### 🟣 Pact magic (separate block)
- For **Warlock** only
- Restored on **short rest** (per PHB)
- Distinct colour from regular slots

### 🟡 Arcanums (separate block)
- **Level VI–IX** spells for Warlock
- Unlocks at **Warlock level 11/13/15/17** → 1/2/3/4 arcanums
- Restored **only on long rest** (per PHB)
- The whole block is hidden when Warlock is not selected

### 📖 Spell catalog
- Built-in **spell database** in Room (local, offline)
- **Spells** screen with filters by level, school, class
- **SpellDetail** screen with full description, components, duration

### 💾 Persistence
- Classes, levels, and used slots **persist** across sessions
- Data — local only (SharedPreferences for state, Room for the spell database)
- No tracking, no analytics, no ads

---

ASCII schematic of the home screen for a Wizard 9 + Warlock 5 multiclass:

```
┌─────────────────────────────────────────┐
│ Spell Tracker                           │
├─────────────────────────────────────────┤
│ Classes: Bard 1  Cleric 3  Wiz 9  ...   │
├─────────────────────────────────────────┤
│ PACT MAGIC                       Warlock │  ← (if warlockLevel > 0)
│   V  ▮▮▮▮▮  (2/2)                       │
├─────────────────────────────────────────┤
│ SPELL SLOTS                             │
│   I    ▮▮▮▮ (4/4)   IV  ▮▮▮  (3/3)      │
│   II   ▮▮▮  (3/3)   V   ▮▮   (2/2)      │
│   III  ▮▮   (2/2)                      │
├─────────────────────────────────────────┤
│ ARCANUMS                                │  ← (if warlockLevel ≥ 11)
│   VI  ▮ (1/1)    VII ▮ (1/1)            │
│   VIII▮ (1/1)    IX  ▮ (1/1)            │
├─────────────────────────────────────────┤
│ [ Long rest ]   [ Short rest ]          │
└─────────────────────────────────────────┘
```

---

## 🏗 Stack

| Layer | Technology |
|-------|-----------|
| Language | **Kotlin 2.0.21** |
| UI | **Jetpack Compose** (BOM 2024.10.01) + **Material 3** |
| State | **Lifecycle 2.8.7** + `StateFlow` / `SharedFlow` |
| Navigation | **Navigation Compose 2.8.4** |
| Local DB | **Room 2.6.1** (spell catalog) + **SharedPreferences** (state) |
| Build | **AGP 9.1.1** + Gradle + KSP |
| Min SDK | **24** (Android 7.0) |
| Target SDK | **36** (Android 16) |
| Java | **17** |

**No third-party dependencies beyond AndroidX / Compose / Room.** Single-Activity + Compose Navigation + MVVM.

---

## 🏛 Project structure

```
app/src/main/java/com/example/spelltracker/
├── MainActivity.kt              # Single-Activity host (Compose)
├── data/
│   ├── Classes.kt               # Definitions of 9 classes
│   ├── ClassFilter.kt           # Class-filtering logic
│   ├── Spell.kt                 # Spell model
│   ├── SpellDao.kt              # Room DAO
│   ├── SpellDatabase.kt         # Room @Database
│   ├── SpellParser.kt           # Raw-data parser
│   ├── SpellRepository.kt       # Spell repository
│   └── SpellStorage.kt          # SharedPreferences + StateFlow
├── ui/
│   ├── detail/                  # Spell details screen
│   ├── home/                    # Home screen (slots, pact, arcanums)
│   │   ├── HomeScreen.kt        # 3 sections: Pact → Slots → Arcanums
│   │   └── HomeViewModel.kt     # HomeState + HomeEvent (Flow + Combine)
│   ├── nav/AppNavigation.kt     # Compose Navigation graph
│   ├── spells/                  # Spell list screen
│   └── theme/                   # Color, Theme, Type (Material 3)
└── res/values/strings.xml       # All UI strings (i18n-ready)
```

**Key patterns:**

- **MVVM** — `HomeViewModel` combines `StateFlow` via `combine(...)` → `HomeState` → `HomeScreen`
- **Conditional rendering** — sections like `if (warlockLevel == 0) return` render only when there's data
- **Single source of truth** — `SpellStorage` holds state in SharedPreferences and replicates it in `StateFlow` for reactive UI
- **Reactive UX** — `animateColorAsState`, `animateDpAsState`, haptic feedback on tap

---

## 🔨 Build

### Requirements
- **JDK 17**
- **Android SDK** with platform `android-36` and build-tools compatible with AGP 9.1.1
- Internet access for the first dependency download (Gradle)

### Commands

```bash
# Clone
git clone https://github.com/SiberianFoboZ/spell-tracker-dnd.git
cd spell-tracker-dnd

# Build release APK
./gradlew assembleRelease
# → app/build/outputs/apk/release/spell-tracker-v2.5.4.apk
```

Windows (PowerShell):

```powershell
.\gradlew.bat assembleRelease
```

### Install on a device

```bash
adb install -r app/build/outputs/apk/release/spell-tracker-v2.5.4.apk
```

### Release APK signing (Stage 23)

The release APK is signed with a **personal keystore** (`keystore/spell-tracker-release.jks`)
rather than the debug key, so users no longer see the "signed with debug key" /
"installed from unknown source" warning.

If `keystore.properties` is missing, the build falls back to the debug keystore
(for backward compatibility).

`keystore/spell-tracker-release.jks` and `keystore.properties` are listed in `.gitignore`
— real passwords must **never** reach the repository. For collaboration, transmit
`keystore.properties` (and the keystore itself when needed) **separately from the code**
over a secure channel, or use `keystore.properties.example` as the template for your
own generation.

**Manual generation** (when you need your own alias / password / DN):

```powershell
keytool -genkeypair -v `
    -keystore keystore\spell-tracker-release.jks `
    -alias spell-tracker `
    -keyalg RSA -keysize 2048 -validity 10000 `
    -storepass YOUR_STORE_PASSWORD `
    -keypass YOUR_KEY_PASSWORD `
    -dname "CN=Spell Tracker, OU=Personal, O=vk241, C=RU"
```

For CI to sign a Release APK, the keystore and `keystore.properties` are encoded as
base64 and stored as GitHub Actions repository secrets
(`KEYSTORE_BASE64`, `KEYSTORE_PROPERTIES_BASE64`). The CI workflow
`.github/workflows/release.yml` decodes them on tag push and signs the APK
automatically.

> ⚠️  If a device already has an APK signed with a different key (e.g. an old
> debug APK), updating on top of it **won't work** due to mismatched signatures.
> Uninstall the old version first (`adb uninstall com.example.spelltracker` or
> manually in Settings).

---

## 📜 Release history

| Version | What's new | Date |
|---------|-----------|------|
| **v2.5.4** | Fix: slots stretch only in width (removed `aspectRatio`; height fixed at 48/38dp) | 2026-07-03 |
| **v2.5.3** | Custom = ClassCard (shared `GridCard`); slots now distribute across the row width | 2026-07-02 |
| **v2.5.2** | `longRest` now resets ALL custom slots; custom-slot row styled like pact magic (56dp badge, 16sp title) | 2026-07-02 |
| **v2.5.1** | Fix: custom slots reset on rest + Auto-Backup | 2026-07-02 |
| **v2.5.0** | Multi-character support + dynamic slots + release APK signing | 2026-07-02 |
| v2.4.2 | Hide the "ARCANUMS" block when `warlockLevel == 0` | 2026-06-13 |
| v2.4.1 | Pact Magic block moved out separately (revert of unification) | 2026-06-13 |
| v2.4.0 | Warlock arcanums (VI–IX), separate block | 2026-06-13 |
| v2.3.0 | UX redesign: larger slots, haptic feedback, animations | 2026-06 |
| v2.2.0 | Full multiclass, 9 classes | 2026-05 |
| v2.1.1 | Fixes in slot calculation | 2026-05 |
| v2.1.0 | Warlock pact magic, 8 classes | 2026-05 |
| v2.0.0 | Full rewrite: Kotlin + Jetpack Compose | 2026-04 |
| v1.1.0 | Support for 5 classes | 2026-03 |
| v1.0.0 | First release (Java + XML) | 2026-02 |

All releases: [github.com/SiberianFoboZ/spell-tracker-dnd/releases](https://github.com/SiberianFoboZ/spell-tracker-dnd/releases).

---

## 🤝 Contributing

PRs and bug reports are welcome — open an issue before larger changes.

### Adding a new class

1. Open `data/Classes.kt` and add a new `ClassDef` with all fields
2. Add the PHB / XGE progression table to the slot calculator
3. Add class rendering in `HomeScreen` (if it needs special rest logic)
4. Run `./gradlew test` — PHB-table tests should pass

### Architectural rules

- **State** lives only in `SpellStorage` (single source of truth)
- **Composables** should be side-effect-free; side effects belong in `LaunchedEffect` / `ViewModel`
- **Strings** — all in `res/values/strings.xml`, no hard-coded UI strings

---

## 📋 Roadmap

- [ ] Session snapshot (export / import)
- [ ] Dynamic Color (Material You) on Android 12+
- [ ] Localization: Russian, Ukrainian
- [ ] Publication on Google Play (with a personal keystore)

---

## ⚖️ Legal

**Spell Tracker** is an unofficial fan project. **Dungeons & Dragons** and related
terms (PHB, XGE, arcanums, pact magic, etc.) are trademarks of **Wizards of the Coast LLC**.
This project uses material from the **System Reference Document 5.1** under the
**Creative Commons Attribution 4.0 International License (CC BY 4.0)**.

The source code is distributed under the [MIT License](LICENSE).

---

## 🙏 Acknowledgments

- **Wizards of the Coast** — for D&D 5e SRD, without which this project would not exist
- **Google / JetBrains** — for Kotlin, Jetpack Compose, and the Android ecosystem
- Everyone who playtested and gave feedback

---

<div align="center">

🪄 **Magic is just a schedule of slots. Keep track of them — and let the heroes watch your back.**

*Made with ☕ and a d20.*

</div>
