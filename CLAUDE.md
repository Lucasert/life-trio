# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

life-trio is a local-first Android app for personal workflows: Memo (notes with tags/images/speech), Ledger (expense/income with budget tracking), Plan (recurring plans with China legal workday support), and Password vault (biometric-locked). Built with Kotlin + Jetpack Compose + Room. No account system, no cloud sync, no network. UI text is Chinese.

## Commands

```bash
# Build debug APK
./gradlew assembleDebug

# Run all unit tests
./gradlew testDebugUnitTest

# Run a single test class
./gradlew testDebugUnitTest --tests "com.lifetrio.plan.PlanSchedulerTest"

# Instrumented tests (requires connected device/emulator)
./gradlew connectedDebugAndroidTest

# Lint
./gradlew lint
```

Toolchain: AGP 8.7.3, Kotlin 2.0.21, JDK 17, compileSdk 35, minSdk 26. Room schemas are exported to `app/schemas/` (KSP arg) — bump the DB version and provide a migration if you change any entity.

## Architecture

### Dependency Wiring — No DI Framework

`LifeTrioApp` (Application) creates a single `AppContainer` (`core/data/AppContainer.kt`), which constructs the Room database, repositories, `PlanScheduler`, and the password vault. The container is passed directly into each screen composable from `MainActivity`. There are no ViewModels — screens hold state with `remember`/`collectAsState` and call repositories directly.

### UI Layer

- `MainActivity.kt`: Compose Navigation `NavHost` with 5 bottom-tab destinations (Home/Memo/Ledger/Plan/Password). Must stay a `FragmentActivity` — `BiometricPrompt` for the password vault requires it.
- `ui/screens/Screens.kt`: ALL screen composables and their helpers live in this one ~1800-line file.
- `ui/components/UiComponents.kt`: shared components (tab bar etc.).

### Data Layer (Room)

- All entities in one file: `core/data/db/entity/Entities.kt`; DAOs per domain in `core/data/db/dao/`; one repository per domain in `core/data/repository/`.
- Money is stored as cents (`Long`); convert with `String.toAmountCents()` / `Long.toYuanText()` from Entities.kt.
- Memo images are copied into the app's private files dir; `MemoEntity.imageUris` stores local paths (comma-joined string).

### Plan Scheduling (the most intertwined subsystem)

- `PlanScheduler` (`plan/scheduler/`) is pure logic: given a `PlanEntity` rule (Daily/Weekly/Monthly/EveryNDays/LegalWorkday) it computes matching dates.
- `ChinaWorkdayCalendar` holds a hardcoded holiday/adjusted-workday table for 2026 only. User overrides live in the `workday_overrides` table and take precedence; `DeviceCalendarReader` (`plan/calendar/`) can import overrides from the device calendar (READ_CALENDAR permission).
- Occurrences are pre-materialized into `plan_occurrences`: `PlanRepository.generateOccurrences()` runs at app launch (yesterday → +31 days) and on plan add/update (−179 days → +31 days). The unique `(planId, date)` index makes regeneration idempotent.
- Unfinished occurrences are carried forward or skipped per `CarryStrategy`; completions are recorded separately in `plan_completions`.

### Password Vault — Not in Room

`password/` stores records in an encrypted binary file (`filesDir/password_vault.bin`) via `PasswordVaultCodec` + AES through Android Keystore (`AndroidKeystorePasswordVaultCrypto`). Unlock state is in-memory only; biometric auth gates the screen. Keep vault data out of the Room database and out of backups (`allowBackup=false`).

### Tests

Unit tests (`app/src/test/`) cover pure logic only: plan scheduling, ledger math, memo search, password vault codec/crypto round-trip (with a fake crypto). Keep business logic out of composables so it stays unit-testable this way.
