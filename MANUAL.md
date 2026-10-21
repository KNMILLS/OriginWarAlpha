# Origin War

2115. You are a young Terran security officer on your first assignment, sent to secure a science station orbiting Mars. Captain Marcus West's briefing is incomplete. Find the records, meet A.L.I.C.E., and decide what to do with your orders.

Start OriginWar.exe in the Windows package. A bundled runtime is included; installing Java is unnecessary. Extract the entire package before launching. New Expedition starts a seeded run; enter a number to replay its station layout or choose Randomize. Resume loads the current expedition. Death ends that expedition. There is no permanent character progression between runs.

Time advances when a world action succeeds. Opening a menu, inspecting, aiming, reading dialogue, or issuing an invalid command does not advance the station. Ordinary actions cost one tick; low gravity and hazards take two, no gravity three. Real resource transfers and longer door forcing take their stated ticks. Save data and settings live in your Windows LocalAppData/OriginWar directory. Keep the game directory together; replacement dependency JARs remain accessible under app.

| Control | Action |
| --- | --- |
| WASD / arrows | Move; bump closed doors to open; bump hostile robots to melee |
| Left mouse | Walk through explored terrain; threats interrupt the route |
| Right mouse | Inspect a visible cell and select it as a target |
| E / Enter | Interact with a nearby switch, console, person or hatch |
| G | Pick up the item underfoot |
| R | Use a finite oxygen tank |
| Space / period | Wait one tick |
| F | Fire at selected cell; a legal empty shot still spends ammunition/time |
| M | Melee an adjacent hostile |
| H | Hack selected or adjacent equipment |
| B | Force a locked door using STR |
| V | Check reliable suit readings |
| Tab / I | Inventory; U uses, Q equips, X drops the selected item |
| J / C | Journal / character and earned attribute points |
| 1 / 2 / 3 | Choose a displayed dialogue option |
| Esc / F1 | Back or pause / paged manual |
| Z / F11 / F12 | World zoom / fullscreen / screenshot |

HP, oxygen reserve and body oxygenation are separate. A refill restores reserve before that action's breathing cost, but your body needs real turns to recover. With reserve empty, oxygenation falls; critical hypoxia distorts perception and eventually harms HP. Phantom impressions cannot collide, attack, change quests, or become loot. The suit HUD and legitimate sensor inspection remain reliable. Settings includes reduced distortion, enlarged text, independent master/music/effect volume and mute. Menus provide thinking time, not free recovery.

STR increases melee damage and reduces the effort of forcing doors. INT enables security hacks and persuasion; A.L.I.C.E. does not bypass these checks for you. DEX affects accuracy and evasion. Light armor improves effective DEX; heavy armor trades DEX for protection. Guns use finite battery cells. Compare equipment in the inventory. Medicines receive shuffled identities per expedition: using a dose or analyzing it at a medical terminal teaches its effect for that run. Unknown medicine is a risk.

Watch robot behavior. S.I.R.S. pursue and search after losing contact. A.D.D.s favor ranged firing positions. Cameras alert nearby security. R.O.V.E.R.s close in and telegraph their charges; move out of the one-tile blast radius before detonation. Cover interferes with shots, and glass blocks projectiles even when you can see through it. Hacking and forcing offer alternatives to locked shortcuts; public routes remain accessible.

Find each security override, then its hatch. The journal also identifies story objectives at physical station terminals and refuge encounters. The Constructor's industrial deck has two genuine solutions: redirect explosive R.O.V.E.R.s with sufficient INT, or reach and kill its human operator. Defeating a security switch alone cannot complete that encounter. The command deck requires Carmack's confrontation and A.L.I.C.E.'s decision before the evacuation pod can conclude the campaign.

The primary save is written between decisions, with a last-good backup and a run-specific death record. Resume may report recovery from the backup if the primary is corrupt. Copying older saves is outside the intended permadeath rules. An interrupted write cannot guarantee preservation of the very last action; report any save error shown by the game.

Build from source with JDK21: `gradlew.bat test :lwjgl3:installDist`. Launch development with `gradlew.bat :lwjgl3:run`. Before packaging, run `python tools/distribution/fetch_sources.py` with Python3 to fetch and verify the accompanying upstream sources. On Windows, `gradlew.bat :lwjgl3:packageWindows -PwindowsImageDir=<new-output-path>` creates an app image with its runtime, licenses and dependency sources. Existing package outputs are preserved. The source, original-design reference, provenance, dependency notices and verification records accompany the distribution. Current acceptance and any unresolved checks are stated in docs/VERIFICATION.md; development checkpoints are not release acceptance.
