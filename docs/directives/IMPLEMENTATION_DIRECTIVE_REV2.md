# CODEX IMPLEMENTATION DIRECTIVE — ORIGIN WAR

**Revision 2 · 20 October 2026 — preserved legacy baseline, Git-tracked remake, selective subagents**

**Project:** Origin War — the fully realized version of the original 2016 science-fiction roguelike

**Target:** Complete, playable, polished, distributable Windows-first desktop game

**Foundation:** Java + current, verified SquidSquad modules + libGDX/LWJGL3

**Creative authority:** The original 2016 game design and code, supplemented by the owner's decisions in this directive. **One deliberate later addition:** oxygen-deprivation hallucinations.

**This is an implementation assignment, not a request for a game pitch, design-document-only output, proof of concept, ASCII-only demo, or engine experiment.** Build the game, produce its artwork and music/sound effects, test it, and package a playable release. Work through complete vertical slices and continue toward the full game rather than stopping at the first slice.

---

## 0. Acquire the current project, archive it, then start the remake

**Repository of record: `https://github.com/KNMILLS/OriginWarAlpha`. Keep using this repository.** This revision explicitly authorizes local implementation, tests, commits, normal non-force pushes of this project's reviewed changes and preservation refs to that repository, and an implementation pull request if useful. It supersedes the earlier build prompt's requirement to ask again before ordinary remote publication. It does **not** authorize publishing secrets, changing repository visibility/settings, rewriting history, buying services, or deploying a hosted game. Follow the execution environment's actual approval requirements; do not bypass them.

The required order is **protect existing local work → acquire the latest remote state → verify a complete pre-remake archive → establish the modern development branch → build, test, commit and push progressively**. “Archive” means preserve recoverable project history and files. Do **not** activate GitHub's read-only repository-archive setting, replace the remote, create a new repository, or delete the old project to start fresh.

### 0.1 Inspect first; protect the local state before pulling

Locate the real checkout and applicable `AGENTS.md` files. Read status, current branch/HEAD, remotes, upstreams, worktrees, tags, stashes, submodule configuration and any existing modern-version work **before editing**. Inspect remote URLs without exposing embedded credentials. Confirm the owner/repository, not merely the remote nickname `origin`. Discover the actual default branch; do not assume `main`, hard-code an old commit, or treat a local checkout as current without fetching.

For an existing checkout, first preserve its current state outside the source tree in an owner-accessible, persistent backup location. Include tracked files, staged and unstaged edits, untracked and ignored project files, hidden files, authorable assets, project-local configuration/saves, and the actual Git metadata. A linked worktree's `.git` file is only a pointer: account for its common Git directory and other project worktrees. Include locally reachable commits/refs and preserve stashes/reflogs/index state in the private snapshot. Do not silently omit large files because they are inconvenient. Record inaccessible files or storage limitations and do not claim a complete archive while any are missing.

Quiesce project writes before a filesystem snapshot. Coordinate your own workers; never kill another user's process or an unrelated agent. Do not copy an actively changing `.git` directory and call it a verified backup. Preserve symlinks as links and record their relevant dependencies rather than recursively copying unrelated directories. Keep private configuration, credentials and local saves **local/private**; none belong in a public repository or public evidence log. A plain commit or source ZIP is not a complete backup of local state. [W1]

**No destructive preparation:** no `reset --hard`, `clean -fdx`, forced checkout, stash-and-forget, indiscriminate recursive deletion, or overwrite of uncommitted work. If the checkout is dirty, shared, or diverged, preserve it and use a separate clean worktree/clone for remote acquisition; do not force it to match GitHub. Preserve local-only commits and reconcile relevant newer work before selecting the remake baseline.

### 0.2 Pull in the current remote project, including its history

If there is no checkout, clone into a new directory. Otherwise fetch the verified Origin War remote, all its ordinary branch refs and tags, with sufficient history; avoid pruning historical refs during preservation. Check for shallow/partial/single-branch clones and retrieve missing history/objects needed for the archive. Inspect any existing remake branch before creating another one. Pull with an explicit **fast-forward-only** policy into the appropriate clean tracking branch when safe; a failed fast-forward is a divergence to investigate, not permission to reset or force. Never pull the default branch blindly into a worker's feature branch. [W2]

Record both the fetched remote heads and any local-only work. “Current state” includes approved local progress as well as the latest remote baseline. Do not silently revert a newer SquidSquad implementation to 2016 just because this directive mentions the original alpha. If this migration already started, verify its archive/checkpoints, fetch, reconcile and resume; do not perform a second clean-slate migration.

Inspect at minimum, where these files still exist:
- `core/src/io/zipcoder/OriginWarAlpha.java` — generation, grid movement, fog, oxygen, switches, hatch, progression.
- `core/src/io/zipcoder/Entities/{Creature,Player,Enemy,Robot}.java` and `Items/{Item,Oxygen}.java`.
- `core/src/io/zipcoder/graphics/{OriginLayers,OriginResources,TextDisplay}.java` and `Util/{OriginInput,SoundSingleton}.java`.
- `desktop/`, tests, `core/assets/`, Gradle files, executable entry points, and the actual current equivalents.
- Historical `master`, `ken`, and `evan` branches **if still present**, plus any newer branches relevant to the remake. Keep incompatible historical versions distinct; don't merge them indiscriminately.
- `Origin War.docx`, the supplied original 2016 design by Ken Ragonese and Evan Hitchings. Keep it unchanged as reference material.

### 0.3 Create and verify the pre-remake archive

Before moving or replacing legacy source/build files:

1. Create a **self-contained Git history bundle** from the fully acquired repository, covering the preserved refs; use `git bundle create <outside-repo-path> --all` where appropriate, then `git bundle verify`. A bundle is an additional history backup, not a replacement for the private worktree/configuration snapshot. Also export an immutable clean source/asset snapshot of the selected fetched baseline. [W1]
2. Check for **Git LFS and submodules**. Preserve actual required LFS objects, not only pointer files, and archive submodule repositories/content at recorded revisions. Superproject bundles do not supply these payloads automatically. Inventory relevant repository-hosted wiki/design material or release-only assets if used by the project; Git history alone does not archive GitHub metadata. Record what was and was not recovered. [W3]
3. Create a unique, annotated preservation tag and an archive branch pointing to the clean pre-remake baseline, for example `origin-war-legacy-<UTCSTAMP>` and `archive/pre-squidsquad-<UTCSTAMP>`. Never move or overwrite an existing preservation ref. Separately record local snapshots so a clean remote-baseline tag is not misrepresented as containing uncommitted files.
4. Write `docs/LEGACY_ARCHIVE.md`: baseline hashes, included branches, archive filenames/checksums, LFS/submodule status, restore commands, verification results and any omissions. Keep sensitive inventories and absolute private locations in the private backup, not the published document.
5. Restore/clone the bundle into a disposable directory and verify the expected baseline and refs; extract the file snapshot and compare a SHA-256 manifest. Verify representative source and binary assets. Try the original documented build without modifying the archived originals. A legacy dependency failure is recorded separately from archive-integrity success; modern development need not wait for obsolete dependencies to be repaired.
6. Push **only the explicitly named, reviewed preservation branch/tag** to the verified Origin War remote and confirm their remote hashes. Do not push private bundles, raw `.git` copies, credential-containing snapshots, unrelated branches, or all local tags. Do not use `git push --mirror`, `--all`, blanket `--tags`, or force options. [W4]

Do not begin destructive migration before the local restore check passes. If archive/ref publication is blocked by authentication, preserve and verify locally, record the blocked push, and continue non-destructive implementation on a separate branch; never label the remote archive as complete. Do not enable LFS billing or paid backup storage without approval.

### 0.4 Start the new version in the same Git history

Use one integration branch, normally **`codex/origin-war-squidsquad`**, descended from the reconciled baseline. Reuse that branch if it already contains the authorized remake; don't overwrite it or restart. Use separate worktrees for independent workers. The default branch and historical branches remain unchanged during development; merging the remake to the default branch requires owner approval unless already explicitly granted. A draft PR can make the ongoing branch easy to inspect without requiring an approval after every task.

Prefer a clean modern project layout on the remake branch. Legacy files may be moved into a clearly labeled `legacy/` directory or removed **from that branch's active build** after verified preservation. Do not include the old runtime as a second active engine or copy a giant private backup into Git. Preserve recoverable original content, original design references and asset provenance. Exclude legacy code from new builds where appropriate. Record the migration in its own understandable commit.

Classify findings as `implemented`, `partial`, `documented original intention`, `new implementation decision`, `superseded`, or `unknown`. Audit old asset rights before reuse. A stub class is not evidence of working gameplay. Then build the real game; the archive/audit is a prerequisite, not the deliverable.

### 0.5 Commits, pushes and recoverable progress are required

- Use focused commits describing **what changed and why**, connected to task IDs and verification results. Stage intended paths and inspect the staged diff; do not blindly `git add .` from a shared or unknown working tree. Preserve configured author identity; don't impersonate someone or change global Git settings.
- At every completed coherent task/integration checkpoint: review → compile/test affected code → update necessary status/evidence → commit → **push the integration branch**. Also checkpoint before risky refactors, context handoff and session end. Do not wait for the entire game to be finished before the first push.
- Incomplete work that needs preservation goes to a clearly named **WIP branch/checkpoint**, with failing/untested status recorded, not a green milestone or release tag. The ordinary integration branch should stay buildable; known legacy failures and new regressions must be distinguished.
- Only the lead/integrator publishes integration changes and shared milestone tags. Workers commit only in their assigned worktrees/branches. Fetch before integration; resolve conflicts deliberately, rerun affected checks and serialize Git writes. Never rewrite already pushed history. If a remote push is rejected, inspect the remote change and merge/retest appropriately, rather than force-pushing.
- Verify publication using the remote branch hash (for example `git ls-remote`) against the intended local commit. A local commit, a push command printed in chat, or a PR URL is not proof the push succeeded. Observe CI if available; `pending` is not `passed`. Log the remote SHA and result after the fact in the next checkpoint/report rather than creating an endless commit-to-document-itself loop.
- Keep build outputs, caches, local saves, private snapshots and huge repeated capture dumps out of ordinary Git history. Track reproducible source, required game assets, manifests, build/configuration changes, meaningful tests and compact evidence. Use the repository's existing artifact/LFS policy where appropriate; do not rewrite old history to add LFS or enable paid quotas automatically.
- When auth/network permissions fail, preserve commits locally, report the exact blocker and unpushed refs, and retry only with a reason to expect a changed result. Continue independent local work. Never weaken security, bypass hooks/protection, conceal unpushed work, or claim a successful GitHub update that was not verified.

## 1. Scope and authority rules

The priority order for design decisions is:
1. This request, including the specific decision to build a **full 2016-style game with SquidSquad/libGDX** and to add **only oxygen hallucinations** from later Origin War 2 concepts.
2. The **2016 `Origin War.docx`** and older original source, interpreted as original intent (distinguish explicit plan from already implemented code).
3. Practical implementation decisions needed to make a cohesive, playable, complete game, documented as *new design* rather than falsely attributed to the 2016 designers.
4. Contemporary library documentation and tests, as technical authority for APIs and behavior.

Do **not** carry over Origin War 2's EXO-9 mech, AEGIS enemies, Duskers-inspired command-console identity, persistent echoes/bloodstains, roguelite metaprogression, invented alternative lore or campaign, or wholesale Lie Budget specification. Hallucinations arising from oxygen deprivation are the **single authorized concept to borrow**. You may internally use a modest effect budget/cooldown for clarity and fairness, but do not import Origin War 2 as a game system or narrative.

No Furnace engine, no GBA constraints, no demake. This is a modern desktop roguelike RPG with coherent pixel art—not a game about demonstrating a renderer.

Make reversible routine decisions yourself. Correct technical assumptions after checking the actual repository, current APIs, compiler output and running game. Document material corrections and apply them consistently. **Do not quietly alter these creative requirements** merely because another implementation would be faster. Ask only where a genuinely high-impact, unresolved creative decision cannot be made without contradicting the sources, or access/permissions block an essential action.

## 2. The original 2016 game's story and core identity

**Setting:** The overarching war is between the **Terran Loyalists** and **Martian Colonists**, with deliberate parallels to the American War of Independence. Year 2115. Mars has become habitable through rapid terraforming. On Earth two major powers remain: the United American States (North and South America) and the EuroAsian Federation. War over resources and control of Mars disrupts supply convoys. The Mars colony asks for sovereignty, is refused, and is effectively abandoned when Earth suspends further deliveries. Colonists seize the final shipment's ships and declare independence. Earth responds with a force to reclaim Mars.

**Player:** A young Terran security officer on their first mission. Initially briefed that the Mars colony's communications failed for technical reasons; near Mars, Captain Marcus West reveals the real reclamation operation. The first mission is to secure the research/science station orbiting Mars.

**Important people:**
- **Captain Marcus West:** Experienced, grizzled commander reluctantly returned to military service shortly before retirement.
- **Dr. James Carmack:** Brilliant scientist who designed and clandestinely maintained A.L.I.C.E. aboard the science station. His later attempt to override her ethics and force her to kill the player is a climactic plot event.
- **A.L.I.C.E. — Artificial Lifeform Intended for Control and Enforcement:** A **unique sentient android**, originally designed to replace human police officers. Her project was canceled because she refused to kill an assigned target, falsely characterized as defective ethics. She joins the player as a companion and personality, not just an emotionless HUD voice. Her moral agency is central to the story.

**Original planned conclusion:** As Carmack attempts to subvert her, A.L.I.C.E. refuses to kill the player, transmits the truth about the conflict unencrypted to all available channels, chooses to destroy the station and herself, and seals the player in an escape pod that launches toward Mars. This concludes the full **orbital-station campaign** and can seed a future sequel; don't silently replace it with a generic boss-then-credits ending. The player's choices can change context, relationships and a final epilogue *without invalidating the original intended core ending*.

**Tone:** Atmospheric hard-ish science-fiction adventure, dangerous and lonely but populated with characters, ethical conflict, dry humor (especially from A.L.I.C.E.), suspense and discovery. Exploration and survival first, thoughtful tactical combat second, and story throughout. Do not make it a generic space shooter, pure horror walking simulator, metroidvania, real-time bullet hell, a reskinned fantasy dungeon or Duskers clone.

**A complete game means:** A satisfying beginning, middle and ending; meaningful character and equipment development; multiple types of challenge; replayable procedural levels; fully working combat, progression and exploration; properly authored art, music, menus, save/resume, credits, and a shippable desktop package.

## 3. Nonnegotiable original gameplay requirements

Build these systems as integrated gameplay rather than isolated screens or mocked hooks.

### 3.1 Expedition and procedural station floors
- Top-down **tile-grid movement**, with clearly turn-based decisions and responsive short animations between discrete turns.
- Procedurally generated rooms, corridors, item placement and enemy placement. Different seeds generate distinct **playable** layouts. Support more than one station sector style while respecting station architecture; don't generate arbitrary natural caves as if we're in a fantasy dungeon.
- Limited field of view, explored-but-unseen memory, blocked lines of sight, darkness and local lighting; ensure enemies/objects don't reveal information beyond legitimate sensor/vision rules.
- Old alpha interaction: locate an activation switch/key/security override to unlock an exit hatch, then descend/proceed to the next level. Build on this with optional exploration, environmental narratives, doors and meaningful alternative routes where appropriate.
- A whole run should move **deeper into the orbital station**, revealing its story and increasing danger. Mix generated spaces with **authored anchor rooms/events** for important story beats and boss encounters.
- Seed reproducibility for debugging and scoring. Never generate a seed whose required switch, hatch, essential story trigger or route is irretrievably inaccessible.
- The alpha had a progression ending at level 10. Treat roughly ten decks as a historical reference, **not a blind fixed quota**; choose a complete campaign length based on actual pacing and verified gameplay, with several visually and mechanically distinctive sectors.

### 3.2 Oxygen as the central survival economy
- Oxygen tanks are finite, collectible, visible if discovered, equippable/usable as designed, and meaningfully change the risk of routes, exploration and fights.
- Time/turn cost has clear rules. Terrain costs in the original alpha included standard flooring and movement-modifying conditions such as low/no gravity or difficult terrain. Preserve this *design idea*, but rationalize inconsistencies and make it comprehensible to players.
- Define the relationship among **oxygen reserve, breathing/survival, turns, health damage, terrain and optional oxygen management** unambiguously. Avoid unintentional exploits caused by rapidly toggling life support or repeated menu actions.
- Oxygen scarcity must create decisions but not make legitimate generated levels mathematically impossible in a normal run. Provide warning, emergency alternatives and understandable pacing.
- A human character in practical space-exploration equipment is appropriate; **do not introduce the EXO-9 heavy mech**.

### 3.3 The four original character attributes
- **HP** — health; zero means death/end of run.
- **STR** — melee effectiveness and physically forcing doors.
- **INT** — hacking security computers and persuasion/dialogue checks.
- **DEX** — ranged weapon accuracy and avoiding enemy attacks.

Add coherent in-run advancement/character-build tradeoffs using these original attributes. Show effects in UI and explain how increases change outcomes. Do not replace them with a new skill system without compelling gameplay reason. No passive grind requirement.

### 3.4 Equipment and discovery
- **Armor** mitigates incoming damage, with readable tradeoffs.
- **Guns** consume **battery cells** as ammunition/energy. Different tactical roles and resource costs should make equipment choice meaningful.
- **Medicine** uses the 2016 idea: randomized/unfamiliar pharmaceutical names whose properties are initially unknown; using/identifying a medicine reveals its effects during that run. Clearly represent discovered knowledge and avoid contradictory identity mappings. Make beneficial and risky discoveries feel fair; manage severe effects carefully.
- Inventory, equipment, pickups, examination, comparison and use/drop actions all work with keyboard and mouse. Include item descriptions and stable controls.
- Integrate hacking, locked doors, terminals, security cameras, turrets and alternate routes into actual levels instead of merely mentioning them in dialogue.

### 3.5 Combat, enemy AI and tactics
- Full deterministic, legible **turn-based combat** with movement, aiming/LOS, ranged attacks, melee, protection, hit/dodge, damage/status and enemy turns.
- Original enemies that **must** appear: **S.I.R.S.** (Standard Issue Robotic Soldiers), **A.D.D.** (Advanced Defense Droids), and **R.O.V.E.R.** (Remotely Operated Vehicle for Exploration & Reconnaissance, robot-dog form). They need distinct silhouettes, sounds, tactics and stats, not merely palette swaps. More enemy variants are allowed as authored additions that suit the station and original lore.
- Pathfinding, searching, awareness, cover, choke points, suppressive threat, ranged vs close positioning, sound/line-of-sight reactions as appropriate. A.I. should be fair, intentional and demonstrably different across types.
- **The Constructor boss** is mandatory: it attacks by deploying R.O.V.E.R.s set to explode. Two genuinely viable victory routes from 2016 must work: **hack/redirect the attacking R.O.V.E.R.s back to their controller** or **physically locate and defeat the Constructor's operator**. Neither route may be a fake dialogue option. A non-INT build needs a credible route.
- Add carefully scoped bosses/major encounters where they strengthen the full campaign, but preserve The Constructor and A.L.I.C.E.'s story climax as anchors.

### 3.6 A.L.I.C.E., storytelling and player agency
- A.L.I.C.E. is a companion with an evolving relationship to the player, reactions to discoveries, useful nonlethal assistance/analysis, dry dialogue, and credible limitations. Her original refusal to kill should be reflected in behavior and story; do not make her a generic lethal combat pet.
- Implement Captain West's misleading briefing, the discovery of the colonists' predicament, Carmack's secrecy and plan, and a believable route to the original finale.
- Use logs, dialogue, short in-engine scenes and location-specific discoveries, not giant exposition dumps. Dialogue should reflect the war's moral ambiguity rather than treating Martian independence as inherently villainous.
- INT-based persuasion should produce meaningful outcomes where conversations are plausible. Include human survivors, recordings or station personnel as needed for the narrative; don't transform all gameplay into talking to NPCs.
- Narrative beat locations must survive procedural variation. Provide an in-game journal/map/objective log to stop players losing their place after time away.

## 4. The ONE addition: oxygen-deprivation hallucinations

As oxygen becomes critically low, the player's **perception** becomes less reliable. This should be a strong, distinctive feature of Origin War, but not a remake of the 2025 Origin War 2 design.

Implement a progression of effects that can include subtle audio changes, breathing/heartbeat, wavering or unusual colors/light, peripheral silhouettes, momentary false enemy impressions, ghosted movement, unreliable incidental environmental messages, and a few carefully designed moments when the player cannot immediately tell whether something was real. Increase frequency/severity smoothly as hypoxia worsens; effects stop or subside when oxygen is restored and the player recovers.

**Fundamental engineering rule:** Maintain separate **authoritative simulation state** and **perceived/rendered state**. A hallucinated enemy is not a real enemy, a phantom exit is not allowed to permanently alter the path graph, and an audio/UI distortion must not corrupt saved actual resources. The player should have reasonably accessible ways to verify important facts; misdirection creates tension, not unfair unavoidable death or permanent softlock.

Test hallucinations at normal, warning and critical oxygen levels. Verify deterministic playback for seeded tests; prevent stacked effects from obscuring controls or required information excessively. The system must work through actual libGDX rendering/audio and gameplay state, not screenshots of proposed effects.

**Explicit exclusions:** no EXO-9, AEGIS taxonomy, mandatory typed suit-console minigame, persistent echoes, or entirely new alien-biohorror premise. An internal hallucination scheduler is fine; don't import the entire later framework.

## 5. Visual/art direction — create the actual assets

**Make the final game visually cohesive and recognizably complete.** This is a graphical pixel-art sci-fi roguelike, not an ASCII-only proof of concept. Render through libGDX. SquidSquad handles maps and simulation; it need not own every visible pixel. Use sprites, texture atlases, tile layers, overlays, particles and shader/light effects where helpful.

Suggested visual identity: industrial orbital research station; worn metal, sealed bulkheads, observation windows with Mars outside, illuminated instruments, deep shadows, sickly emergency lighting, medical/research areas, maintenance tunnels and sterile command/security spaces. Favor readable silhouettes and a controlled palette over noisy AI-looking textures. The visuals should tell the story of a station whose political and technical order is breaking down.

Create and integrate original production-quality assets, including at minimum:
- One cohesive multi-sector tile/decoration kit (floors, walls, corners, doors open/closed, hatches, panels, glass/windows, terminals, alarms, pipes, debris, cover, medical/industrial props, environmental hazards).
- Fully readable top-down character sprites and animations for player, A.L.I.C.E., major human NPCs and each enemy, plus The Constructor encounter's machines.
- Weapons, armor, battery cells, oxygen tanks, medicines, inventory icons, world pickups and interaction markers.
- Combat, hacking, door, alarm, oxygen, injury and hallucination effects.
- Main menu/branding, map/minimap, inventory/equipment, stats, journal/logs, dialogue, tactical feedback, controls/options, game over, victory and credits.
- Environmental variant details and lighting states so decks are distinct and alive, not the same gray corridor repeating endlessly.

Choose a consistent native pixel scale/tile size and screen presentation after checking readability at desktop window sizes. Use nearest-neighbor scaling for pixel art; provide adjustable fullscreen/windowed resolution and sensible pixel-perfect layout. A 16/24/32-pixel logical tile grid is a design option, **not a non-negotiable assumption**. UI typography can use crisp scalable rendering if that makes dense RPG text readable.

For asset production, inspect the actual tools available in this Codex workspace. Use accessible image/illustration generation tools if authorized and supported; otherwise **author assets programmatically or with local asset editors**, including layered pixel sprites, palette variants and geometry-assisted sprite construction. Build a repeatable export/atlas pipeline. An AI image prompt, an empty directory, colored rectangles, tiny stick figures, Unicode characters, stock-demo tiles, and assets copied without a valid license **do not satisfy the final-art requirement**. Temporary prototypes are fine during development but must be replaced or explicitly flagged before claiming completion.

Use generated/third-party packs only with confirmed redistribution rights and consistent art direction. Keep `assets/ASSET_PROVENANCE.md` with every asset's source, license, generation method and edits. Preserve/source original authorable sprites when feasible, plus exported formats used by the game. Do not assume old `core/assets/` files are free to redistribute.

Quality gate: launch and capture **real in-engine screenshots** of at least one floor per major sector, gameplay with entities, inventory, dialogue, combat, and at least three hypoxia states. Review actual pixels for scale, clarity, animation, seams, occlusion, bright/dark balance and UI legibility; iterate until believable. Do not substitute concept art for screenshots.

## 6. Music and sound — actually produce and integrate them

The full game must **ship with original usable music and sound effects**, not a mute game, reference YouTube tracks, fake file names, or MIDI files the game cannot play. The source alpha has existing sound files with unverified provenance; audit licensing before reuse. Original new tracks are strongly preferred.

Produce a coherent soundtrack with at least the following distinct, loopable cues (scope can grow if worthwhile):
1. Main-menu/intro theme establishing the game's identity.
2. Lonely station exploration/ambient score.
3. Tense security/combat variation.
4. Oxygen danger/hypoxia soundscape that shifts as oxygen falls.
5. A.L.I.C.E. character/ethical-choice motif and emotional finale cue.
6. Ending/credits resolution.

Music should favor memorable motifs and restrained science-fiction textures: synths, industrial percussion, faint signals, atmospheric pads, subtle dissonance and dynamic layers. Avoid looping one generic 30-second ambient drone throughout every deck. Compose music by available legitimate means: original tracker/MIDI work rendered with rights-cleared instruments/soundfonts, synthesized waveforms with authored patterns/arrangement, a local DAW workflow, or a supported generative-audio tool whose output can be legally shipped. Include source projects or scripts and final compressed streaming audio (for example OGG), with actual playable loops tested. Don't claim a music model is available without checking.

Produce and integrate an SFX library, ideally **25+ distinct purposeful cues**: normal/metal/low-gravity footsteps, oxygen refill/use/depletion, breath/pulse/warning, doors/locks, switches/hatches, hacked terminals, gun/melee impacts, robotic footsteps and attacks, R.O.V.E.R. movement/countdown/explosion, item pickup, medicine identification, UI navigation/confirm/error, alerts, voiced-feel dialogue text beeps, hallucinations, death and victory. The distinction between an actual R.O.V.E.R. threat and a hallucinated one should be tactically intelligible without breaking the mood.

Use libGDX `Sound` for short effects and `Music` for streamed tracks unless a proven alternative offers real value. Implement master/music/SFX volume sliders, mute, pause/focus behavior, loop/crossfade or clean transitions, and prevent audio duplication/leaks on level transitions. Loudness-match cues by listening/level inspection. Verify file decoding, loop behavior, and multiple simultaneous SFX in a running build.

Provide `assets/AUDIO_PROVENANCE.md` and the compositional/generation source. **Do not call audio complete until the game audibly plays original music and cues.**

## 7. Current technical stack — verify instead of guessing APIs

Earlier preparation named the following **dependency candidates**, not a resolved lockfile. Recheck upstream documentation, actual installed APIs and build compatibility before selecting/pinning them; do not confuse a number printed in this prompt with a successfully verified dependency:
- **libGDX 1.14.2**, desktop through **LWJGL3** (not the obsolete old LWJGL2 desktop backend).
- **SquidSquad 4.0.12** stable via verified artifact repository (published examples use `com.github.yellowstonegames.squidsquad` and JitPack). Confirm exact versions, module names, repository configuration and transitive dependencies from current docs and by actually resolving/building. Pin a known-good release/commit; do not use mutable `-SNAPSHOT` artifacts without strong justification.
- A modern JDK/Gradle toolchain compatible with the selected libGDX bootstrap project. **Don't mechanically convert source code using API names from 2016 SquidLib**; API packages changed.
- Consider `gdx-liftoff` for generating a modern Gradle/LWJGL3 project if rebuilding the old Gradle setup is more expensive than carrying code into a clean modern structure.

Likely SquidSquad modules, to be confirmed during implementation:
- `squidplace` for map generation and room decoration.
- `squidgrid` for grid/visibility/field-of-view and related algorithms.
- `squidpath` for Dijkstra/A* pathfinding (compare with `squidseek` using the selected release's current documentation).
- `squidstore*` where genuinely useful for serializing SquidSquad types; custom versioned game-state save code remains your responsibility.
- `squidsmooth` for turn-to-turn visual animation if it fits; `squidglyph` is optional for textual overlays but **not a requirement to render an ASCII-only game**.

Use official references rather than inventing class names:
- Original repository: https://github.com/KNMILLS/OriginWarAlpha
- SquidSquad: https://github.com/yellowstonegames/SquidSquad
- libGDX: https://libgdx.com/ and https://github.com/libgdx/libgdx
- libGDX gdx-liftoff: https://github.com/libgdx/gdx-liftoff
- libGDX audio docs: https://libgdx.com/wiki/audio/audio
- Codex iteration guidance: https://developers.openai.com/cookbook/examples/codex/iterating-development-workflows-with-codex

Design boundaries:
- `core` pure game simulation where practical: seeded RNG, authoritative grid/world state, generated floor graph, turn scheduler, rules, enemy decision system, LOS/FOV, combat, items, stats, quests, dialogue effects, hallucination truth/perception separation.
- `desktop`/`lwjgl3` launch/runtime; rendering, audio, input, screen flow and platform features. Pure/headless model tests must not require an OpenGL window.
- Content in structured/versioned definitions where helpful; don't bury every number and quest line in an 8,000-line update/render method.
- Stable IDs for tiles, encounters, items, objectives, AI, sprite families and story beats. No magic string keys scattered across subsystems.
- Keep performance bounded with cached FOV/path calculations, occupancy grids, atlas batching and scene-aware rendering; profile before inventing an elaborate engine.
- Use a modern save format with schema version, bounds validation, atomic/backup write and deterministic replay seed. Suspend/resume and autosave should work without eliminating the roguelike's death stakes. Avoid online/cloud requirements for core play.
- The original document included an optional leaderboard server. Implement an offline score/stat screen and, if reasonable, a **self-hostable** service/API option. A live hosted service, accounts or publishing costs are **not authorized**; do not delay the complete offline game for external infrastructure. If implemented, label anti-cheat limitations honestly.

The exact internal class diagram is **not** a mandated design. Adjust it to the actual modern library and tests. Document substantive architectural changes in `docs/DECISIONS.md` and update every affected test and specification.

## 8. Full-game pacing and content expectations

Plan a bounded, complete orbital-station campaign, not an endlessly expanding roadmap. Use a coherent sequence such as arrival/security breach → maintenance/habitation → research/medical → automated defense → Carmack/A.L.I.C.E. revelations → station collapse and escape. **These labels are suggestions**, not invented 2016 canon. Pace combat, oxygen scarcity, narrative discoveries and noncombat problem solving across the full run.

A plausible initial content target to tune via actual playtests: ~8–12 substantially varied floors distributed across 4–6 visually recognizable station zones, multiple enemy behaviors, at least the original three enemy archetypes, one fully scripted Constructor boss with both victory routes, additional major set-piece challenges, a meaningful item pool, several hacking/STR/DEX alternative paths, character growth, authored lore/dialogue, and a complete ending. Do **not** use a fixed quantity as a substitute for depth; document final numbers and explain adjustments from playtesting.

The game should have enough authored variety that runs are recognizably different but not so random that the main story breaks. Generated spatial complexity should produce decisions about oxygen, equipment and enemy routes; simply increasing maze length is not progression.

Provide a title screen, New Game, Resume, Settings, How to Play, Credits, death/retry, victory, accessibility basics (remappable keys or clear alternate inputs, readable UI scaling, adjustable sound, reduced screen-distortion), and final packaged build. Support mouse/keyboard well; controller only if it can be fully implemented and verified without slowing the core game. The original target was mobile, but **this commission prioritizes desktop**; don't build simultaneous iOS/Android releases unless explicitly requested later.

## 9. Testing, simulation and quality gates

Trace the real production path first. Add the smallest useful regression check and production change, get that code compiling/running, then extend verification around **that same gameplay implementation**. Do not spend a phase building a separate fake game, benchmark framework or orchestration platform instead of the requested feature. Existing reliable tools take priority over new wrappers. Every feature needs reproducible tests and a visual/audio check when appropriate. At minimum:

**Generation:** Exercise hundreds/thousands of deterministic seeds; validate walkable start, room connections, reachable required switch/exit, no essential actors/items trapped inside walls, appropriate floor bounds, no unreachable mandatory narrative room, spawn safety, sane oxygen opportunities and no softlock under intended rules. Log failing seeds and replay them. Randomized loot may be uneven; progression must never be logically impossible.

**Turn/combat:** Verify action costs, explicit pass/wait, enemy scheduling, movement collisions, weapon range/LOS, cover, boss attack telegraphs, INT hacks, STR force-door paths, DEX/armor interactions, death, no extra enemy actions because an animation replayed, no action during paused menus, save/resume roundtrip and deterministic seeded combat outcomes.

**Oxygen/hallucination:** Verify oxygen consumption across terrain/actions, refill behavior, overdraw/zero cases, warning transitions, status ailments, perceptual-only phantom entities, true vs perceived map separation, recoverability, no permanent phantom blockers, no real damage from unreal enemies, no hidden irreversible quest-state change, and no unwinnable generated map due to deceptive effects.

**Narrative:** Every required beat fires exactly once or according to clear replay rules. All seeded floor permutations can reach the original canonical ending. The Constructor's two routes both work. A.L.I.C.E.'s moral choice and ending are interactive/in-engine and persist correctly after reload.

**Presentation:** Capture real gameplay screenshots and short video/GIF or automated frame captures from a **running build** at representative resolutions and oxygen levels. Check that the inventory and dialogue are not clipped, lighting doesn't hide mandatory information, sprites don't overlap HUD, and animations play at normal frame rates. Inspect and listen to **actual exported** art/audio.

**Packaging:** Fresh checkout build, tests, Windows desktop launch (or explicit Windows validation gap if the execution host is non-Windows), save/reload, a scripted complete run or replay trace from new game to credits, final distributable archive with all runtime dependencies and assets. Never say a build is validated on Windows unless you actually validated it there.

Maintain `docs/VERIFICATION.md` with the tested commit/build ID, environment, exact commands, exit status, actual test counts, seeds, capture paths and unresolved limitations. Separate **implemented / compiled / headless-tested / runtime-tested / visually-reviewed / audibly-reviewed / packaged / remotely-pushed** status. Do not mark all of them green because one step passed. Reopen relevant evidence after a change invalidates it.

At milestone/release gates, an independent reviewer must inspect the integrated changes against requirements, not merely repeat the implementer's summary. Test adverse ordering, repeated interactions, malformed saves, death during transitions, lost focus, menu/input boundaries, inventory overflow, resource starvation and both Constructor routes. Diagnose whether a failure belongs to production code, the test, toolchain or environment before changing anything. Never weaken assertions, disable tests, fabricate expected output, or lower quality targets to manufacture a pass. Legitimate test corrections require an explanation and evidence.

Keep quick incremental checks for ordinary edits; use a fresh disposable checkout/worktree for canonical clean-build/package verification. Do not repeatedly delete the primary Gradle/build caches or rebuild unchanged assets. Test isolated user-data directories so automation cannot destroy a real save. Use deterministic seeds and **separate simulation and presentation RNG streams** so adding hallucination art/audio does not change combat outcomes or loot. Record real reproducibility rather than promising it from the architecture alone.

A real-play gate must launch the current integrated game, exercise controls/menus and the feature, and inspect captured output. Audio decoding/waveform checks do not establish that the sound was actually heard through the running game. If listening, GUI access or Windows execution is unavailable, label the gap and retain an explicit validation task; do not call it completed. Green unit tests are not a finished game.

## 10. Autonomous workflow, subagents and lessons carried forward

You are the lead engineer and integration owner. The owner should direct the game, not manually supervise agents, paste messages between them, select the next ordinary ticket, or approve every commit. Continue within the authorized scope until the full acceptance contract is met or a genuine blocker/session limit requires a checkpoint. This does not grant unattended future execution outside an available active task or authorization to consume paid external services.

### 10.1 A small, current source of truth

Read the full directive and original design at kickoff. Then keep a concise `AGENTS.md` with guardrails, actual commands, branch/worktree rules and links—not a copy of this entire document. Store the full directive in a stable project-doc path. On resume, read applicable guidance, current `docs/PROGRESS.md`, the relevant plan/tasks, and Git status/diff before acting. Load full specifications only for the task at hand; verify approved content rather than relying on an old chat summary. [W5]

Create or reuse these **small** records; don't duplicate an existing equivalent tracker:
- `docs/ORIGINAL_2016_REQUIREMENTS.md` and `docs/GAME_BIBLE.md`: original intent versus new implementation choices, with stable requirement IDs. Link to the unchanged design document.
- `docs/IMPLEMENTATION_PLAN.md`: milestones and **one task ledger** with ID, goal, requirements, dependencies, owner, owned files, branch/worktree, acceptance checks, state and evidence. Keep one canonical ledger, not competing TODO files, issue systems and spreadsheets.
- `docs/PROGRESS.md`: current branch/HEAD, latest verified pushed commit, current playable state, active workers/ownership, blockers, known failures and the exact next executable step. Distinguish remote-pending from remote-verified.
- `docs/DECISIONS.md`: material corrections, rejected approaches, owner decisions and small failure-to-rule lessons. Include rationale and affected files/tests. No repeated speculative redesign essays.
- `docs/VERIFICATION.md`, `docs/LEGACY_ARCHIVE.md`, and asset/audio provenance records as specified above. Keep detailed raw logs outside auto-loaded guidance; link useful evidence.

Task status should distinguish **planned → ready → in progress → review → integrated → verified**, with explicit blocked/reopened states. Local completion, integration, runtime verification and push status are separate facts. Each required feature must link to real code/content and acceptance evidence. File existence and claimed percentages are not completion.

### 10.2 Use subagents deliberately, not as a permanent swarm

**Explicitly use available subagents for bounded independent work and independent verification where that improves throughput.** Inspect the actual Codex environment, tools and supported agent configuration first. Do not invent agent APIs, model names, concurrency settings or capabilities. Native orchestration is preferred; do not build an agent-management platform or spawn recursive copies of Codex to simulate unavailable tools. If subagents cannot run, perform the same bounded tasks sequentially and report that limitation. [W6]

Start with **one lead and at most two workers concurrently** as a project default, not a claimed platform limit. Increase only for truly independent work when CPU/RAM, build tools and integration capacity support it; record the reason. Close/reuse finished agents. Do not maximize agent count or assign the entire game to every agent. Use a fresh worker for a coherent task; checkpoint the lead at natural milestones before context becomes stale. Where supported, choose available lower-cost models for narrow exploration/checks and stronger reasoning for architecture or difficult review; never downgrade beyond reliable acceptance merely to save tokens.

The lead owns shared contracts, the turn/state model, save schema, dependency/build files, task ledger, top-level instructions, atlas/audio registries and integration branch. Workers may propose changes to these, but must coordinate instead of silently editing them. All shared registries/generated outputs have **one writer at a time**.

Before delegation, give each worker:
```
Task ID and player-visible goal:
Baseline commit; branch and absolute worktree path:
Requirements/source sections to read:
Owned files/directories; read-only interfaces:
Required interfaces, asset IDs and invariants:
Dependencies already available; explicit exclusions:
Tests and runtime/asset evidence required:
Bounded deliverable; stop/escalation conditions:
Return: commit SHA, changed files, tests actually run/results,
        evidence paths, limitations and integration notes.
```

Give concurrent code/asset writers **distinct branches and worktrees** starting from the agreed integrated commit. Do not let two sessions edit the same checkout, index, save directory, atlas output or build directory. Worktrees share repository refs and some configuration: the lead must still serialize shared Git operations. Pass small relevant context and concrete paths, not the entire conversation/project history to every worker. [W7]

Good parallel work once interfaces are stable: isolated robot behaviors and tests; tile/sprite source assets in separately owned folders; music/SFX source production; source/license audits; a read-only edge-case review. Bad parallel work: two turn schedulers; two migration architectures; generation and FOV implementations inventing incompatible grids; multiple workers repacking the same atlas; workers all editing Gradle or the save schema. Art/audio workers should hand off source and exports to the lead's actual integration path, not create disconnected showcase scenes.

Subagents do not re-delegate without the lead's explicit approval. They cannot change scope, publish integration refs, remove another worker's code, or weaken tests. The lead continues independent critical-path work while workers run; it must not implement their assigned task in parallel just to stay busy. If ownership changes, stop/reassign explicitly.

At handoff, the lead reads the diff and evidence, checks API/requirements compatibility and merges/cherry-picks **serially**. Run integrated tests and a runtime smoke check; isolated worker success is insufficient. Resolve conflicts rather than selecting “ours/theirs” wholesale. Use a separate read-only reviewer for milestone acceptance when available; otherwise do a distinct review pass and label it honestly, not “independent subagent review.” Preserve worker commits until integration and push are verified; remove only clean, task-owned worktrees whose work is safely recoverable.

### 10.3 The production loop

**Inspect → choose one ready task → trace the production path → implement a bounded change with regression checks → compile → test/play/inspect/listen as applicable → review → integrate → commit/push → update checkpoint → continue.** Use the real source and actual library APIs; don't code from guessed names. Carry routine reversible engineering decisions yourself, with material corrections recorded consistently.

Keep playable development builds available throughout. Don't wait until the last phase to discover that art, audio, inventory, saves and narrative cannot coexist. Establish one representative quality bar early: actual station art, animated actor/robot, one composed loop, purposeful SFX and the original gameplay on screen. Then produce content against those proven interfaces in parallel while the campaign grows. The later asset-completion milestone is a cleanup gate, not permission to postpone audiovisual work until the end.

Use project-local reproducible tooling, the checked-in Gradle wrapper, pinned compatible dependencies and task-isolated user data. Reuse proven commands. Prefer incremental compilation, cached asset generation keyed by source changes and small seed sweeps during iteration; run broad sweeps and fresh-checkout gates at meaningful milestones. Benchmark bottlenecks before speculative optimization. Don't keep cleaning builds, touching every generated file, updating dependencies or replacing infrastructure without evidence of a benefit.

Add a minimal CI workflow for the remake branch/PR if the repository's existing permissions allow it: compile/test, validate data/assets, package or smoke-test where supported, and retain compact test artifacts. Use least-privilege credentials, no embedded secrets and no new paid/self-hosted infrastructure. No infinite agent-triggered CI loops. If CI is unavailable, equivalent reproducible local checks are still required and the remote CI gap must be recorded.

### 10.4 Lessons that must govern this build

- **The approved game is the product.** No substitute engine, disconnected demo, bespoke orchestration stack, elaborate test platform or documentation factory. Reuse working systems; don't create redundant managers/adapters just because another agent missed the existing one.
- **Inspect before replacing.** Preserve latest approved functionality, uncommitted work, original assets and stable build behavior. Remove obsolete active code only after tracing dependencies and verifying the archive. Never resurrect rejected features from an old document or another game.
- **Correct the general rule, not one symptom.** When a bug exposes a faulty assumption, add a focused regression and update the relevant durable instruction/contracts. Don't append every transient error to `AGENTS.md` or treat a one-off workaround as universal policy.
- **Keep one source of behavior.** Tests exercise production code; renderer/audio do not change turn rules; hallucinations change perception, not authoritative saves/collision/resources. All tuning descriptions, help text and mechanics agree.
- **Don't make the tests green by changing the goal.** No weakened assertions, fake fixtures masking failures, silent feature cuts, disabled audio, reduced visual scope or fabricated completion. Repair production or a demonstrably wrong test and explain the distinction.
- **Real assets, not promises.** Exported sprites must animate and align in the actual game; composed music must decode, loop and play; provenance must be real. Track temporary assets as replacement tasks. A generated image or audio filename is not an integrated asset.
- **Use terminal/build tools before desktop control.** Don't commandeer the owner's mouse/keyboard, interrupt their work, close unrelated windows or alter global settings when a CLI or isolated app window works. Coordinate any genuinely necessary interactive access and never claim local inspection that did not occur.
- **Bound failure loops.** After two materially similar failed attempts, stop repeating them: capture the smallest reproducible case, revisit the hypothesis/API/source, seek focused review or change the technical approach. A new attempt needs new evidence. No endless retrying the same download, test, build or unavailable MCP service.
- **Measure progress in working features.** Track integrated requirements, real runs and preserved remote commits—not hours spent, agents spawned, files written or unsupported percentages. Make engineering simplifications openly without weakening the required player experience.
- **The owner is the director.** Ask only for an irreducible creative conflict, destructive/external action beyond this authority, missing access, spending or another genuine blocker. Otherwise choose the next ready task and continue; don't end every milestone with “what next?”

These are carried-forward project rules and practical workflow decisions. They are not permission to copy mechanics, lore or technical constraints from Pallet, Furnace, PRIVATEER or other projects into Origin War.

### 10.5 Milestones and stopping conditions

Use this sequence as a dependency guide; correct technical sequencing with evidence while preserving the full scope:

**M0 — recovery and modern bootstrap.** Complete the current-state acquisition, verified archives and preservation refs; establish/reuse the remake branch and push the initial checkpoint. Build and launch the modern SquidSquad/libGDX application, with minimal tests. Don't get stuck modernizing the archived alpha.

**M1 — playable exploration/survival and quality sample.** Generated station floor, movement/FOV/lighting, oxygen/terrain, switch/hatch, death/restart and HUD. Integrate representative original art/animation, one music loop and key SFX early; temporary gaps have explicit tasks. Publish a runnable checkpoint.

**M2 — original RPG systems.** STR/INT/DEX, combat, inventory, guns/batteries, armor, medicine identification, S.I.R.S./A.D.D./R.O.V.E.R., hacking and alternative paths. Integrate suitable art/audio as the systems land.

**M3 — oxygen hallucinations.** Truth/perception separation, actual visual/audio effects, recovery, fair feedback, deterministic tests and runtime evidence. No wholesale Origin War 2 redesign.

**M4 — full station campaign.** Distinct sectors, authored story anchors, A.L.I.C.E./West/Carmack, Constructor's two real routes, journal/objectives, original sacrifice/broadcast/escape ending, progression and balance.

**M5 — production-content completion.** Finish all art, animations, original music, SFX, UI, credits and provenance; remove documented placeholders. Review the integrated audiovisual game, not just an asset directory.

**M6 — release candidate.** Broad seeds/edge cases, full beginning-to-end runs, original ending and both Constructor approaches, performance/accessibility/save validation, independent review, fresh-checkout package and actual Windows validation or a clearly unresolved gap. Commit and push final source/evidence; provide the distributable and release instructions. Don't publish a store listing, live service or GitHub Release without its required authorization.

Every milestone closes with source/build ID, genuine test/runtime evidence, remaining content/bugs, archive/push/CI status and next action. **Milestones are checkpoints, not permission to stop at a prototype.** A missing live-service SDK does not block the offline game; a lack of audio/image tools requires original local authoring or an honest production gap, not a pretend generated deliverable.

On a session/context/resource limit: finish a safe checkpoint, stop/collect active workers, commit and push reviewed work (or label WIP), and write exact resume steps plus active ownership. Do not imply work will continue in the background after the execution ends. On resume, fetch and compare the saved checkpoint with actual current source before continuing.

## 11. Final acceptance contract

I should be able to download/run the finished Origin War on a Windows PC, start a new seeded game, explore varied station maps, run out of oxygen and experience genuine oxygen-deprivation hallucinations, refill oxygen and recover, fight distinct robots using turn-based tactics, use STR/INT/DEX to solve real problems, equip guns/armor, discover unidentified medicines, follow and influence a credible story alongside A.L.I.C.E., beat The Constructor through either planned route, encounter Carmack's attempt to override A.L.I.C.E., and reach the original station-destruction/escape ending **while seeing a consistent illustrated game and hearing its original soundtrack and sound design**.

The package must include playable binaries/runtime, all assets/music/sounds, source and reproducible builds, credits and rights records, a concise manual, representative screenshots and truthful verification evidence. The same Origin War repository must contain the reviewed remake commits and published preservation refs, with verified remote hashes or explicitly reported publication blockers. Provide legacy restore instructions, the tested release-candidate commit, branch/PR location and an accurate list of anything still unverified. "All scripts exist" is not equivalent to a working game. "All tests pass" is not equivalent to art/music quality. A concept image is not a screenshot of a running program. Do not mark anything complete without checking it.

## 12. Your first concrete actions now

1. Locate the actual Origin War checkout, inspect it read-only and protect existing local files/history before any pull or migration. Confirm the remote is `KNMILLS/OriginWarAlpha` and discover its current branches.
2. Fetch/pull the latest remote state safely; reconcile newer local/remake work rather than resetting it. Create the verified private file snapshot and history bundle, restore-check them, and publish the specifically named safe preservation refs. Record any real gaps.
3. Establish or resume `codex/origin-war-squidsquad` in the same history. Read the original design and this directive; write concise source mapping, current commands, task dependencies and ownership. Don't build a giant speculative GDD or agent platform first.
4. Verify modern dependency APIs/build tooling, correcting candidate assumptions; build and launch the actual desktop game. Commit and push the initial modern checkpoint.
5. Once interfaces and ownership are clear, delegate bounded independent work to available subagents/worktrees, keep the lead on the critical path and independently review integration. Start real art/music/SFX production alongside gameplay, not after everything else.
6. Implement/play/test the generated-floor oxygen/switch/hatch loop and continue through the full RPG, story, hallucination and audiovisual scope. Commit, push, inspect CI and checkpoint progressively.
7. Finish with a playable packaged game, verified preservation and repository history, real evidence, and clear residual blockers—not only a plan or a pile of scripts.

**Execute the implementation task in the real workspace. Preserve the original, build the complete remake, test the integrated result, and keep the verified work pushed to the existing Origin War repository.**

## 13. Workflow reference notes

These references support tooling details, not the game's creative requirements. Check the versions actually available at execution time. The concurrency cap, branch policy, review cadence and milestone choices above are **project decisions**, not universal platform rules. The existing game requirements remain authoritative.

- **[W1] Git — `git bundle`:** https://git-scm.com/docs/git-bundle — history bundles and verification; separate preservation is needed for working files/configuration/local state.
- **[W2] Git — `git pull`:** https://git-scm.com/docs/git-pull — fetch/integration and explicit fast-forward behavior.
- **[W3] GitHub — backing up a repository:** https://docs.github.com/en/repositories/archiving-a-github-repository/backing-up-a-repository — repository backup scope, LFS and separately hosted material.
- **[W4] Git — `git push`:** https://git-scm.com/docs/git-push — ref-specific publication and remote-update behavior.
- **[W5] OpenAI — project guidance:** https://developers.openai.com/codex/guides/agents-md — scoped durable instructions; keep automatically loaded guidance small.
- **[W6] OpenAI — subagents:** https://developers.openai.com/codex/subagents — supported delegation, task instructions and resource tradeoffs; discover actual availability/configuration.
- **[W7] Git — worktrees:** https://git-scm.com/docs/git-worktree and OpenAI — worktrees: https://developers.openai.com/codex/app/worktrees — isolated working directories with shared repository state.
- **[W8] OpenAI — best practices:** https://developers.openai.com/codex/learn/best-practices — current workflow reference, subordinate to this game's agreed scope.

Workflow references reviewed 20 October 2026. This prompt update does not itself execute an archive, commit, push, subagent job or game build. Those are obligations for the Codex implementation run.
