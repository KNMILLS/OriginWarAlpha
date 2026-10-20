# Current checkpoint

Branch: codex/origin-war-squidsquad. Baseline: 0c22bc2df23fe28aad6a13b8877d44e157bd65f5. Latest verified pushed checkpoint: 3717330568070c26792a2f047e36fc5c9af4af67. Windows CI PASS (run 39508786188).

Preservation: private raw snapshot, bundle and source/assets restorations verified. Remote annotated tag verified; archive branch BLOCKED GH007. See LEGACY_ARCHIVE.md.

Playable state: original alpha preserved; modern bootstrap compiles and launches on Windows. Two headless boundary tests pass. Correctly oriented real framebuffer capture inspected at 1280x720. No modern gameplay or media acceptance claimed.

Ownership: lead owns integration/build/model/docs/runtime. M1-ENGINE: codex/m1-engine, ../worktrees/engine, engine production/tests only. M1-MEDIA: codex/m1-media, ../worktrees/media, art/audio sources/exports only. Workers started from 3717330 and require lead coordination before commits. Next step: integrate engine and media with lead's interface, then play/test the actual survival loop.

Known failures: original Gradle 2.10 HTTP distribution is invalid. Archive branch push blocked by GH007. Full campaign, RPG, hallucinations, production media and package are not yet implemented.
