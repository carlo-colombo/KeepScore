# KeepScore: complete Kotlin + Compose migration plan

**Status date:** 2026-10-19  
**Baseline:** `main` at `8f6b20c` (`1.6.0-build.19`), 80 production Java files, 15 Kotlin files, and 35 XML layout files.

## Goal

Move all first-party production code to Kotlin and all interactive app UI to Jetpack Compose. Remove the legacy View/fragment/ListView/custom-widget UI and its layout/menu XML. Keep only XML that remains an Android resource/configuration contract (manifest, values, drawable/adaptive icons, backup rules, FileProvider paths, and localized string resources).

## Invariants

- Keep `applicationId` `ovh.litapp.keepscore`, `minSdk 34`, `compileSdk/targetSdk 36`, Gradle 9.8.1, AGP 9.4.1, Kotlin 2.4.21, JDK 21, Room schema v5, and release signing/pipeline behavior.
- Preserve Room v1→v5 migrations and existing installed database contents. Do not change SQL column names or CSV encoding of score history/timestamps without a tested migration.
- Preserve compatibility with every released XML/GZIP game backup (versions 1–5), FileProvider/SAF import/export, WorkManager auto-backup, and setting-set data.
- Keep the calculator dark theme, responsive scoring layout (portrait column; landscape two-column grid up to 8, usable portrait column above 8), undo/redo, score/history batching, round totals, highlight, autosave, deep links, and accessibility semantics.
- Compose is the only UI implementation: no feature flags and no parallel legacy screen retained after cutover.
- Every integrated wave is a commit on `main`; every push runs CI and publishes a signed release. Keep the activity/package IDs stable so updates remain installable.

## Execution waves

### Wave 1 — remaining user-facing screens (parallel, isolated worktrees)

Separate owners; each owns one activity/screen, its new `ui/<screen>/` package, and the screen-specific XML layouts/menus it replaces. Shared theme, strings, Room APIs, backup contracts, and `DialogHelper` are read-only dependencies in this wave.

1. **Main** — migrate game list, date grouping, selection mode, rename/delete, backup import/export, CSV export, sharing and menus to a Kotlin activity + Compose screen. Keep file/Room operations off the main thread and keep SAF/FileProvider contracts.
2. **Name Players** — migrate the 2–30 player name/color form, autocomplete suggestions, orientation-independent responsive layout, and new-game intent contract.
3. **History** — migrate tabs, round/player tables, time/round charts and zoom to Compose; use Compose Canvas for charts rather than retaining `LineChartView`/ViewPager fragments.
4. **About** — migrate credits/version/license/links to Compose and keep Nolan Lawson/upstream attribution. Avoid a layout XML/WebView screen if the source content can be represented as Compose text/links.

Activities must remain callable under their existing class names/contracts until the wave is integrated. Do not edit the same activity or shared helper from two lanes.

### Wave 2 — shared dialog and UI infrastructure

After Wave 1 integration, move shared `DialogHelper` interactions (delta calculator, color chooser, history actions, confirmations, import/export selectors) to Compose dialog/state APIs. Replace the `ambilwarna` dependency with a Compose-native color picker or a small first-party Compose picker. Convert action-bar menus to Compose `TopAppBar`/dropdowns. Preserve exact settings and result contracts.

### Wave 3 — Kotlin production-code conversion (parallel by package ownership)

Once screen boundaries are stable, convert remaining Java packages in disjoint lanes:

- Room/database facade, entities, DAO, `Game`/`PlayerScore`/`Delta`/`GameSummary` (retain Room schema/migration behavior and Java-facing activity contracts until call sites move).
- XML/GZIP backup serialization and backup worker/scheduler (retain released backup compatibility).
- Pure utilities and data models/adapters that remain referenced.
- Helpers/preferences/storage/network-independent Android services and application class.
- Activity hosts and remaining `PortraitGameActivity`/`LandscapeGameActivity` can be collapsed into a single Kotlin adaptive activity after call sites/manifest are updated and process-death/rotation state tests pass.

Use Kotlin-friendly Room KSP only if it is needed for Kotlin entities; keep schema export checked in and test migrations against real legacy v1/v4 databases.

### Wave 4 — remove legacy UI surface and Java

After all screens and dialogs have Compose replacements:

- Delete unused `res/layout*`, `res/menu`, View/Fragment/Adapter/custom-widget classes, old drag-sort implementation, and obsolete dependencies only after repository-wide reference checks.
- Convert remaining first-party `app/src/main/java/**/*.java` production files and `app/src/test/**/*.java` tests to Kotlin; preserve package names, Room schema, parcel/backup contracts, and release behavior.
- Keep necessary Android XML resources: manifest, values/strings/locales, adaptive icons/vector drawables, FileProvider/backup rules. The goal is no first-party Java code and no XML layout/menu UI, not removal of Android's required resource/manifest XML.

### Wave 5 — final hardening and delivery

- Independent read-only review after each integrated wave; fix P1s before release.
- Run `:app:assembleDebug`, `:app:testDebugUnitTest`, `:app:lintDebug`, `:app:assembleRelease`, `:app:lintRelease` on the exact integration head.
- API 36 emulator smoke/regression: clean install, upgrade install from last signed release, portrait↔landscape rotation with in-progress score/undo state, 2/8/9/30 players, all settings/backup/history flows, accessibility scan, no app fatal logs.
- Burn down `app/lint-baseline.xml`; never use the baseline to silence new findings.
- Update README/AGENTS, issue #1, screenshots and release notes. Publish a real-key-signed release per integrated wave.

## Definition of done

1. `find app/src/main/java -name '*.java'` returns **0** first-party production Java files; tests are Kotlin as well.
2. `find app/src/main/res -path '*/layout*/*.xml' -o -path '*/menu/*.xml'` returns **0** first-party screen/layout/menu XML files (required manifest/config/value/icon/provider XML remains).
3. Every screen is Compose, all listed user flows have focused tests or device verification, all 127+ tests pass, lint has no unreviewed new baseline entries, release R8 build passes, and the latest release installs/updates with the same signing certificate.
