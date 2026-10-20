# Origin War

The Windows-first SquidSquad/libGDX remake of the original 2016 Origin War. Development follows the preserved original design and Revision 2 directive.

Current development checkpoint: illustrated ten-deck station exploration, turn-based RPG combat, oxygen hallucinations, finite equipment/medicines, A.L.I.C.E.'s original campaign and save/resume. Six composed soundtrack roles,28 effects and directional/sector artwork are integrated. Complete ending runs, final audiovisual review and release packaging are in validation; this is not yet a release-candidate acceptance claim. Read the [manual](MANUAL.md), [progress](docs/PROGRESS.md), [task ledger](docs/IMPLEMENTATION_PLAN.md) and [verification](docs/VERIFICATION.md).

Build with JDK21: `gradlew.bat test`. Launch: `gradlew.bat :lwjgl3:run`. Package on Windows: `gradlew.bat :lwjgl3:packageWindows -PwindowsImageDir=<fresh-output-path>`.

The original code/assets/build are preserved under `legacy/` and excluded from modern builds. See [legacy recovery](docs/LEGACY_ARCHIVE.md). Original media rights are unverified; it is not included in the modern runtime.
