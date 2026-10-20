# Origin War

The Windows-first SquidSquad/libGDX remake of the original 2016 Origin War. Development follows the preserved original design and Revision 2 directive.

Current checkpoint: modern bootstrap. The complete game is in development; this is not a finished release. Read [progress](docs/PROGRESS.md), the [task ledger](docs/IMPLEMENTATION_PLAN.md) and [verification](docs/VERIFICATION.md).

Build with JDK 21: `gradlew.bat test`. Launch: `gradlew.bat :lwjgl3:run`. Package on Windows: `gradlew.bat :lwjgl3:packageWindows`.

The original code/assets/build are preserved under `legacy/` and excluded from modern builds. See [legacy recovery](docs/LEGACY_ARCHIVE.md). Original media rights are unverified; it is not included in the modern runtime.
