# Origin War game bible

Year 2115. A young Terran security officer boards a research station orbiting Mars. Captain Marcus West's technical-failure briefing conceals a reclamation operation against colonists abandoned by Earth. The station reveals the conflict through exploration, survivors, records and A.L.I.C.E., a unique android whose moral agency matters.

The mandatory ending preserves Carmack's attempted override, A.L.I.C.E.'s refusal to kill, unencrypted broadcast, chosen station/self-destruction and the player's pod escape toward Mars. Choices affect trust and context, not her survival. No EXO-9, AEGIS, persistent echoes or Mars sequel campaign.

New implementation defaults: ten decks in five paired zones (docking/security; habitation/maintenance; medical/research; automated defense/industrial; command/evacuation). Cardinal movement, 32px illustrated world tiles and independent readable UI. Tune pacing from complete runs.

Owner playtest clarification,20 October2026: the original art and sound were intentional, aimed at making the player feel unsettled and tense. That emotional direction governs the creative work. Remake palette/costume/score choices were implementation proposals, not evidence of original continuity. Separate researched art and sound briefs live in docs/design/art and docs/design/sound. Stable readable information and room to learn the opening support that tension.

Accepted actions resolve once, followed by oxygen/status/enemy ticks. Ordinary actions cost one tick, difficult/low-gravity movement two, no-gravity three. UI/invalid commands consume no time/RNG. Valid refill resolves before consumption. Reserve, oxygenation and HP are separate. No free life-support toggles. Hypoxia perceptions never mutate actual world state; reliable inspection respects legitimate vision/sensors.

SquidSquad provides procedural/grid/path algorithms. Renderer/audio consume copies/events. Separate generation, combat, loot, medicine and perception random streams. A.L.I.C.E. is nonblocking and nonlethal; assistance does not replace player INT checks. Both Constructor routes must work, including an operator route for low INT.
