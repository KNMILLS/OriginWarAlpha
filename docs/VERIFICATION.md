# Verification evidence

Environment: Windows x64, Git 2.49.0, portable Temurin 21.0.12.1+1 and Gradle 8.14.3 (download SHA-256 verified). Date: 20 October 2026.

Archive checks: stable private source/backup manifests, independent raw restore fsck, self-contained bundle verify, bundle restore fsck, historical commit existence, all 582 extracted source/binary hashes PASS. No submodules/LFS pointers found in acquired history. See LEGACY_ARCHIVE.md for exact archive hashes.

Original `gradlew.bat desktop:classes`: FAIL during obsolete HTTP wrapper distribution download, before source compilation. Archived originals unchanged.

Modern M0: `gradlew.bat test :lwjgl3:classes` PASS, 2 tests, 0 failures/errors. Actual `:lwjgl3:run --args="--frames=90 --capture=..."` PASS on Windows with OpenGL/LWJGL3. Real 1280x720 bootstrap frame captured and inspected. Initial framebuffer PNG was inverted; corrected the PNG export flip and inspected a fresh running-build capture. Captures are retained outside ordinary Git history under the owner's workspace evidence directory. This verifies bootstrap runtime/capture only, not gameplay, final art or audio.

Evidence statuses are separate: implemented / compiled / headless-tested / runtime-tested / visually-reviewed / audibly-reviewed / packaged / remotely-pushed. Reopen evidence when relevant source changes.

M0 publication: remote integration SHA 3717330568070c26792a2f047e36fc5c9af4af67 matches local. Windows CI run 39508786188 completed SUCCESS. Independent reviewer inspected integrated build/model/archive docs and upright capture; no remaining bootstrap blocker.

M1 persistence boundary: `gradlew.bat test` PASS (2 core + 5 desktop tests). Production JSON roundtrip preserves nested actors/items, RNG, medicine knowledge and perception schedule; corrupt primary restores last good backup; recorded death blocks backup resurrection; new run does not inherit death; malformed bounds/schema rejected. This is headless persistence evidence, not runtime save/resume acceptance yet.

M1 integrated source: `gradlew.bat test :lwjgl3:classes` and `test :lwjgl3:installDist` PASS; 31 tests total (21 engine, 1 generation sweep, 2 model, 7 persistence). Sweep traverses 200 seeds x 10 decks using actual production finite-resource actions. It does not prove combat balance, narrative completion or the unfinished finale. Suffocation/poison during hatch transitions now resolve on the departing floor; overflow rejects before moving items.

Independent integration review found hidden HUD hitboxes, retained click-walking, missing focus pause, stale prior-run backups and incomplete recovery validation. All five fixed and re-reviewed. New regressions demonstrate first-action corruption cannot resurrect the prior expedition and invalid objective coordinates fall back before engine construction.

Actual Windows runtime at seed 2016: generated illustrated floor/HUD, inventory and fresh-process resume framebuffer captures visually inspected at 1280x720. Native Computer Use actions: G pickup -> tick 1 / reserve 359; Tab then Space -> still tick 1; mouse Use -> tick 2 / reserve 359 / one tank remaining; pause then Space -> still tick 2. Fresh process resumes exactly tick 2 and its inventory/logs. No real owner save was used; isolated user data/captures are outside Git. In-game F12 capture is correct. Native helper screenshots intermittently omit foreground OpenGL text; use retained framebuffer captures for visual evidence, and do not attribute that helper defect to game rendering.

Audio exports decode and load as 1 Music + 16 Sound objects; native actions call playback. Actual listening and completed runtime loop/mix evidence remain OPEN. No full-game or release acceptance is claimed.

M1 development package: :lwjgl3:packageWindows PASS. Launched OriginWar.exe with JAVA_HOME empty, bundled runtime, seed 2115 and isolated user data. Exit 0; atlas/music/SFX loaded; real 1280x720 framebuffer captured. This is a development package smoke test, not release acceptance or a fresh-checkout build gate.
