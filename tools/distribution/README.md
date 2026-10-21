# Windows distribution

Use the checked-in Gradle wrapper and the exact Windows x64 Temurin21.0.12.1+1 JDK identified in licenses/temurin/RELEASE_IDENTITY.txt. Development compilation needs JDK21; distribution packaging additionally checks the exact runtime identity so its corresponding bundled sources remain correct.

1. From a clean committed checkout, set JAVA_HOME to that JDK and run `gradlew.bat test :lwjgl3:installDist`.
2. Run `python tools/distribution/fetch_sources.py` with Python3. Six immutable source archives are downloaded and checked without executing downloaded code. Existing mismatches fail and are retained for inspection. You can supply an already verified directory with `-PdependencySourcesDir=<path>`.
3. Run `gradlew.bat :lwjgl3:packageWindows -PwindowsImageDir=<fresh-output-directory>`. Existing output is never overwritten. jpackage creates OriginWar/OriginWar.exe with a bundled runtime. Source paths/checksums and upstream notices are verified before assembly.
4. Test that executable with an isolated `--user-data=<new-directory>` and JAVA_HOME cleared. Extracted app/app.cfg and JARs, runtime/legal, licenses, DependencySources, Source/OriginWarSource.zip, manuals, screenshots, evidence and BUILD_ID must stay together. The game source archive includes the wrapper, current source, authoring assets, documents and unchanged original design reference; preserved legacy runtime/media are excluded. Recover legacy through docs/LEGACY_ARCHIVE.md.
5. ZIP the complete OriginWar directory after inspection, record SHA256 and source commit, and retain the tested app image. The release acceptance supplement must distinguish headless tests, scripted normal-action runs, actual native input, visual inspection and audible review. No GitHub Release/store publication is performed by these tasks.

Runtime dependency JARs remain individually replaceable; see licenses/REPLACEMENT.md. The notices collector is a separate reproducibility tool, not a step that silently changes licenses during ordinary packaging. Original game and asset licensing remains owner-controlled.
