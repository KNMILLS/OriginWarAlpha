# Origin War

The Windows-first SquidSquad/libGDX remake of the original 2016 Origin War. Development follows the preserved original design and Revision 2 directive.

Playable ten-deck station campaign: turn-based RPG combat, oxygen hallucinations, finite equipment/medicines, A.L.I.C.E.'s original story and save/resume. Six original soundtrack roles,28 effects and directional/sector artwork are integrated. Both Constructor routes reach the original ending in the bundled Windows executable; complete file-backed campaign replays, corrupt-save recovery and fresh-process final-dialogue resume are verified. Current AI-enabled hypoxia/recovery and enlarged map/record/inventory/stats frame checks pass. Audible artistic/mix and human difficulty review remain open; see [release candidate](docs/RELEASE_CANDIDATE.md) and exact scopes in [verification](docs/VERIFICATION.md). Read the [manual](MANUAL.md), [progress](docs/PROGRESS.md) and [task ledger](docs/IMPLEMENTATION_PLAN.md).

Build with JDK21: `gradlew.bat test`. Launch: `gradlew.bat :lwjgl3:run`. For the Windows app image with runtime, source, licenses and dependency materials, follow [distribution instructions](tools/distribution/README.md).

The [owner playtest update](docs/PLAYTEST_FEEDBACK.md) improves all UI text and new-run oxygen:900 suit reserve, two360-unit carried tanks and360 opening pickup. Existing saves retain their resources. Dedicated [art](docs/design/art/FB1_VISUAL_DIRECTION.md) and [sound](docs/design/sound/SOUND_DIRECTION.md) briefs ground the next creative comparison in the original goal of unease and tension; current media is unchanged.

The original code/assets/build are preserved under `legacy/` and excluded from modern builds. See [legacy recovery](docs/LEGACY_ARCHIVE.md). Original media rights are unverified; it is not included in the modern runtime.
