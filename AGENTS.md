# AGENTS.md

Working notes for agents and contributors in this repository. Everything below is
derived from `settings.gradle`, `app/build.gradle`, `gradle/libs.versions.toml`,
`gradle/wrapper/gradle-wrapper.properties`, `gradle/gradle-daemon-jvm.properties`
and `.github/workflows/`. Re-read those files when a detail matters instead of
trusting this summary.

## Project context

KeepScore is a score-keeping app for board and card games (Android, single
module). It is a fork of Nolan Lawson's original app, maintained at
<https://github.com/carlo-colombo/KeepScore>.

- Original author, original code: Nolan Lawson — <https://github.com/nolanlawson/KeepScore>
- License: WTFPL v2 (`LICENSE`). Keep `NOTICE` and the in-app "About" credits
  intact; attribution is not a legal requirement under the WTFPL but it is the
  project's standing rule.
- App ID and namespace: `ovh.litapp.keepscore`; sources live under
  `app/src/main/java/ovh/litapp/keepscore/`.

## Architecture

- **Views first, Compose incrementally.** Activities, fragments and XML layouts
  are still the norm; Jetpack Compose is adopted one screen at a time. The
  pattern to copy is the New Game screen: `NewGameActivity.kt` is a thin host
  calling `setContent { MaterialTheme { NewGameScreen(...) } }`, with the UI in
  `ui/newgame/NewGameScreen.kt`. New Compose code belongs in `ui/<screen>/`.
- **Screens.** `MainActivity` (game list, menus, import/export/share),
  `GameActivity` as the shared base for `PortraitGameActivity` /
  `LandscapeGameActivity`, `NamePlayersActivity`, `HistoryActivity`
  (charts/tables in `fragment/`, custom views in `widget/`),
  `SettingsActivity`, `OrganizePlayersActivity`, `AboutActivity`.
- **Persistence.** SQLite through `db/GameDBHelper` (a `SQLiteOpenHelper`) with
  the `db/Game` and `db/PlayerScore` models. The backup/export format is
  `serialization/GamesBackup` + `GamesBackupSerializer` (XML, GZIP-compressed);
  it must stay readable by released versions, so treat it as a stable contract.
- **Background work.** `data/work/BackupScheduler` enqueues one unique daily
  `PeriodicWorkRequest` (`BackupWorker.UNIQUE_NAME`,
  `ExistingPeriodicWorkPolicy.KEEP`, battery-not-low constraint) from
  `KeepScoreApplication.onCreate`. `BackupWorker` writes the daily backup. Do not
  reintroduce `AlarmManager` + `IntentService`.
- **Storage and sharing.** Backups and CSV exports go to the app-specific
  external files dir (`getExternalFilesDir`, `SdcardHelper`), falling back to
  internal storage; nothing is written to shared storage. Sharing goes through
  the `${applicationId}.fileprovider` `FileProvider` declared in the manifest with
  `res/xml/file_paths.xml`, and receivers get an explicit read grant.

## Build and toolchain

- Gradle 9.8.1 through the committed wrapper; AGP 9.4.1; Kotlin 2.4.21 compiled
  by AGP's built-in Kotlin support (the only Kotlin plugin applied is
  `org.jetbrains.kotlin.plugin.compose`, at the root, with `apply false`).
- JDK 21: `compileOptions` target 21 — and `gradle/gradle-daemon-jvm.properties`
  pins the JETBRAINS 21 daemon toolchain. Keep those, the CI action and
  `gradle.properties` in step; a mismatch makes Gradle download a second JDK.
- All dependency versions live in the version catalog `gradle/libs.versions.toml`.
  Add a library alias there and reference `libs.*` in `app/build.gradle`; never
  inline coordinates in the module build file.
- SDK levels: `minSdk 34`, `compileSdk 36`, `targetSdk 36` (Android 16). Do not
  lower `minSdk`, and keep behavior correct at the target SDK rather than
  reintroducing legacy compatibility shims.
- Release builds run R8 (`minifyEnabled`), `shrinkResources` and
  `lintVitalRelease`. Framework-instantiated classes need keeps in
  `app/proguard-rules.pro`; verify with `:app:assembleRelease`, not just debug.
- `versionCode`/`versionName` default to `28` / `1.5.1` and are overridden in CI
  via `-Pandroid.versionCode` / `-Pandroid.versionName` (read with
  `findProperty`). Never hard-code a release bump in the build file.
- Lint: `abortOnError = true`, `warningsAsErrors = false`, baseline
  `app/lint-baseline.xml` recording legacy debt. New code must not add baseline
  entries — fix the finding instead of suppressing it.

## Common commands

```bash
./gradlew :app:assembleDebug            # debug APK
./gradlew :app:testDebugUnitTest        # JVM unit tests (JUnit4 + Robolectric)
./gradlew :app:lintDebug                # lint the debug variant
./gradlew :app:assembleRelease          # minified + shrunk release APK
```

Useful extras:

```bash
./gradlew :app:lintRelease                                  # what CI validates before tagging
./gradlew :app:testDebugUnitTest --tests ovh.litapp.keepscore.db.GameDBHelperTest
./gradlew --stop                                            # free the daemon, e.g. before a second heavy task
```

Debug APK: `app/build/outputs/apk/debug/app-debug.apk`. Release APK:
`app/build/outputs/apk/release/app-release-unsigned.apk` (CI signs it separately).
`local.properties` (gitignored) needs `sdk.dir=/path/to/Android/Sdk`.

## CI and release invariants

Two workflows, and the invariants below are load-bearing — do not "simplify" them
away without a decision from the maintainer.

- **`pr.yml` (pull requests, `workflow_dispatch`)**: `permissions: contents: read`
  for the whole workflow; no PR-triggered job may hold a write token. A `changes`
  job applies the docs-only path filter (`documentation/**`, `docs/**`, `gimp/**`,
  `*.md`, `LICENSE`, `NOTICE`, `.github/dependabot.yml`) and the always-running
  `required-checks` job keeps the required branch-protection check green instead
  of leaving it pending for docs-only PRs. `concurrency.cancel-in-progress: true`.
  Dependabot PRs are auto-merged with `DEPENDABOT_TOKEN` and skipped with a
  warning when the secret is absent.
- **`build.yml` (push to `main` only)**: `contents: write` is scoped to the single
  build job; `concurrency.cancel-in-progress: false` so a release is never
  cancelled once its tag exists.
- **Validate before tag.** The order is fixed: resolve the next tag with a dry run
  (`anothrNick/github-tag-action`, `DRY_RUN: true`) → `:app:assembleRelease` →
  `./gradlew --stop` → validate with `:app:testDebugUnitTest` and
  `:app:lintRelease` → push the tag (`CUSTOM_TAG` pinned to the resolved tag) →
  `zipalign` + `apksigner` (build-tools version resolved from the SDK) → GitHub
  release. A failure anywhere before the tag push must leave no tag behind; a
  release-only breakage must not reach signing.
- **Signing is optional but never blocks a release.** `SIGNING_KEY` (base64
  keystore), `ALIAS`, `KEY_STORE_PASSWORD` produce a properly signed APK. When
  they are missing or unusable, CI generates a throwaway key so the release still
  publishes, and the release body says the APK is for testing only. Keep that
  fallback: every push to `main` publishes a release.
- **`setup-jdk-gradle` is the single source of the JDK setup** (JetBrains 21 +
  `gradle/actions/setup-gradle`) and must stay in lockstep with
  `gradle/gradle-daemon-jvm.properties`. `actions/checkout` stays in the calling
  workflow because a local composite action is only resolvable after checkout.
- **Every `uses:` is pinned to a full commit SHA** with a trailing version
  comment. Dependabot (weekly, `gradle` and `github-actions` ecosystems) bumps
  them; do not replace pins with floating tags.
- Release assets, in order: `keepscore-<tag>.apk`, `keepscore-<tag>.apk.sha256`,
  `LICENSE`, `NOTICE`.

## Testing

- JVM unit tests: JUnit4 + Robolectric, in `app/src/test/java/…`, annotated
  `@RunWith(RobolectricTestRunner.class)` and `@Config(sdk = 34)`, using
  `ApplicationProvider.getApplicationContext()`. They must be hermetic — no
  device, no emulator, no network — because CI runs only these.
- AndroidX test + Espresso are on `androidTestImplementation` for
  `app/src/androidTest`, but nothing in CI executes them; put anything that must
  be verified under `app/src/test`.
- Put tests next to the code they cover and name them `<ClassUnderTest>Test`
  (`GameDBHelperTest`, `GamesBackupSerializerTest`, `SettingSetHelperTest`,
  `CSVUtilTest`, `TimeUtilTest`, `FunctionsDateTest`). Prefer pure helpers
  (`util/`, `serialization/`) over new Robolectric-dependent code paths.
- Prefer adding a regression test with every behavior change, especially around
  persistence, backup compatibility and the delta/score arithmetic.

## Modernization roadmap

1. **Compose the remaining screens**, following the New Game pattern
   (`ui/<screen>/` screen composable + thin activity host): settings, the
   scoring/game screens, history, organize players, about. Delete the XML layout
   and legacy helper code a screen leaves behind; do not keep two
   implementations of the same screen alive.
2. **Move persistence to Room** under `db/`, replacing `GameDBHelper`. The
   on-disk schema and the XML/GZIP backup format must stay compatible with
   released versions; start with migration + backup round-trip tests.
3. **Rewrite Organize Players.** `OrganizePlayersActivity` still relies on the
   legacy `widget/dragndrop/DragSortListView`; reimplement it in Compose and
   remove the old view code.
4. **Burn down debt.** Retire entries from `app/lint-baseline.xml` as screens are
   rewritten, and keep this file and `README.md` in step with the build files.

## Documentation ownership

`README.md`, `AGENTS.md` and `NOTICE` describe the project's public surface and its
attribution. Update them in the same change that alters the build commands,
versions, CI pipeline or credits.
