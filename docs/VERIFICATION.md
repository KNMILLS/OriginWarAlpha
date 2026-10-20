# Verification evidence

Environment: Windows x64, Git 2.49.0, portable Temurin 21.0.12.1+1 and Gradle 8.14.3 (download SHA-256 verified). Date: 20 October 2026.

Archive checks: stable private source/backup manifests, independent raw restore fsck, self-contained bundle verify, bundle restore fsck, historical commit existence, all 582 extracted source/binary hashes PASS. No submodules/LFS pointers found in acquired history. See LEGACY_ARCHIVE.md for exact archive hashes.

Original `gradlew.bat desktop:classes`: FAIL during obsolete HTTP wrapper distribution download, before source compilation. Archived originals unchanged.

Modern M0: `gradlew.bat test :lwjgl3:classes` PASS, 2 tests, 0 failures/errors. Actual `:lwjgl3:run --args="--frames=90 --capture=..."` PASS on Windows with OpenGL/LWJGL3. Real 1280x720 bootstrap frame captured and inspected. Initial framebuffer PNG was inverted; corrected the PNG export flip and inspected a fresh running-build capture. Captures are retained outside ordinary Git history under the owner's workspace evidence directory. This verifies bootstrap runtime/capture only, not gameplay, final art or audio.

Evidence statuses are separate: implemented / compiled / headless-tested / runtime-tested / visually-reviewed / audibly-reviewed / packaged / remotely-pushed. Reopen evidence when relevant source changes.
