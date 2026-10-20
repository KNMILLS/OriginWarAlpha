# Legacy preservation

Baseline: `0c22bc2df23fe28aad6a13b8877d44e157bd65f5` on fetched `master`. Full history contains 215 commits; `ken` (`00edb708d232cd98ef8e412260facd9724e95481`) and `evan` (`55debfce07d72429f9080207f61f59c9fb2ae8d0`) are ancestors. No pre-existing modern Git branch was found.

Private archive identifier: `20261020T222309Z`. The owner-accessible backup contains raw Git checkout/config/index/reflogs, stable SHA-256 manifests, the supplied starter files and both unchanged unversioned Godot experiments. Public records intentionally omit private locations/configuration.

| Artifact | SHA-256 | Verification |
| --- | --- | --- |
| origin-war-history.bundle | 44e5225ca0c1e4fdeaad6a79ed662ea70279b381c16234a6e319c6de25f4a43c | Self-contained; bundle verification and restored Git fsck pass |
| legacy-source-assets.zip | 449aad76f01b63ecebbac9d877aadcd30bbce527b84ba620a02a9fe5773eb17a | All 582 tracked source/binary files match extracted SHA-256 hashes |
| Origin War.docx | 73f69847cd160d6d17f447754b26e444e02a3e2e74f8db8e9a521e39daa868bf | Unchanged supplied original |

The independent raw checkout restoration also passes fsck and its 610-entry file manifest. Sources were stable before/after capture. No actual symlinks/junctions were found in the preserved candidates; OneDrive file content was read/copied and verified. This preserves on-disk files, not unsaved editor buffers.

No historical .gitmodules or .gitattributes and no LFS pointers were found across acquired history. GitHub has no releases; the wiki endpoint was not found, and no wiki content is used. Git history does not represent a backup of issue/PR metadata.

Local archive branch: `archive/pre-squidsquad-20261020T222309Z`. Its push is BLOCKED by GitHub GH007 email privacy protection on the historical commit. No author or account privacy settings were changed. The annotated tag `origin-war-legacy-20261020T222309Z` IS remote-verified: tag object `f6030d77d2c7432fb901ff152f4101f4a0759b1c`, peeled commit matches the baseline. Do not claim both preservation refs are remotely published.

Restore with `git clone <private-backup>/origin-war-history.bundle <fresh-path>`, inspect `git bundle list-heads <bundle>`, and check out the recorded baseline. Extract legacy-source-assets.zip into a separate empty directory and compare the private manifest. For original index/config/reflogs, restore the raw private checkout instead. Never overwrite an active checkout.

Original build attempt: `gradlew.bat desktop:classes` failed during the HTTP Gradle 2.10 distribution download (`zip END header not found`). Archived source was not changed to repair it. This legacy build failure is separate from successful archive integrity/restoration.
