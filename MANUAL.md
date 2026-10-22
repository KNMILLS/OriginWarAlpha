# Origin War field reference

2115. A Terran security officer boards a failing science station orbiting Mars. West's stored mission packet calls it a communications failure. The station's records suggest something else. ALICE is its resident intelligence and the only live voice on the channel.

Extract the complete Windows package and launch OriginWar.exe. No separate Java installation is needed. A new expedition uses a chosen seed; Resume restores the current expedition. Death ends the run. Descent is permanent and supplies above are abandoned. There is no character progression between runs.

## Commitments and controls

Only committed actions advance the station. Inspecting, aiming, browsing the pack, reading and rejected commands cost no world time. Ordinary movement costs one tick; careful movement costs two; slow terrain adds one. Turning the body, toggling the lamp, doors, collecting, dropping, equipping and using equipment cost a tick. The HUD previews known costs and observable danger. Machines act during long commitments; movement or work completes on the final tick. An obstruction leaves you at the source after spending the time.

| Control | Action |
| --- | --- |
| WASD / arrows | Commit a cardinal step |
| Shift + movement / Q | Careful step / toggle careful mode |
| Alt + arrow | Turn body and real helmet beam, one tick |
| Ctrl + arrow / Backspace | Move free inspection and targeting cursor / clear target |
| Left mouse | Request a route through known terrain; each step replans and visible threats stop travel |
| Right mouse | Select or clear a target; inspect freely |
| E / Enter | Operate the selected or adjacent physical mechanism |
| G / R | Collect underfoot / use carried oxygen |
| N / L | Listen / toggle lamp |
| P / B | Toggle local circuit / install or recover a closed-door brace |
| H / O | Use local service access / force a locked shortcut |
| F / M | Shoot selected cell / emergency melee |
| V / T | Medical analysis / query available station radio |
| Space / period | Wait one tick |
| Tab / I | Physical pack; Up/Down selects, U uses, Q equips, X drops, G collects |
| J / K / C | Journal / known map / suit telemetry |
| Backslash | All action buttons, including turning and descent |
| Escape / F1 | Pause/back / field reference |
| F11 / F12 | Fullscreen / screenshot |

Use Tab or Up/Down and Enter for menu buttons. Page Up/Down pages long text; Up/Down selects journal entries. All world operations also have pointer-accessible action buttons. Moving either cursor never sweeps the actual lamp or advances a machine fuse.

## Life support and carrying

Start with 360 suit oxygen, one 180-unit canister, a pistol, twelve loaded cells, a twelve-cell pack and an identified healing dose. The pressure suit is permanent. Reserve, body oxygenation and HP are separate. Empty reserve lowers oxygenation; recovery takes paid actions after a refill. Exact suit readings and genuine warnings remain reliable during hypoxia.

Your automatically arranged pack holds twelve bulk. Duplicates each take their real space; equipping a gun does not remove its bulk. One worn protective shell has its own mount.

| Physical item | Bulk |
| --- | ---: |
| Pistol |2|
| Rifle / scattergun |4|
|180-unit oxygen canister |3|
| Twelve-cell pack |1|
| Medicine / door brace |1|
| Service probe |2|
| Unworn light / heavy shell |4 /6|

Partial refills keep the remainder in the actual canister or cell pack. Suit power holds at most twelve cells. Dropping the last gun leaves you unarmed. An armor swap can leave the previous shell on the floor when it cannot fit. Killing a machine purchases local safety; it awards no XP or guaranteed supplies.

## Observing the station

Light, occlusion, machine facing and proximity affect optical detection. Darkness conceals; nearby machines can still notice you. Sound follows connected spaces: walls and sealed partitions block it, closed bulkheads attenuate it. Listen spends a tick and reports approximate direction and character, never a concealed actor's exact cell or identity.

Machines patrol, investigate sounds, pursue sightings, search last-known positions and return. Powered local surveillance shares observations. Mobile machines carry independent power. Door capabilities differ, and a brace can make isolation effective. A newly armed R.O.V.E.R. allows another decision before its fuse can expire. Subsequent long commitments can still be dangerous.

A local terminal or breaker affects its circuit's lights, communications and fixed security. It does not unlock a whole deck. A powered service terminal copies a static public-route chart and authenticated record into the suit. The chart supplies geometry, not current hidden objects or remotely changing door states. Later observations supersede it. Optional locked service connections can trade probe cells or noisy work for a shortcut. The public descent route requires neither optional tool. Forcing damages the release permanently and leaves the shortcut unable to reseal; a probe preserves the mechanism. Opened service panels and damaged releases survive saving.

Find the physical hatch release, then the hatch. On the fabrication deck, stop the Constructor through its physical supervisory controller or its unit-redirection interface. Deployed units remain active after shutdown. On command, restore authority, power and operate the outbound relay, then physically board and seal the evacuation pod. Reading and speech completion never unlock these mechanisms.

## ALICE and accessibility

ALICE has no physical body or follower. Ordinary operations remain yours. Her authored voiced exchanges use actual telemetry, records and remembered actions. Optional starts are spaced by real playing time and paid actions, suppress during known danger and expire without a queue. Started and completed speech history survives saving; unfinished essential contact/ending statements can resume. The journal and suit log preserve acquired information.

Settings provide independent sound controls, clear speech, interference reduction, sound captions, reduced motion, reduced dynamic range, conversation frequency and separate text scaling. Pause and focus loss freeze action presentation and audio coherently. Suit breathing, station mechanisms and silence form the baseline; music is intermittent and cannot announce hidden threats.

## Saving

The survival profile is `%LOCALAPPDATA%/OriginWar/survival-0.2`. Each expedition has its own snapshot, last-good backup and death marker. A process holds the profile exclusively. New Game activates a new run only after its first snapshots are durable; the prior files remain preserved. There is no legacy migration.

Saving happens in the background. World input waits for the committed snapshot; inspection remains available. A failure is shown explicitly. Retry writes that same outcome and cannot reroll or repeat the action. Close waits for a pending save. If a write fails, restore writable storage and retry. A corrupt primary can recover only its own expedition's backup; a death marker prevents a living backup from reviving the run.

Current verification and unresolved release gates are in [survival qualification](docs/SURVIVAL_QUALIFICATION.md). Build from source using JDK 21 and `gradlew.bat test verifyAssets`. Windows packaging requires a clean committed checkout, pinned runtime and verified dependency-source materials; see tools/distribution/README.md.
