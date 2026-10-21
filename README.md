# Origin War

The Windows-first SquidSquad/libGDX remake of the original 2016 Origin War. Development follows the preserved original design and Revision 2 directive.

Playable ten-deck station campaign: turn-based RPG combat, oxygen hallucinations, finite equipment/medicines, A.L.I.C.E.'s original story and save/resume. Six original organ-score roles,34 muffled effects, continuous suit breathing/heartbeat, nine A.L.I.C.E. radio speech clips and directional/sector artwork are integrated. Both Constructor routes reach the original ending in the bundled Windows executable; complete file-backed campaign replays, corrupt-save recovery and fresh-process final-dialogue resume are verified. Current AI-enabled hypoxia/recovery and enlarged map/record/inventory/stats frame checks pass. Audible artistic/mix and human difficulty review remain open; see [release candidate](docs/RELEASE_CANDIDATE.md) and exact scopes in [verification](docs/VERIFICATION.md). Read the [manual](MANUAL.md), [progress](docs/PROGRESS.md) and [task ledger](docs/IMPLEMENTATION_PLAN.md).

Build with JDK21: `gradlew.bat test`. Launch: `gradlew.bat :lwjgl3:run`. For the Windows app image with runtime, source, licenses and dependency materials, follow [distribution instructions](tools/distribution/README.md).

The [owner playtest update](docs/PLAYTEST_FEEDBACK.md) improves all UI text and new-run oxygen:900 suit reserve, two360-unit carried tanks and360 opening pickup. Existing saves retain their resources. The [atmosphere pass](docs/ATMOSPHERE_PASS.md) implements the owner's dark institutional station direction: completely replaced floor/wall materials, dynamic shadowed helmet lighting, smooth movement and suit UI, plus independently authored [art](docs/design/art/ATM_ART_HANDOFF.md) and [sound](docs/design/sound/ATM_SOUND_HANDOFF.md).

The original code/assets/build are preserved under `legacy/` and excluded from modern builds. See [legacy recovery](docs/LEGACY_ARCHIVE.md). Individual legacy recordings are not included in the modern runtime; the owner's helmeted character is a visual reference for the newly produced art. Per-asset provenance and voice permissions accompany the game.
