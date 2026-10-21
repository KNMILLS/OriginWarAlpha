# Owner playtest update:20 October2026

The owner reports difficulty reading all text and insufficient first-deck oxygen, and clarifies their original art/sound intent: the player should feel unsettled and tense. They explicitly requested separate dedicated art and sound research/design agents.

## Readability and oxygen update

- UI now uses a smooth, high-resolution font at20px logical default, replacing the nearest-scaled15px bitmap. Further enlargement remains available. Text contrast, opaque reading surfaces, measured headings, buttons, item rows, logs and slider gutters were reviewed at1024x720 with100% and200% settings. Inventory now shows selection/count and has visible Up/Down controls. Error notices remain visible during dialogue and occupy their own bottom strip.
- New expeditions receive900 reserve rather than360, two360-unit carried tanks rather than one240, and a360 opening pickup rather than180:1,980 total early units versus780. Both remain finite. Warning/color thresholds use a quarter of suit capacity. G collects supplies; R refills from a carried tank, and the HUD reports carried tanks.
- Existing expeditions keep their saved capacity and resources. No retroactive free refill or owner-save edit occurs. Start a new run for the larger opening allocation.
- Menus/inspection remain free; paid turns still consume one oxygen per tick; terrain/AI/hypoxia/recovery retain their rules. The earlier route-based feasibility tests did not demonstrate beginner exploration comfort. This is a generous tuning response, not a claim of new human balance acceptance.

Current headless validation:86 tests, zero failures/errors/skips;10,000 authored deck structures; both complete normal-action finite-resource campaigns, full-state replays and real file-backed save checkpoints/fault recovery. New dynamic campaign pilots win alive through operator (seed0,1,926ticks,HP71/reserve194) and redirect (seed1,1,105ticks,HP78/reserve155) routes. Existing immutable normal-action fixtures also still complete, so no accepted command fixture was rewritten to hide a failure.

The enlarged reserve correctly exposed an old500-WAIT test timeout. Its first failure is retained outside Git; the test now drains the actual suit capacity with a bounded margin and preserves all critical/recovery/truth/RNG assertions. Current integrated replay:1,989actions,906actual waits to critical,HP96, real360 tank use, oxygenation20->76 and reserve346. The current package's companion verification receipt records final executable/ZIP/CI checks separately.

## Separate art and sound design

The dedicated agents inspected the preserved original work and actual current implementation, researched primary developer/tool/paper sources and authored separate briefs. They did not replace production assets or claim listening/owner emotional acceptance.

- [Art direction](design/art/FB1_VISUAL_DIRECTION.md) and [annotated comparisons](design/art/FB1_COMPARISON.md): original helmet/visor/expressive human identity, quieter surfaces and motivated light. First proposed comparison is one continuity-led character/portrait and one frozen room, preserving truthful readable UI and actual FOV.
- [Sound direction](design/sound/SOUND_DIRECTION.md), [source/research evidence](design/sound/EVIDENCE.md) and [listening plan](design/sound/LISTENING_PLAN.md): foreground suit/station detail, longer musical gaps, distinct mechanical identities and cues that respect turn-based threat timing. First proposed comparison is90seconds exploration,30seconds hypoxia/recovery and short ROVER signatures.

The owner is the creative authority. Current generated costumes/palette and synthesized score were new remake choices; provenance and successful playback did not establish continuity with the original intent. Source evidence, proposed direction, production, actual listening and owner acceptance remain distinct. Original materials are preserved; rights/attribution are resolved per asset when relevant.

Brief comparison links address the complete repository checkout. In the distribution, extract Source/OriginWarSource.zip to inspect modern authoring files. Preserved legacy code/media are deliberately excluded from the modern source ZIP; consult the verified original preservation tag and LEGACY_ARCHIVE.md for those original references.
