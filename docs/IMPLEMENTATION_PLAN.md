# Implementation task ledger

One lead, at most two bounded workers. Status: planned -> ready -> in progress -> review -> integrated -> verified; blocked/reopened remain explicit. Integration, runtime and remote evidence are separate.

| ID | Goal / requirements | Depends | Owner / files / branch | Acceptance | State / evidence |
| --- | --- | --- | --- | --- | --- |
| M0 | Preserve original; modern build/launch; OW-10 | none | lead; build/model/docs/runtime bootstrap; integration branch | restored archives, tests, actual desktop capture, push hash | bootstrap verified at 3717330, CI PASS; archive branch GH007 exception |
| M1 | Generated oxygen/switch/hatch loop and AV sample; OW-01/02/10 | M0 | lead; engine dca6525 and media 6ef3b06 integrated; worker worktrees retained | production seed tests, real input/gameplay, original art/music/SFX | integrated; 31 tests PASS, native controls and runtime save/resume PASS; audible/loop review OPEN |
| M2 | RPG/combat/items/robots/routes; OW-03/04/05 | M1 | engine worker; engine/** and engine tests; codex/m2-combat; worktrees/engine at6db098f+2f63026 | deterministic combat, all inputs, distinct tactics and actual finite-resource routes | in progress;25 focused checks PASS in worker, full traversal/Constructor checks pending |
| M3 | Actual hypoxia effects/recovery; OW-09 | M1 | lead; perception/**, tests and runtime | normal/warning/critical/recovery captures, no truth mutation | integrated locally;3 pure tests and actual WAIT/USE Windows replay PASS; independent review in progress; audible acceptance OPEN |
| M4 | Complete campaign and both Constructor routes; OW-06/07/08 | M2/M3 | lead; campaign/**, tests/runtime; engine hooks after worker release | new-game-to-credits runs, both routes, saves | in progress; authored graph with5 checks PASS; graph not yet connected to live engine |
| M5 | Complete original art/audio/UI/credits; OW-10 | concurrent from M1 | media worker; audio c32e55f integrated74ea660; now assets/art/animation/** and sectors/** plus owned pack/validator; codex/m5-art at74ea660 |6+ cues,25+ SFX, no placeholders, actual review |6 score roles/28 effects export/decode/repeatability PASS; actual runtime loads/calls PASS; listening/mix OPEN; directional/sector art in progress |
| M6 | Release candidate; all requirements | M4/M5 | lead plus independent reviewer | 1000 seeds/deck, packaged Windows launch, complete runs | planned |

Workers must have explicit ID, baseline, branch/worktree, owned files/read-only interfaces, acceptance and stop conditions. Only lead publishes integration refs. Each coherent checkpoint is reviewed, tested, committed, pushed and remote-verified. Session limits require a truthful safe checkpoint and exact resume step; milestones do not reduce scope.
