# Local completion scope — 2026-10-07

Authoritative delivery evidence and remaining runtime limitations: [LOCAL_VALIDATION_REPORT.md](LOCAL_VALIDATION_REPORT.md). Progress sections below retain historical failures and are superseded by the final report.

This pass completes the existing offline gallery audit, cleaner and encrypted vault. No publishing, remote pushes, signing changes, new vision models, language picker, widgets or export features. Preserve existing work. Only synthetic media in isolated emulators may be deleted.

## Bounded checklist

- [x] Restore a buildable baseline while retaining existing DAO availability filtering; migrate existing databases without losing vault records.
- [x] Reconcile accessible MediaStore contents, external deletions and permission changes; implement API 26–32, 33, and 34+ permission branches and reselection (runtime coverage: API 26/35/36).
- [x] Make deletion confirmation, cancellation, Android 10 retry, partial failures and post-delete lists/statistics consistent; guard repeated requests and lifecycle changes (API 29 runtime remains unverified).
- [x] Verify scan rules, decoding, failure/retry/progress and background continuation; avoid repeated expensive duplicate regrouping and overlapping scans.
- [x] Check risk/cleaner previews and selection, empty states, vault locking and failure feedback; maintain English/Chinese parity and accessible action labels (hardware/TalkBack limits in final report).
- [x] Run debug build, unit tests, lint and meaningful instrumentation; exercise available emulator versions with synthetic fixtures, including empty/large database cases and permission denial/revocation.
- [x] Audit merged manifest and privacy constraints; record website discrepancies here without modifying the website.
- [x] Deliver evidence and explicitly identify any unexecuted checks and remaining dependencies.

## Initial evidence

- README and master plan specify offline bundled Latin OCR + rules, pHash/quality cleanup, encrypted redacted copies and Android 8+.
- Cursor history records the compatibility changes and cleaner refresh work; current implementation still treats Android 10 authorization as completed deletion and only inserts during gallery sync.
- Initial working tree reports nine modified files; content diff currently includes PhotoDao availability filters. These filters reference a missing entity column.
- Baseline `:app:assembleDebug :app:testDebugUnitTest :app:lintDebug` fails in KSP: `no such column: galleryAvailable`. Tests/lint did not complete.
- ADB has no connected device. Installed system images include API 26, 35, 36.1 and 37.1. Runtime checks are pending.

## Historical progress (superseded by latest evidence below)

- Added schema v5 migration for `galleryAvailable`, preserving existing rows; atomic reconciliation hides inaccessible gallery rows while retaining encrypted vault associations and prior reviews. Reconciliation chunks IDs at 500 to avoid SQLite bind limits.
- Added Android 14+ selected-photo permission and reselection UI, checking actual permission state on resume. English and Chinese partial-access explanations added.
- Added instrumentation tests for selection/revocation, vault retention, repeated 2,200-row snapshots and empty snapshots. Compiled but not yet executed.
- Latest debug app and instrumentation APK builds succeeded; 18 JVM tests passed (14 privacy rules, 3 quality, 1 template). Log: `build/local-validation.log`.
- First lint run failed with an internal Kotlin analysis exception (`Unexpected owner function: null`); rerun pending. This is not a passing lint result.
- Official references: https://developer.android.com/about/versions/14/changes/partial-photo-video-access and https://developer.android.com/reference/android/app/RecoverableSecurityException .
- Remaining priorities: deletion state machine, scan cancellation/error accounting, preview/vault lifecycle, meaningful runtime tests, migration test and privacy audit. No checklist item is yet claimed complete end-to-end.
- Lint rerun completed analysis and reported 4 resource-access errors plus 58 warnings. Replaced the four Compose context resource lookups with `stringResource` values; verification of this latest correction remains pending. Internal analyzer crash did not recur.

## Second implementation and verification pass

Implemented:

- Serial deletion coordinator with an app confirmation on all versions, retained system-consent state, Android 10 authorization retry, 100-URI modern batches, duplicate-request guard and localized result/error feedback. Reconciles MediaStore after completion/cancellation instead of assuming requested IDs were deleted.
- Scan cancellation propagation, decoder failure accounting, normalized legacy EXIF orientation, one active scan, pause action, permission check in worker, bounded retries and fewer duplicate regroup operations.
- Exact Hamming candidate indexing replaces repeated all-pairs comparison; duplicate flags are committed atomically.
- Optional photo-location metadata permission and original-media EXIF read; denied metadata access is explained. Gallery access never depends on location metadata permission. Newly granted metadata access requeues old scans.
- Vault auto-lock on background, cancellation-safe preview loading, vault deletion confirmation and real deletion-result checks. App screenshots/recents are protected with FLAG_SECURE. Database excluded from backup/transfer, app backup disabled.
- Selection checkboxes now expose checked state and usable touch targets; stale selected IDs are filtered, previews reset zoom per image and report image-load errors. Home blurry/duplicate cards open the matching cleaner category. Added permanent-denial settings entry.

Authoritative evidence:

- `build/local-validation.log`: debug APK, instrumentation APK, 20 unit tests and lint passed (`BUILD SUCCESSFUL`). Latest small accessible-ID regroup change happened after this run and needs final rebuild.
- `build/instrumentation-api26-all.log`: 7 tests passed, including v4→v5 migration preserving vault/review, 2,200-row snapshots, bundled OCR (synthetic email/card), encrypted round-trip, malformed input, rotation, and actual MediaStore insertion/external deletion refresh.
- `build/instrumentation-api26-delete.log`: real Compose confirmation cancellation + recreation + synthetic deletion passed (1 test).
- Emulator API 26 is a fresh `SkryApi26` at port 5560 under `build/isolated-avds`; stopped after testing. Existing unrelated `KeyicValidation` / other emulator instances were not used.
- `tools/Start-SyntheticEmulator.ps1` creates only project-local AVD data and refuses ports already in use. API 35 `SkryApi35` at port 5562 is currently running; suite output goes to `build/instrumentation-api35-all.log`. Current install/test command handle: 62088 (do not restart without checking).
- Merged debug manifest contains selected-media and metadata permissions, and contains neither INTERNET nor ACCESS_NETWORK_STATE.

Historical remaining audit/work (see latest state below):

- Finish API 35 tests and selected-photo/revocation/denial runtime matrix; attempt API 29 or explicitly document unavailable version evidence. Check Android 16 if feasible.
- Vault move callback/busy state across recreation, redaction fallback when some detected regions lack OCR boxes, stale cleaner related-detail loads, gallery metadata changes, and rapid pause while indexing need final scrutiny.
- Complete UI checks for large font, Chinese, empty states, resume/pause and errors; add focused regression coverage where findings warrant it.
- Final full build/test/lint, merged-manifest audit, source diff review and honest completion report. No release or remote operation has occurred.

Website notes (read-only audit): add Android 14 selected-photo access and optional ACCESS_MEDIA_LOCATION wording, clarify local database backup/transfer exclusion, and avoid implying GPS scans are complete without metadata access. Record only; do not edit or deploy the privacy website.

API 35 first run terminated before tests (`Process crashed`). `dumpsys activity exit-info` identifies a process-start ANR, with several Google setup/services also ANRing during cold boot; no Skry Java exception was recorded. This is not a passing run and not yet attributed solely to the environment. With cold boot complete and the API 26 emulator stopped, a retry is running (exec handle 90964; authoritative output path `build/instrumentation-api35-retry.log`). Poll the existing handle before deciding whether to rerun.

## Latest verified state

- Schema is now v6: metadata edits invalidate stale findings and queue a rescan, while preserving encrypted vault copies. v4→v6 migration and all three gallery snapshot tests passed on API 35.
- Vault results survive recreation; busy guards serialize saves; backgrounding locks previews. Existing vault copies of an older source cannot silently authorize deletion of a newer source. Original deletion is an explicit action after reviewing the encrypted copy.
- Missing sensitive-region coordinates trigger whole-image mosaic, rather than an arbitrary center strip. GPS-only copies preserve pixels and remove metadata. Recognition remains heuristic and users must review copies.
- `build/local-validation.log`: debug app/test APKs, **22 JVM tests**, and lint completed successfully. Lint: **0 errors, 66 warnings** (style/resources/dependency notices); not claimed warning-free.
- `build/instrumentation-api35-current.log`: migration, gallery snapshots, OCR/encryption/rotation/MediaStore/redaction and Chinese 200% font tests passed. The deletion test failed in this historical suite; it is superseded by the dedicated passing rerun below. Permission case without an argument is intentionally skipped.
- `build/api35-delete-final.log`: **1 test passed**, real app cancellation, repeated-request guard, recreation, system cancellation, re-confirmation and actual deletion. Test synchronization now drains Compose effects before polling the system window; cleanup no longer deletes an already absent URI.
- `build/api35-delete-worker.log`: the **41-photo WorkerFlowTest passed**, including duplicate enqueue, continuation past the 40-photo limit, final scan status and duplicate counts. The separate deletion failure in that historical log is superseded by the final deletion run.
- `build/api35-permission-{full,denied,partial}.log`: **one passing UI test per permission state**, with real package-manager grants/revocations.
- Actual system picker on API 35: three synthetic PNGs imported into the isolated emulator. Selecting one yields **1 photo**, adding one through Manage photo access yields **2 photos**, revoking selected-media access restores the denied-access home. Evidence: `build/api35-picker-{select,one,reselect-grid,two,revoked}.xml`. No real photos involved.
- API 35 airplane mode enabled during runtime verification; bundled OCR and scanning worked offline. Its isolated emulator is now stopped. Latest API 26/36 verification remains in progress.
- `build/api26-final.log`: **10 tests passed** on latest schema/permission/delete implementation: migration, three snapshot cases, four offline pipeline cases, actual delete flow and Chinese large-font layout. API 26 was stopped afterward.
- Final worker audit removed writes that could restore `isScanActive` after a user pause; continuation now also requires the active preference and gallery access. Worker regression now pauses after scanning starts, verifies no automatic restart, explicitly resumes, and completes 41 photos. Latest full build/unit/lint passed after this source change; API 36 runtime check in progress.
- Final result: `build/api36-final.log` reports **OK (11 tests)**, including the new pause/resume regression and 41-photo continuation. Bounded scope is complete; unexecuted device/version checks are explicitly retained in the final report, not claimed passed. No publishing or remote operations performed.
