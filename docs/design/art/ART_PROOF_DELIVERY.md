# Art comparison delivery record

21 October 2026. The first playable comparison is technically verified; owner method selection and artistic acceptance remain open. This post-build record does not change the immutable runtime or its source archive.

## Delivered files

| Item | Location / identity |
| --- | --- |
| Self-contained Windows build | `C:/OriginWarReboot/dist/art-comparison-20261021-image/OriginWar/CompareArt.cmd` |
| Distribution ZIP | `C:/OriginWarReboot/dist/OriginWar-ArtComparison-20261021-windows-x64.zip` |
| ZIP size | 593,158,241 bytes |
| ZIP SHA256 | `8b40bb5e52841827804b829ef858bd7d192f308571f1fee78efa21d4e34d4b09` |
| Exact runtime/source archive commit | `bb7651c08ec204bbf9e9c2d17de5679865fb4038` |
| Full verification receipt | `C:/OriginWarReboot/dist/OriginWar-ArtComparison-20261021-windows-x64.verification.json` |
| Painted normal-speed footage | `C:/OriginWarReboot/.evidence/art-proof-20261021/painted-final.mp4` |
| Rendered normal-speed footage | `C:/OriginWarReboot/.evidence/art-proof-20261021/rendered-final.mp4` |

F6 switches the method in the shared staged room. Move with WASD/arrows; Alt+arrows turns, Ctrl+arrows aims, F fires, L changes the helmet lamp, Tab opens the pack and ESC pauses. Rifle and heavy shell are underfoot. This is a staged art comparison using the production simulation, not the completed quality slice.

## Verified scope

- All 111 tests pass with zero failures/skips. Both exact-runtime-source CI runs pass: [39682712604](https://github.com/KNMILLS/OriginWarAlpha/actions/runs/39682712604), [39682701235](https://github.com/KNMILLS/OriginWarAlpha/actions/runs/39682701235).
- The actual packaged executable launches with the comparison assets. All 16 runtime atlas pages match the catalog. Authoring masters/rejected candidates are absent from the runtime JAR; editable sources remain in the included source archive.
- All 1,413 source archive files match a fresh Git archive of the stated commit. ZIP CRC and every packaged file's hash were checked.
- Paired packaged 16-action movement/fire runs end in identical complete saved states: tick 16, HP 100, reserve 344, cells 8. Separate equipment runs agree at tick 10 with rifle/heavy selected; paused switching retains identical save bytes.
- Final capture disables frame duplication: painted 1,421 frames over 24 seconds (59.21fps), rendered 1,425 (59.38fps). The 14-second normal-speed MP4 copies remove 10 seconds of paused setup: 825 painted frames (58.93fps), 827 rendered (59.07fps). These clips contain video only.
- Uncaptured gameplay was measured at 720p, 1080p and the actual 3440x1440/60Hz owner display. Means are approximately 16.67ms; exact intervals and screenshot-export stalls are scoped in [ART_PROOF.md](ART_PROOF.md) and the receipt. This is not a claim of perfect 60fps capture or completed audible synchronization review.
- Purchase spend: **$0**. No paid asset purchase or external commission was made.

## Recommendation and open gates

The painted candidate is closer to the intended mature illustrated finish. The original Blender rig provides more consistent construction and reusable equipment movement; its current material/shape treatment remains too mannequin-like. Retain the painted visual target while improving the rig's construction and illustrated treatment, or use rig renders as controlled painting references. The owner selects the production method after reviewing normal-speed motion.

Neither candidate establishes Crown Trick/Hades quality. Painted continuity and drawing cadence, rendered foot plants and finish, missing equipment actions, full machine/door states and commercial workplace composition still need work. The 15-20-minute docking/maintenance slice and all-ten-deck art expansion remain pending the approved review gates. The historical native crash root cause remains open.

The [art bible](ART_BIBLE.md), [painted handoff](PAINTED_PROOF.md), [rendered handoff](RENDERED_PROOF.md), complete coverage ledger and exact prompt/provenance records remain editable. Source/technical checks do not substitute for owner artistic, readability, motion or listening acceptance.
