# Origin War

The Windows-first SquidSquad/libGDX remake of the original 2016 Origin War. Development follows the preserved original design and Revision 2 directive.

Playable ten-deck station campaign: turn-based RPG combat, oxygen hallucinations, finite equipment/medicines, A.L.I.C.E.'s original story and save/resume. Six original soundtrack roles,28 effects and directional/sector artwork are integrated. Both Constructor routes reach the original ending in the bundled Windows executable; complete file-backed campaign replays, corrupt-save recovery and fresh-process final-dialogue resume are verified. Final map/record and current hypoxia presentation checks are in progress. Audible artistic/mix review remains open; see the exact acceptance scopes in [verification](docs/VERIFICATION.md). Read the [manual](MANUAL.md), [progress](docs/PROGRESS.md) and [task ledger](docs/IMPLEMENTATION_PLAN.md).

Build with JDK21: `gradlew.bat test`. Launch: `gradlew.bat :lwjgl3:run`. For the Windows app image with runtime, source, licenses and dependency materials, follow [distribution instructions](tools/distribution/README.md).

The original code/assets/build are preserved under `legacy/` and excluded from modern builds. See [legacy recovery](docs/LEGACY_ARCHIVE.md). Original media rights are unverified; it is not included in the modern runtime.
