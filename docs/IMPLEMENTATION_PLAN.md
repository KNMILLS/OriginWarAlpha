# Implementation task ledger

One lead, at most two bounded workers. Status: planned -> ready -> in progress -> review -> integrated -> verified; blocked/reopened remain explicit. Integration, runtime and remote evidence are separate.

| ID | Goal / requirements | Depends | Owner / files / branch | Acceptance | State / evidence |
| --- | --- | --- | --- | --- | --- |
| M0 | Preserve original; modern build/launch; OW-10 | none | lead; build/model/docs/runtime bootstrap; integration branch | restored archives, tests, actual desktop capture, push hash | bootstrap verified at 3717330, CI PASS; archive branch GH007 exception |
| M1 | Generated oxygen/switch/hatch loop and AV sample; OW-01/02/10 | M0 | lead; M1-ENGINE codex/m1-engine ../worktrees/engine; M1-MEDIA codex/m1-media ../worktrees/media | production seed tests, real input/gameplay, original art/music/SFX | in progress; save/storage boundary verified, engine/media pending |
| M2 | RPG/combat/items/robots/routes; OW-03/04/05 | M1 | unassigned | deterministic combat, all inputs and distinct tactics | planned |
| M3 | Actual hypoxia effects/recovery; OW-09 | M1 | unassigned | normal/warning/critical captures, no truth mutation | planned |
| M4 | Complete campaign and both Constructor routes; OW-06/07/08 | M2/M3 | unassigned | new-game-to-credits runs, both routes, saves | planned |
| M5 | Complete original art/audio/UI/credits; OW-10 | concurrent from M1 | unassigned | 6+ cues, 25+ SFX, no placeholders, actual review | planned |
| M6 | Release candidate; all requirements | M4/M5 | lead plus independent reviewer | 1000 seeds/deck, packaged Windows launch, complete runs | planned |

Workers must have explicit ID, baseline, branch/worktree, owned files/read-only interfaces, acceptance and stop conditions. Only lead publishes integration refs. Each coherent checkpoint is reviewed, tested, committed, pushed and remote-verified. Session limits require a truthful safe checkpoint and exact resume step; milestones do not reduce scope.
