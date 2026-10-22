# Campaign filesystem-save verification

Baseline: worker `81f9a1f65c06edb5a2654dd846847b707a9c298d`, equivalent integrated campaign tests `bf6bd0d`. Scope is the new desktop `CampaignSaveTest`, two compact immutable action resources and this handoff. No production, schema, Gradle, rendering, audio or planner changes.

## Reproduction

```powershell
$env:JAVA_HOME='C:\OriginWarReboot\.tools\jdk-21.0.12.1+1'
$env:GRADLE_USER_HOME='C:\OriginWarReboot\.cache\engine-gradle'
.\gradlew.bat :lwjgl3:test --tests io.zipcoder.originwar.desktop.CampaignSaveTest --no-daemon
```

These are headless JVM filesystem tests; no desktop application, graphics/audio context or user save is opened. JUnit creates a separate temporary save directory per test.

## Immutable fixtures

The two checked-in resources are the exact successful M6 campaign command exports with only comments and presentation `CAPTURE` lines stripped. The tests parse only ordinary `TYPE<TAB>x<TAB>y<TAB>id` lines and check the complete normalized SHA-256 before replay. Line endings normalize to LF for the hash so a Windows checkout does not change its meaning.

| Resource under `lwjgl3/src/test/resources/campaign/` | Seed / route | Commands | LF bytes | Normalized SHA-256 |
| --- | --- | --- | --- | --- |
| `operator-seed0.tsv` | 0 / physical operator | 1,936 | 21,460 | `9362102c1330c001f77fbf80b0710217d37e05bd7c46c9353a4dda26e63d4422` |
| `redirect-seed1.tsv` | 1 / real redirected explosions | 1,131 | 12,796 | `0094e429f2fa5a1a2830251dd96d8b8186038d613d7142c2270329af15cc43c7` |

No second pilot or generator is introduced. Both engines start from `new RunEngine(seed)` at original STR/INT/DEX 4. These same finite-resource, fully enabled-AI action streams previously proved the real Constructor routes and canonical ending. Their privileged bot planning provenance remains explicit; they are not normal human play evidence.

## Assertions

- Each seeded campaign has an uninterrupted reference engine and an engine that actually continues from file-loaded states. Every action must be accepted, and its complete `TurnResult` must equal the reference, including integer cost and emitted events.
- All public save DTO fields match after every turn and immediately before/after each file roundtrip. This includes every RNG stream, tiles/FOV/exploration/light, resources, inventory, medicine knowledge, ordered actor/item state, live fuses, story flags/dialogue/trust, journal and log. Reflection enumerates fields rather than maintaining an incomplete hand-picked subset.
- At deck 5, a live Constructor countdown and the post-broadcast `sacrifice-plan` dialogue, production `SaveStore.save` writes actual JSON files. A fresh `SaveStore` loads them; a new `RunEngine(loaded)` becomes the ongoing engine. Neither the in-memory reference nor its timeline is reset.
- The operator campaign's deck-5 checkpoint additionally writes a genuinely later primary by executing the next immutable action on a separate real engine. The test corrupts only that primary, then requires `usedBackup()` and exact restoration of the earlier real checkpoint. Both continuing engines subsequently execute the remaining action stream identically through the ending.
- Both routes must finish alive with the real route, mandatory Constructor/broadcast/refusal/sacrifice/pod/station-destruction flags, correct crew choice and appropriate INT fleet-standdown consequence. The completed ending also roundtrips through a fresh file store; another world action remains rejected after victory.
- A separate original new game makes actual opening choices and waits under normal simulation until death. Saving that real death prevents both primary loading and resurrection from a living same-run backup after primary corruption. Starting a new seed-1 expedition initializes both candidates for that new run, whose clean opening state remains loadable from backup after immediate primary corruption.

## Results and evidence

Focused execution completed after the integration owner released the headless build slot: **3 tests PASS, zero failures/errors/skips**, 23-second build, 1.8-second test suite. No runtime/build was launched during the owner's active renderer restriction. No failed gameplay/save trial or production defect occurred in this task.

| Route | File checkpoint action numbers | Backup used | Final result |
| --- | --- | --- | --- |
| Operator, seed 0 | 716 / 1,457 / 1,876 | Deck 5 only, as deliberately corrupted | Victory alive; 1,936 actions, 1,937 ticks, HP 96, reserve 183, batteries 20 |
| Redirect, seed 1 | 668 / 886 / 1,067 | None | Victory alive; 1,131 actions, 1,125 ticks, HP 96, reserve 195, batteries 15 |

The real seed-0 WAIT death happened after 132 waits under enabled AI. The test deliberately makes no claim that suffocation caused that death. The saved HP-zero death blocked its living same-run backup; a new seed-1 briefing state then loaded intact from its own backup after immediate primary corruption.

Ignored generated summaries live under `lwjgl3/build/evidence/` as `campaign-files-operator.tsv`, `campaign-files-redirect.tsv` and `campaign-files-death.tsv`; the JUnit XML is under `lwjgl3/build/test-results/test/`. The campaign summaries record exact checkpoint actions/ticks/resources/RNG streams, live rover fuses, backup selection and final result. Any failed campaign attempt writes a timestamped `FAIL-*` trace outside Git.

These tests implement the directive's save/resume, deterministic seeded combat, corrupt-save and death-stakes checks through the actual production filesystem boundary. Existing generic SaveStore fault tests remain unchanged. Filesystem tests do not establish resume-menu input behavior, audio/rendering quality, packaged runtime behavior or physical power-loss durability; those remain separate integration checks.
