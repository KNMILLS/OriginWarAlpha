# Origin War 0.1.0 — Windows candidate

Extract the entire Windows ZIP, then launch OriginWar/OriginWar.exe. Java is bundled. Read MANUAL.md for controls, survival, equipment and the original campaign. Source/OriginWarSource.zip includes the current source/build wrapper, authoring assets and unchanged 2016 design reference. DependencySources, licenses and runtime/legal must remain with redistributed copies. BUILD_ID.txt supplies the exact packaged source revision; the archive checksum/verification files accompany the delivered ZIP.

This implements the complete ten-deck orbital campaign, including West's briefing/reveal, the unique nonlethal A.L.I.C.E., crew/fleet choices, both physical Constructor solutions, Carmack's override, refusal, public broadcast, chosen sacrifice and pod toward Mars. STR/INT/DEX, three battery guns, armor, per-run medicine identification, security/alternative routes and independent enemy behaviors use one authoritative engine. Hypoxia alters perception while suit readings remain true. The map and expedition record are offline reading screens; no persistent character progression or hosted service is required.

| Verified scope | Evidence |
| --- | --- |
| Headless correctness |84 tests:72 core and12 desktop;1,000 seeds x10 authored deck structures; zero failures/skips on Windows |
| Complete campaigns | Both normal-action, finite-resource routes win alive in the bundled Windows executable; automated planners know geometry, so this is not a human difficulty claim |
| Persistence | Six real file-backed campaign checkpoints, complete state/event determinism, corrupt-primary fallback, permadeath isolation, actual Windows sharing-lock regression and fresh-process final-dialogue resume |
| Current hypoxia | Actual AI-enabled campaign, oxygenation100 ->60 ->20 ->76 after a finite tank; HP96; world-only impressions disappear on recovery; true HUD remains readable |
| Presentation | Five sector families; directional walking/attacks and effects; actual enlarged dialogue, epilogue, map, record, inventory and character captures; independent scoped pixel/source reviews |
| Audio backend | Six original scores and28 original effects load/decode; both full packaged routes and current hypoxia replay have0 failed voices; actual96-second stream loop completed twice |
| Package/source rights | Independent actual JAR/source/runtime/notice audits; exact patched OpenAL/JLayer/JOrbis/Temurin source correspondence;33 upstream notice hashes; no legacy media/private saves/caches in runtime |
| Performance | Current hypoxia run4,279foreground samples: mean16.74ms,p9517.57ms,max84.67ms,heap76MiB; autosaves/captures included, not a GPU/universal benchmark |

Compact actual diagnostics and screenshot provenance are in Evidence/VERIFICATION.md, docs/evidence and docs/screenshots. Prior full campaign executable source was ef823fd; current map/record/hypoxia executable source6b721e8 uses the identical core JAR (SHA2567e6a4c3733c78243a2202d351755dcb22a57edab4a2d4250d9f3b4d49931b48d). The final bounded inventory correction sizes rows from actual wrapped text, fits long titles, and preserves selections and turn rules. First failures are retained outside Git, with corrections recorded in VERIFICATION.md.

Open acceptance is explicit: human listening for artistic quality/mix/seams, human difficulty/feel, and exhaustive observation of every animation variant. The newer native Computer Use snapshot helper failed twice with 'foreground window did not report a process id'; automatic real framebuffers worked. Prior native controls/pause/use/save/resume passed, but current capture-only menu checks do not claim new native mouse/keyboard acceptance. These gaps are not green merely because tests/backend counters pass.

Repository: codex/origin-war-squidsquad, draft implementation PR https://github.com/KNMILLS/OriginWarAlpha/pull/4. Local legacy archives and restore checks passed; preservation tag is published and verified. Archive branch remains blocked by GitHub GH007 historical email privacy. No history/privacy change, master merge, store publication or GitHub Release was performed. See LEGACY_ARCHIVE.md for restore instructions.
