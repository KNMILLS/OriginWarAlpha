# Observed-information survival campaign pilot

This pilot tests ordinary finite-resource play through the actual survival engine, with every generated machine and its AI retained. It gives a bounded policy an explicit observation record, rather than the full simulation. It does not establish human difficulty, a 90–120 minute human run time, graphical or sound quality, native input usability, owner acceptance, or desktop filesystem-save durability.

## Information boundary

`SurvivalObservedPilotTest.observe` is the sole adapter from a production snapshot to policy input. The policy receives suit telemetry and carried physical gear, explored terrain through `StationSystems.knownTile`, currently visible machines/items/controls, identified medicine effects, acquired knowledge, and legitimate event messages. Visible machine HP, arming/alignment and power/redirection status represent observable inspection/state cues. It retains previously observed supply and controller positions; it receives no subsequently updated hidden actor positions. An actual downloaded `supervisor-chart:X:Y` knowledge fact identifies fixed infrastructure without reporting live actor status.

The policy never receives `GameState`, `RunEngine`, raw objective coordinates, route budgets, fixed supply encodings, unacquired blueprints, hidden actors, machine awareness/last-known targets, future schedules, RNG state or unobserved item locations. It does not query action forecasts or probe rejected actions for hidden information. It derives release/hatch/terminal destinations from observed or genuinely downloaded service-plan terrain. It operates the actual terminal to acquire that drawing; reading an archive or completing speech grants no progression permission.

The noninterference regression changes hidden geometry and hidden actor state, raw objective coordinates, route budgets and supply encodings, and checks that policy input and its selected action remain identical. Full snapshot hashes appear only in the audit/replay driver, outside the policy.

## Policy and acceptance

The policy conserves oxygen and ammunition, reloads only real held packs, uses identified healing, wears observed armor, evades visible armed units, and engages immediate observed threats with finite pistol ammunition. It usually extinguishes its lamp; unresolved supervisory hardware can trigger a deliberate illuminated search. It remembers supply destinations while navigating around obstacles. These are policy choices, not claims of optimal stealth or representative human behavior.

The physical approach operates the actual supervisory controller. The redirection approach observes power at the visible supervisory fixture, performs its real manual circuit operation when necessary, spends real cells and ticks on controller commands, then waits actual ticks for units to reach the Constructor. No machine is removed, no HP/resource/flag/RNG is granted or edited, and no failed action is rerolled. A rejected action terminates that attempt and is reported.

Every attempt exports ordinary commands and a trace. Actual core-copy suspend/resume occurs at midcampaign, a visible live-fuse opportunity when encountered, and command refusal. The driver compares every public saved field before and after restoration. An independent fresh engine then replays the exact accepted command list without checkpoints and must match every action cost, event list and complete final state, whether the attempt won or failed. This is core state restoration; the desktop owner separately verifies file-backed suspend/resume.

The focused gate requires at least one genuine complete victory through **each** requested Constructor route. Additional failed seeds remain in `outcomes.tsv`; passing this bounded gate does not mean every sampled seed wins. Attempts stop at death, first rejection, policy dead end or 5,000 actions. Timestamped evidence folders retain earlier failures.

## Running and artifacts

Use JDK21, an isolated Gradle user home, and the focused test selector:

```powershell
$env:JAVA_HOME='C:\OriginWarReboot\.tools\jdk-21.0.12.1+1'
$env:GRADLE_USER_HOME='C:\OriginWarReboot\.cache\survival-pilot-gradle'
$env:ORIGINWAR_GENERATION_SEEDS='10'
$env:ORIGINWAR_PILOT_SEEDS='0,1,6,14'
.\gradlew.bat :core:test --tests io.zipcoder.originwar.engine.SurvivalObservedPilotTest --rerun-tasks --no-daemon --console=plain
```

`ORIGINWAR_PILOT_SEEDS` overrides the four default calibration seeds. Both approaches run for each seed. Other tests are not selected by this command. Use `--rerun-tasks` when changing the seed environment: Gradle does not otherwise consider that environment variable a test-task input, and an up-to-date result does not execute another probe or create a fresh evidence folder.

Ignored output: `core/build/evidence/survival-observed/<timestamp-id>/`.

- `outcomes.tsv`: every attempt, including deaths, rejections and policy limits.
- Per attempt `actions.tsv`: exactly `TYPE<TAB>x<TAB>y<TAB>id`, with a comment header; no artificial waits or speech receipts are appended.
- `trace.tsv`: actual accepted action, purpose, legitimate observations hash, full audit-state hash and events.
- Checkpoint text files: action/tick/hash of actual restored states.
- `deck10-prefix.tsv` and `deck10-suffix.tsv`: first portion ends immediately after genuine descent to deck10; second continues those exact commands from that saved state.
- `deck10-split.txt`: exact prefix/suffix counts, expected telemetry and final hashes.
- `summary.txt`: outcome and evidence boundaries.

For native playback, use the matching seed and a fresh isolated profile, `--play`, `--replay=<actions.tsv>` and `--diagnostics=<report>`. For file-backed resume, play the prefix, close after it drains, then use `--resume` with the same profile and the suffix. Root owns runtime synchronization, optional-conversation settings, captures, filesystem checks and packaging. Source/version identity must match before comparing full-state hashes.

## Preserved first results

The first policy produced four actual victories on seeds0/1 through both approaches. Four additional victories followed on seeds6/14. Extra probes exposed two policy defects: seed37 oscillated around a Manhattan-distance supply threshold and suffocated on deck2; seed420 reached the deck8 hatch without locating dark supervisory hardware and attempted blocked descent. Their complete failed action streams and matching replays are preserved. Subsequent policy changes retain a chosen observed supply target and explicitly search for unlocated supervisory hardware. Seed37 then completed both approaches (2,393/2,394 ticks; HP95/87; reserve307/306). An intermediate seed420 attempt exhausted oxygen while exploring for the controller on deck8.

The integration owner subsequently put fixed supervisory infrastructure on its actual acquired service drawing. With that legitimate chart fact, both seed420 approaches stop the Constructor correctly. This bounded lamp-off policy still suffocates after collecting no canisters on deck9: the physical approach reaches deck10 and dies at tick2,386; the redirected approach dies on deck9 at tick2,388, including39 actual waiting actions. This is a retained resource/discovery-policy limitation, not proof that the seed is impossible.

Before the later power/search corrections below, the refreshed-dependency focused run passed both tests, with12 reported attempts: ten complete victories on seeds0/1/6/14/37, both approaches each; two reported seed420 deaths. All12 streams replay exactly. The six-seed result is bounded automated evidence, not universal seed acceptance. No survival rule, resource or threat was weakened by this worker.

The initial compile also caught a test-side accessor typo (`rejectionReason` versus `reason`); it was corrected before campaign execution. No production file was changed by this worker.

## Later power and machine-search corrections

Production subsequently made electronic controller access respect power, bounded unreachable machine searches and allowed pursuit around projectile-blocking glass. The policy now includes only currently visible supervisory-fixture power in its control observations and manually restores that circuit before electronic access when necessary. Earlier action streams remain preserved as evidence for their exact earlier rules snapshot; refreshed streams are required for native acceptance of the corrected rules.

The first corrected-rules probe exposed an unintended power check on the manual physical shutdown too: seed0's accepted adjacent INTERACT commands produced no effect while unpowered, eventually exhausting oxygen. This was reported to the integration owner with the first failing trace, rather than worked around by silently changing the physical-shutdown test policy. The owner restored manual shutdown and added a production regression. A subsequent fresh0/1 run completed all four campaigns under the corrected power, pursuit and ordered-event rules. Latest seed0 current-source outcomes and native split files are in `NATIVE_HANDOFF.md`; earlier streams remain historical evidence for their original source.

The final current-rules verification consists of two focused executions, each passing both tests: seeds0/1, followed by seeds6/14/37/420. Ten of the12 actual attempts win through their requested route; both seed420 attempts still die after completing the requested Constructor solution, at the same deck/tick reported above. Seed14's corrected-power redirection route takes2,590 actions/2,636 ticks and ends at HP73/reserve64; its earlier easier result is historical. Every attempt, including each death, matches its fresh uninterrupted replay. Current evidence roots are `2026-10-21T18-43-19.155311100Z-182e715a` and `2026-10-21T18-45-23.279508100Z-50fd17b5` under the ignored output directory. Native/file-backed and human acceptance remain separate.
