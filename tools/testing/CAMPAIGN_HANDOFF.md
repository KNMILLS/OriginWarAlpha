# Full campaign action-replay handoff

Baseline: `c64f1767d6702e219a7bc57fe27744b1d8869121`. Worker branch: `codex/m6-campaign`.
Only `CombatPlaythroughTest.java` and this handoff change. Production source, generated assets and saved games are unchanged.

## Evidence scope

Two seeded production engines start at the actual briefing with STR/INT/DEX 4, finite inventory and enabled AI. The existing pilot uses privileged knowledge of full geometry and actors to plan, then submits ordinary production actions. This proves bounded automated campaign feasibility and deterministic replay. It does not establish normal human difficulty, native input usability, presentation quality or desktop SaveStore durability.

The tests now continue through deck 10's terminal encounter, Carmack's override, A.L.I.C.E.'s refusal, public broadcast, sacrifice decision, physical movement to the evacuation hatch and its charged escape action. Victory requires HP above zero, all mandatory narrative flags and both route-specific completion assertions. No flag, inventory, actor, stat, HP or resource injection occurs; no AI is disabled or enemy removed. Earned attribute points, pickups, identification, equipment, healing, battery reloads and oxygen use remain ordinary actions. Healing is policy driven when needed, without an arbitrary consumption requirement.

## Verified seeded outcomes

| Seed / Constructor route | Actions / ticks | Final HP / reserve / oxygenation / battery | Earned level; STR / INT / DEX | Route evidence | Story consequence |
| --- | --- | --- | --- | --- | --- |
| 0 / physical operator | 1,936 / 1,937 | 96 / 183 / 100 / 20 | 5; 6 / 4 / 10 | Two actual adjacent operator melee hits; zero redirected Constructor hits | Real 40-reserve crew transfer plus one world tick; records sent without INT standdown |
| 1 / redirected R.O.V.E.R.s | 1,131 / 1,125 | 96 / 195 / 100 / 15 | 4; 6 / 6 / 6 | Three actual redirects, two real 26-damage explosion hits on Constructor, no operator melee | Earned INT crew guidance plus one world tick; INT fleet standdown |

Both endings acknowledge the rescued technicians. The first epilogue retains the fleet's receipt of the records; the second explicitly holds the assault fleet back. Both retain A.L.I.C.E.'s autonomous final choice and destination Mars.

Each campaign is genuinely resumed using `new RunEngine(savedState)` at all three checkpoints below. Every public save field is compared immediately, including seed/RNG streams, map/FOV/knowledge, all resources, inventory, medicine identities, actor fuse/alert/facing, dialogue, flags, journal and log. The ongoing pilot uses the restored engine. A separate uninterrupted new engine then consumes the exact exported action file and matches every accepted result, integer action cost, emitted event and complete final state.

| Checkpoint | Seed 0 action / tick / HP / reserve | Seed 1 action / tick / HP / reserve |
| --- | --- | --- |
| Deck 5 midcampaign | 716 / 713 / 90 / 207 | 668 / 666 / 78 / 114 |
| Live deck 8 Constructor countdown | 1,457 / 1,458 / 76 / 182; live rover fuse 18 | 886 / 882 / 96 / 258; live rover fuse 18 |
| Final dialogue after broadcast | 1,876 / 1,880 / 70 / 120; `sacrifice-plan` | 1,067 / 1,060 / 96 / 260; `sacrifice-plan` |

## Reproduction and renderer handoff

From the worktree, with JDK 21 and an isolated Gradle user home:

```powershell
$env:JAVA_HOME='C:\OriginWarReboot\.tools\jdk-21.0.12.1+1'
$env:GRADLE_USER_HOME='C:\OriginWarReboot\.cache\engine-gradle'
.\gradlew.bat :core:test --tests io.zipcoder.originwar.engine.CombatPlaythroughTest --no-daemon
```

Verification completed: focused campaign tests 2/2 PASS, then `:core:test` 72/72 PASS with zero failures/errors. Full core verification includes the unchanged 1,000-seed / 10,000-deck structural/resource sweep. That sweep is structural evidence, distinct from the two finite combat campaign proofs.

The worker's ignored outputs are under `C:\OriginWarReboot\worktrees\engine\core\build\evidence\`:

- `campaign-seed0-operator.actions.tsv` and `campaign-seed1-redirect.actions.tsv`: exact runtime format `TYPE<TAB>x<TAB>y<TAB>id`, comments starting `#`, plus presentation-only `CAPTURE<TAB>name` lines. No detailed-trace header is mixed into the runtime command stream.
- Corresponding `.trace.tsv`: all actual actions, costs, position/resources, dialogue/boss phase and emitted events.
- Corresponding `.resumes.tsv`: the three checkpoints and their saved RNG streams/countdowns.
- Corresponding `.txt`: outcome, counts, final flags, epilogue, actual remaining actors and inventory.
- `core/build/test-results/test/TEST-io.zipcoder.originwar.engine.CombatPlaythroughTest.xml`: test results and fresh exported replay confirmations.

For desktop replay, pass the matching seed and an isolated user-data directory, for example:

```text
--play --seed=0 --user-data=C:\OriginWarReboot\runtime-data\campaign-operator --replay=C:\OriginWarReboot\worktrees\engine\core\build\evidence\campaign-seed0-operator.actions.tsv
--play --seed=1 --user-data=C:\OriginWarReboot\runtime-data\campaign-redirect --replay=C:\OriginWarReboot\worktrees\engine\core\build\evidence\campaign-seed1-redirect.actions.tsv
```

Each stream has 16 named capture points: opening, after briefing, five sector entries, first combat, A.L.I.C.E. encounter, active Constructor before/after completion, Carmack, refusal, broadcast, pod-go and final epilogue. `constructor-before` is placed when the machine has actually activated, rather than at the deck entrance. Captures themselves submit no gameplay command and change no state/time/RNG. The root's current renderer saves these in the chosen user-data `screenshots` directory.

## Source-derived coverage and remaining boundaries

- `Campaign.begin`, `enterDeck`, `interact`, `dialogue` and `choose`: actual opening/records/NPC/crew/fleet/finale choices, including availability gates and material crew resource cost.
- `Campaign.hatchBlock`, `RunEngine.submit` and `Campaign.escape`: mandatory campaign progression, charged physical escape, living player, correct terminal narrative flags and resulting station destruction/pod/A.L.I.C.E. sacrifice flags.
- `CombatRules.melee`, `explode` and `finishConstructor`: adjacent physical operator defeat versus real redirected blast damage, preserving both real completion routes.
- `RunEngine(GameState)`: complete state retention at ordinary, live countdown and final story checkpoints; subsequent equality against uninterrupted deterministic replay.

No M6 gameplay failures were encountered in these two runs. If a future action/assertion fails, the pilot keeps a timestamped `FAIL-*` command/trace/checkpoint/summary set under ignored `build/evidence`, rather than overwriting the first failing trace. Existing earlier M2 failure evidence remains outside Git.

The integration owner separately verifies real Windows input, renderer captures/animation/audio, actual desktop SaveStore filesystem faults and resume UI, fresh bundled-runtime distribution, licenses and performance. These tests make no claim to have completed those checks. The command exports are suitable inputs for that owner's independent runtime pass.
