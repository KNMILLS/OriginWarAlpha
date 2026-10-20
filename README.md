# Origin War

The Windows-first SquidSquad/libGDX remake of the original 2016 Origin War. Development follows the preserved original design and Revision 2 directive.

Current development checkpoint: illustrated seeded exploration/survival, oxygen, switch/hatch progression, inventory, save/resume and original music/SFX. Combat, hallucinations and the complete campaign are still in development. Read [progress](docs/PROGRESS.md), the [task ledger](docs/IMPLEMENTATION_PLAN.md) and [verification](docs/VERIFICATION.md).

Build with JDK 21: `gradlew.bat test`. Launch: `gradlew.bat :lwjgl3:run`. Package on Windows: `gradlew.bat :lwjgl3:packageWindows`.

The original code/assets/build are preserved under `legacy/` and excluded from modern builds. See [legacy recovery](docs/LEGACY_ARCHIVE.md). Original media rights are unverified; it is not included in the modern runtime.
