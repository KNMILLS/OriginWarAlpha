# Integrated campaign hypoxia / recovery

Worker baseline: `64fd1c46d600c708b8ef4a33b2f75a42a0e51560`; gameplay matches the current integrated campaign. Only the new desktop `HypoxiaCampaignTest` and this handoff change. No production, schema, Gradle, assets, fixture or planner edits.

## Ordinary-action proof

The test replays the existing immutable seed-1 redirect fixture from a real new game through its 1,067th action, reaching the canonical post-broadcast `sacrifice-plan` dialogue. It verifies the Constructor route, rescued crew, fleet standdown, override/refusal/broadcast flags and a genuinely acquired finite oxygen tank. The only subsequent commands are `CHOOSE pod-go`, normal `WAIT`, a free truthful `SCAN`, actual `USE` of the held tank, and 13 more waits. No state, resources, HP, flags or actors are injected, and all AI remains enabled.

The player waits 276 real turns to oxygenation 20. The actual `oxygen-d6-supply-0` tank supplies 180 units and is consumed. Its charged turn restores oxygenation only from 20 to 24; 13 later waits recover it to 76. The tank grants no HP. All committed action costs match the actual tick increment. True-status scanning remains accessible at critical hypoxia without advancing time.

| Capture | Tick | HP | Reserve | Oxygenation | Perception |
| --- | --- | --- | --- | --- | --- |
| `hypoxia-clear` | 1,060 | 96 | 260 | 100 | CLEAR |
| `hypoxia-warning` | 1,328 | 96 | 0 | 60 | WARNING |
| `hypoxia-critical` | 1,336 | 96 | 0 | 20 | CRITICAL, actual visual impressions present |
| `hypoxia-recovered` | 1,350 | 96 | 166 | 76 | CLEAR, impressions removed |

At each stage the test samples the actual campaign state 101 times with varied presentation phases/reduced-distortion settings. Complete before/after state serialization must remain identical, including every real actor, collision tile, objective, resource, story field and RNG. The authoritative engine's state must also remain unchanged. Critical impressions occupy visible walkable cells containing no living real actor. The saved perception RNG remains unchanged throughout normal depletion/refill/recovery.

This run stops after recovery, alive on deck 10 before physical pod entry. The two original full ending proofs remain separate, unchanged tests. It uses the same privileged planning provenance as their immutable fixture; ordinary commands and finite resources are real, while this is not human difficulty evidence.

## Execution and replay

Focused test: **1 PASS, zero failures/errors/skips**. Build 11 seconds, test 0.723 seconds. No failed gameplay attempt occurred and no alternate approach was needed. No graphics application was launched by this worker.

```powershell
$env:JAVA_HOME='C:\OriginWarReboot\.tools\jdk-21.0.12.1+1'
$env:GRADLE_USER_HOME='C:\OriginWarReboot\.cache\engine-gradle'
.\gradlew.bat :lwjgl3:test --tests io.zipcoder.originwar.desktop.HypoxiaCampaignTest --no-daemon
```

Ignored artifacts in `C:\OriginWarReboot\worktrees\engine\lwjgl3\build\evidence\`:

- `integrated-hypoxia-seed1.actions.tsv`: 1,359 exact ordinary new-game commands plus four presentation-only captures. Runtime format `TYPE<TAB>x<TAB>y<TAB>id`; captures use `CAPTURE<TAB>name`. SHA-256: `d0391c0bc932518410754c5a2f37ab62c7a59cb0b2acdd9ce8c5b1758f28aada`.
- `integrated-hypoxia-seed1.trace.tsv`: every command's actual cost, tick, HP/reserve/oxygenation, perception RNG and events, plus capture-state rows.
- `integrated-hypoxia-seed1.txt`: successful final summary: deck 10, HP 96, reserve 166, oxygenation 76, nine actual actors.
- JUnit XML: `lwjgl3/build/test-results/test/TEST-io.zipcoder.originwar.desktop.HypoxiaCampaignTest.xml`.

Pass these arguments to the packaged runtime with an isolated save directory:

```text
--play --seed=1 --user-data=C:\OriginWarReboot\runtime-data\integrated-hypoxia --replay=C:\OriginWarReboot\worktrees\engine\lwjgl3\build\evidence\integrated-hypoxia-seed1.actions.tsv
```

The integration owner still verifies the actual packaged screenshots, rendering/audio behavior and accessibility controls. This headless test proves the current campaign's genuine physiology/recovery and the perception/state boundary. If a future ordinary action kills the player or rejects, a timestamped first-failure command/trace/summary set remains outside Git under `build/evidence`; no production weakening is authorized.
