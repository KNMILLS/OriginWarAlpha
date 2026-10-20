# Verification evidence

Environment: Windows x64, Git 2.49.0, portable Temurin 21.0.12.1+1 and Gradle 8.14.3 (download SHA-256 verified). Date: 20 October 2026.

Archive checks: stable private source/backup manifests, independent raw restore fsck, self-contained bundle verify, bundle restore fsck, historical commit existence, all 582 extracted source/binary hashes PASS. No submodules/LFS pointers found in acquired history. See LEGACY_ARCHIVE.md for exact archive hashes.

Original `gradlew.bat desktop:classes`: FAIL during obsolete HTTP wrapper distribution download, before source compilation. Archived originals unchanged.

Modern M0: `gradlew.bat test :lwjgl3:classes` PASS, 2 tests, 0 failures/errors. Actual `:lwjgl3:run --args="--frames=90 --capture=..."` PASS on Windows with OpenGL/LWJGL3. Real 1280x720 bootstrap frame captured and inspected. Initial framebuffer PNG was inverted; corrected the PNG export flip and inspected a fresh running-build capture. Captures are retained outside ordinary Git history under the owner's workspace evidence directory. This verifies bootstrap runtime/capture only, not gameplay, final art or audio.

Evidence statuses are separate: implemented / compiled / headless-tested / runtime-tested / visually-reviewed / audibly-reviewed / packaged / remotely-pushed. Reopen evidence when relevant source changes.

M0 publication: remote integration SHA 3717330568070c26792a2f047e36fc5c9af4af67 matches local. Windows CI run 39508786188 completed SUCCESS. Independent reviewer inspected integrated build/model/archive docs and upright capture; no remaining bootstrap blocker.

M1 persistence boundary: `gradlew.bat test` PASS (2 core + 5 desktop tests). Production JSON roundtrip preserves nested actors/items, RNG, medicine knowledge and perception schedule; corrupt primary restores last good backup; recorded death blocks backup resurrection; new run does not inherit death; malformed bounds/schema rejected. This is headless persistence evidence, not runtime save/resume acceptance yet.
