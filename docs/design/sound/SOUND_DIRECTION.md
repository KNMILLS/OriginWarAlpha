# Sound direction: a station that makes you listen

Status: preserved independent design proposal, 20 October 2026. Audited baseline `18938ec`; branch `codex/feedback-sound-direction`. This historical proposal is not audible acceptance. The owner's later occupied-suit/organ/ALICE direction now has actual replacement content and presentation code; see [ATM-SOUND handoff](ATM_SOUND_HANDOFF.md) for exact sources, integration and open listening/runtime gates. The owner's stated priority is **“player feel unsettled, feel tension.”**

The player should hear an occupied suit inside a damaged, working station. Ordinary machinery should leave room to wonder what made the next sound. Music should give that uncertainty a shape, then leave space for listening. Combat remains deliberate and turn based; the soundtrack must not imply that thinking costs oxygen or that a two-turn fuse expires after two seconds.

## Original versus candidate

The 2016 DOCX establishes the human war, station, robots, A.L.I.C.E.'s agency and sacrifice. It contains no prescribed score palette. The strongest original audio evidence is the implementation: breathing, heartbeat and background atmosphere loop together from startup; occasional level/interaction sounds interrupt them. That corroborates bodily pressure as an important part of the original experience. It does not establish how the original mix sounded to a listener.

| Element | Original evidence | Candidate at `18938ec` | Direction |
| --- | --- | --- | --- |
| Body | Continuous breath and heartbeat layers | Short synthesized breath/pulse scheduled during hypoxia | Restore a quiet sense of the suit at healthy oxygen; change effort and space as oxygenation falls. |
| Background | A 600-second track tagged *Derelict Freighter* | Six authored 72–100-second score roles, always looping when selected | Retain six roles and the original motif, but build exploration from air, motors and incomplete musical arrivals. |
| Escalation | Named breath/rapid-heart/voice assets and level triggers; not a verified continuous physiological system | Severity selects danger; separate body/impression cues | Tie severity to true oxygenation and recovery, with clear contrast and bounded event density. |
| Identity | Doors, boots, card lock, oxygen and unsettling one-shots |28 purposeful synthesized effects; enemy distinctions partly pitch changes | Give each machine a different attack/envelope/rhythm, and keep useful mechanisms tactile. |
| Emotional ending | A.L.I.C.E. chooses sacrifice and launches the player | Shared signal motif opens into a relative-major ending | Hold grief and physical aftermath before relief. Preserve her agency; avoid a reward fanfare over the loss. |

Legacy files are references only. Metadata identifies third-party sources for some; authorship/rights remain separate from the owner's creative direction. Do not copy or process those recordings into replacements. Exact source observations and research limits are in [EVIDENCE.md](EVIDENCE.md).

## Score and palette

Keep the existing D–A–C–E signal as a recognizable fragment. During exploration, use two notes or a broken answering interval more often than the complete phrase. Let fans and electrical resonances share its pitch material. Avoid an audible harmonic resolution at every eight-bar boundary. Save a clearly completed phrase for a meaningful relationship or ending moment.

The current source places four sustained notes at every bar and commonly repeats an eight-bar progression. Its 120 BPM combat pulse and regular danger pairs can organize the space more confidently than the intended unease. This is an arrangement inference, not a verdict from listening. Prototype removal and spacing before adding more instruments.

| Role | Concrete candidate brief |
| --- | --- |
| Menu / briefing | A quiet carrier signal, distant ventilation and one incomplete motif statement. Leave several seconds before an answer. West's information should become more troubling without a sinister sting on every sentence. |
| Exploration | No continuous drum pattern. In a 90-second prototype, begin with 12 seconds of only suit/station detail; allow one 6–10-second pitched statement, then 18–30 seconds without foreground melody. At least one 20-second passage should contain no new pitched attack. Air can continue; “silence” here means reduced musical activity. |
| Combat | Keep identifiable action transients. Use low mechanical pulses in uneven short groups, separated by gaps, rather than a full kit promising forward momentum throughout a tactical pause. A candidate may retain an internal 80–100 BPM grid without exposing every beat. Intensity should change with observed action/threat, never with time spent deciding. |
| Danger / hypoxia | Narrow the available space: less reassuring harmony, more breath effort, occasional unstable resonance. Do not layer a permanent siren, a musical heartbeat and a separate heartbeat at full level. Select one pulse source at a time. |
| A.L.I.C.E. / revelations | Sparse glass or reed response, close and unforced. Her ordinary conversation, Carmack's override and her final choice need different density. Thin the accompaniment under reading; do not make her sound like an enemy or a generic digital assistant. |
| Ending / credits | Let broadcast, hatch and pressure tails stand briefly before music returns. Bring back the shared fragment with a missing or delayed answer; permit warmer harmony once the pod is clear. Grief can remain alongside relief. |

Palette: filtered air with small changes of pressure; low motor harmonics audible on ordinary speakers; short irregular metal resonances; dry relay contacts; restrained unstable upper partials. Use original synthesis or new rights-cleared recordings. Avoid a constant sub-bass bed, decorative beeps in every gap, sustained piercing whine, stock monster growls, or full-band noise as a default layer. Those choices would fill the attention space this direction needs.

Developer accounts support these principles without prescribing our score: Creative Assembly describes selective music, intimate foley and mix focus in *Alien: Isolation*; Andersen's *LIMBO* slides demonstrate environmental sound moving into musical material. Our proposed timing and palette are original decisions, not techniques measured to work in Origin War. [Creative Assembly interview](https://www.audiotechnology.com/features/listen-or-die), [Andersen's slides](https://media.gdcvault.com/gdc2011/slides/Martin_Stig_andersen_audio_SoundscapeCompositionInLimbo.pdf).

## Space, cues and truthful pressure

Keep body/suit feedback centered and close. Position visible world mechanisms and enemies modestly left/right according to their location relative to the player; attenuate with distance. The current director sends every effect to pan 0 and has no listener-distance/occlusion policy. The first bounded implementation can use existing FOV plus distance. Do not invent a new AI noise mechanic or a full acoustic simulation to deliver this brief.

Any later offscreen sound needs an explicit hearing rule. A nearby sound through a doorway may suggest a direction; a far or sealed-room event should not arrive as a full-volume, exact enemy announcement. Do not derive music intensity from concealed actors and inadvertently provide radar. Incidental creaks may be ambiguous, but cannot reproduce the real armed-R.O.V.E.R. code or invent quest information.

| Cue | Identity and restraint |
| --- | --- |
| Human / S.I.R.S. / A.D.D. / R.O.V.E.R. | Boot contact plus small equipment movement / dry relay and clipped servo / lower, heavier two-stage actuator / quick uneven small-machine contacts. Distinguish envelopes and spectra; pitch alone is insufficient as the final identity test. |
| Door / hatch / lock | Start, movement and settling weight; one short refusal for a lock. Keep the successful latch distinguishable at low volume. |
| Oxygen | A finite valve opening, pressure rush and seating click. Refill must sound successful even while oxygenation is still recovering. A historical warning must not sound like a new failure. |
| Real R.O.V.E.R. fuse | One brief, distinct arming signature; subsequent changes follow actual paid fuse steps. The current 2.2-second accelerating sample is not an ETA for a turn-based fuse. Allow time to inspect/choose without a repeated fake imminent explosion. |
| UI / pickup / medicine | Quiet, short, materially related. Reduce repeated bright two-note rewards. Do not create a threatening UI sound merely to keep everything unsettling. |
| Hypoxia impression | A soft, displaced breath/metal/glass transformation without a real threat's transients, countdown or direction precision. No damage sound, fake interaction success, or voiced instruction. |

## Hypoxia and mix

| Actual state | Proposed audible behavior |
| --- | --- |
| Healthy | Quiet intermittent breathing/gear detail, with long absences of foreground score. Do not imply distress when resources are healthy. |
| Falling / warning | A single clear threshold alert; breath becomes more present and less comfortable. Pull music back instead of adding several competing alarms. |
| Critical | Less external detail, a closer body, rare ambiguous texture. Preserve actual threat/oxygen cues and visual telemetry. Severity follows saved truth, not how long the player waits in a menu. |
| Refilled / recovering | Valve cue first; persistent strain can ease as oxygenation actually rises. Stop new impressions with recovery; fade a short tail rather than cutting a sustained noise mid-sample. |

Treat this as a dramatic vocabulary, not a clinical simulation or a portrayal of a psychiatric diagnosis. The legacy filenames are not product language.

Suggested initial mix hierarchy: urgent real threat/oxygen result, player action, local mechanisms, body, atmosphere, foreground score. Reserve brief space for dialogue and critical cues. Prototype a 3–6 dB music reduction during a threshold alert or crowded threat cue, with a quick but smooth attack and a 1–2-second return; tune by listening. Side-chain control is a well-established game-mixing tool, not a reason to replace the current backend. [Vora/White session abstract](https://www.gdcvault.com/play/1017778/Interactive-Mixing-in-Dead-Space).

Do not raise everything to compete. Checked-in metrics plus default gains put the danger score about 17 dB above the breath file in RMS; their actual perceptual relationship remains unheard. That makes masking a priority for the prototype. Peaks/LUFS are diagnostics, not target emotion. Keep transient headroom, test the summed mix, and ensure ordinary speakers retain the important information.

## Tension and urgent annoyance

Tension is the player choosing to listen because something may matter. Urgency is a clear cue that a real action/state needs attention. Annoyance is a cue demanding attention repeatedly after it has told the player everything it can.

Thomas Grip describes reducing repeated scares and supporting anticipation with context; his design lessons are relevant to pacing, not authority to remove Origin War's combat or hide its true suit readings. Roughness research links rapid modulation with aversion, which supports restraint rather than “more harshness equals more atmosphere.” [Grip](https://frictionalgames.com/2019-10-9-years-9-lessons-on-horror/), [Arnal et al., 2019](https://www.nature.com/articles/s41467-019-11626-7.pdf).

Acceptance question: **Does the owner hesitate to move because they are listening, while still understanding the real threat and their next choice?** A listener lowering the volume to escape a repetitive cue, or feeling rushed by a paused turn, is a failed tension result. Produce the small comparison in [LISTENING_PLAN.md](LISTENING_PLAN.md) before committing to a whole-score replacement.
