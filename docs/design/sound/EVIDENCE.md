# Sound evidence and research

Read-only audit of baseline `18938ec`, 20 October 2026. No playback, game launch, build, asset export, source replacement or shared registry edit was performed. Metadata was read with ffprobe; arrangements, triggers and checked-in metrics were inspected. No audible quality, recognizability, seam or mix PASS is claimed.

## Local source findings

References below use repository paths/line numbers at the audited baseline. These observations describe implementation, not a recording of the original or candidate experience.

| Evidence | Exact finding | What it supports / limit |
| --- | --- | --- |
| `docs/reference/Origin War.docx`, paragraphs 35–57 | Terran/Martian conflict, station, A.L.I.C.E.'s refusal/sacrifice, robots and Constructor routes; no sound-palette or score specification | Preserve the human/science-fiction stakes. The DOCX does not authorize a claim that a particular musical genre was original canon. Local full text inspected; 94 paragraphs. |
| `legacy/core/src/io/zipcoder/OriginWarAlpha.java:555–559` | `initSound()` loops breathing at 0.6, heartbeat at 0.7, background at 0.3 | Body and environment are structurally present together from startup. Raw gain values do not establish perceived loudness. |
| Same source:562–584 | Stair/switch cases play a voice-named file, monster or lock; level 9 plays two unsettling-named files, level 10 stops the listed loops and starts credits | Authored interruption/escalation exists. It is level based, not proof of a complete severity-sensitive audio system. |
| `legacy/core/src/io/zipcoder/Util/SoundSingleton.java:33–53,210–217` | Loads original audio paths; `allSounds` includes background/breath/heart/rapidHR/labored/voices | Several physiological/uncanny source categories exist. Loaded or listed assets are not proof all are dynamically used. |
| `tools/media/compose_audio.py:54–87,92–126,152–213` | Six role arrangements; shared D–A–C–E family; eight-bar chord cycles; four-note sustained layers every bar; combat 120 BPM with repeated pulse/percussion; danger paired kicks | Repeatability and thematic identity are strengths. Regularity, continuous pad coverage and familiar cadences are candidates for reduction to serve uncertainty. That last sentence is design inference. |
| Same source:224–258, 283–342 | ROVER 2.2 s accelerating pitched sequence; breath 2.4 s shaped noise; pulse 0.9 s paired 64 Hz body; phantom 1.9 s detuned glass/noise; purposeful mechanism/action cues | We can revise original synthesized material. These source identities have not been audibly assessed here. |
| `lwjgl3/src/main/java/io/zipcoder/originwar/desktop/AudioDirector.java:18–40,48–70` | Every score is set looping; one role crossfades over 1.5 s; short cues use group gains/cooldowns and pan 0 | No authored no-score state, per-role gain, spatial attenuation, occlusion or mix ducking in this director. None is silently claimed implemented. |
| `OriginWarApp.java:120–126,394–409` in the same desktop directory | Accepted events go to audio; role order is ending/dialogue/danger/visible alerted combat/exploration; body scheduling needs nonzero perception, PLAY, focus and no active dialogue | Ordinary exploration has no bodily loop from this scheduler. Dialogue on deck 10 overrides danger music, while body cues stop during reading. These may be deliberate; listening should decide the balance. |
| `core/src/main/java/io/zipcoder/originwar/engine/EnemyTurns.java:98,116,144,179` | Deploy/armed/movement/door events have world positions; several can originate outside player FOV | A future listener policy must decide what is audible. The present all-events, centered path is not evidence of hearing distance or occlusion. |

The current physical/musical ROVER interval is especially important: the sample's accelerating 2.2 seconds cannot accurately announce how long an indefinitely pausable two-tick fuse has left. This is a cue-language issue to address without changing turn rules.

### Legacy metadata inspected

All 23 MP3/WAV files under `legacy/core/assets` had readable ffprobe metadata. Selected anchors:

| File | Duration / format | Embedded source evidence |
| --- | --- | --- |
| `21_Derelict_Freighter.mp3` |600.032653 s, 44.1 kHz stereo MP3 | Artist `Tabletop Audio`, title `Derelict Freighter`, 2014, comment `http://tabletopaudio.com` |
| `breathing.wav` |29.879683 s, 44.1 kHz stereo PCM16 | Logic Pro encoding metadata; no inspected attribution/license grant |
| `heartbeat.mp3` |41.430200 s, 44.1 kHz stereo MP3 | No source attribution in inspected tags |
| `labored.wav` / `rapidHR.mp3` |31.498375 s / 15.027200 s, 48 kHz stereo | Editor/encoder metadata; no inspected attribution/license grant |
| `schizophrenia.mp3` |218.827755 s, 44.1 kHz stereo MP3 | Comment names a conversion service; filename is retained evidence, not recommended terminology |
| `romero.mp3` / `playerdeath.mp3` |5.328980 s / 1.149388 s, 11.025 kHz stereo MP3 | Generic title and archive.org comment; exact recording/rights not identified |
| `whatAreYouDoingHere.wav` / `monster.wav` |7.640000 s / 5.648526 s, stereo PCM16 | Names/durations alone do not establish spoken content or its effect |

No recording was heard, copied or supplied as a design asset. Metadata can identify provenance questions; it cannot establish permission. Preserve the original privately/reference-only while making original replacements.

### Current numerical mix evidence

`assets/audio/validation.json` reports the following prior export measurements; they were read, not regenerated in this task:

| Role | Duration | BPM | Integrated LUFS | Loudness range LU | True peak dBTP |
| --- | ---: | ---: | ---: | ---: | ---: |
| Menu |72 s |80 |-20.09 |0.8 |-8.00 |
| Exploration |96 s |80 |-18.80 |2.6 |-7.03 |
| Combat |96 s |120 |-18.94 |4.0 |-6.95 |
| Danger |72 s |80 |-22.75 |1.2 |-9.97 |
| A.L.I.C.E. |100 s |96 |-20.52 |2.4 |-7.96 |
| Ending |80 s |96 |-19.93 |1.1 |-7.99 |

At director defaults, score gain = 0.8 × 0.55 = 0.44 (−7.13 dB); breath/pulse gain = 0.8 × 0.7 × 0.6 = 0.336 (−9.47 dB). Danger file RMS −24.212 becomes approximately −31.34 dBFS; breath file RMS −38.82 becomes −48.29; pulse −37.987 becomes −47.46. These linear-gain calculations ignore bus summation, device response, momentary levels and hearing. They identify a 16–17 dB body-versus-danger RMS disparity to listen for, not proof of inaudibility. The actual user's saved slider values may differ.

Circular release tails and decoded seam diagnostics are implemented. All six prior loop flags are unflagged; this does not establish an inaudible boundary. Combat and danger have about 5.8/5.2 dB first/last 20 ms RMS differences; ending has the largest decoded sample discontinuity (0.010012). Prioritize these on loop listening, without treating the numbers themselves as defects.

## Primary research actually read

These are design references. No soundtrack, recording, image, code, or licensed asset from them is being incorporated. Proposed timings/gains in SOUND_DIRECTION are our hypotheses, not findings from these sources.

| Primary material | What was read and learned | Application and limit |
| --- | --- | --- |
| [Creative Assembly audio team, *Listen or Die*](https://www.audiotechnology.com/features/listen-or-die) | Direct interviews with Byron Bullock, James Magee, Haydn Payne and colleagues: selective music, intimate foley, threat-aware mix focus, and caution that proximity-driven music can give away position | Contrast and believable local detail are useful references. Their acoustic networks/middleware and enemy hearing are not requirements for our top-down turn-based game. Article text read; audio not listened to. |
| [Martin Stig Andersen, *The Environment is the Orchestra: Soundscape Composition in LIMBO*, GDC 2011](https://media.gdcvault.com/gdc2011/slides/Martin_Stig_andersen_audio_SoundscapeCompositionInLimbo.pdf) | Slides especially 24–28 and 37–49 show synthetic/harmonized environmental sound and foundry material transforming toward musical abstraction | Start from a recognizable mechanical source and let it become ambiguous. PDF text reviewed; examples were not heard and the talk was not watched. [Session identity](https://www.gdcvault.com/play/1014797/). |
| [Sean Vora/J.White, *Interactive Mixing in Dead Space 3*, GDC 2013](https://www.gdcvault.com/play/1017778/Interactive-Mixing-in-Dead-Space) | Public session abstract explicitly identifies runtime mix control and side-chain ducking | Supports a modest mix-priority prototype. Abstract only; no claim about specific settings or the full presentation. |
| [Thomas Grip, *9 Years, 9 Lessons on Horror*, 2019](https://frictionalgames.com/2019-10-9-years-9-lessons-on-horror/) | Lessons 1, 3, 5, 7: repeated scares can become annoying; spacing/context/anticipation and uncertainty can matter more | Apply to audio event density and mood. His discussion of removing combat and hiding systems is not adopted; our RPG combat and true oxygen UI remain authoritative. |
| [Arnal et al., *The rough sound of salience enhances aversion through neural synchronisation*, Nature Communications 10:3671, 2019](https://www.nature.com/articles/s41467-019-11626-7.pdf) | Original paper, abstract/results: fast amplitude-modulation roughness 30–150 Hz relates to aversion; substantial listener variation; experiments used short click trains, not this game | Restraint with continuous rough textures is justified. This does not prescribe an atmosphere recipe, safe listening level, or prove tension/immersion. Modulation rate is not a recommendation to add a 30–150 Hz bass oscillator. DOI `10.1038/s41467-019-11626-7`. |

The Sound Architect interview URL timed out; it was not used for factual claims. No research source establishes that our candidate currently sounds good or bad. The owner's listening comparison in LISTENING_PLAN is the next acceptance gate.
