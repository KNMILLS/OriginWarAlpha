# Survival sound and the only answering voice

SH-SOUND, 21 October 2026. The approved survival directive supersedes the earlier RPG and physical-companion material. This is original offline production for the first survival slice. Current production facts and technical checks are recorded in `assets/audio/survival/source/validation.json`; actual runtime playback and human artistic acceptance remain separate.

## Direction

The station should make the officer listen before moving. Its ordinary sound is occupied suit, worn machinery and space between events. The officer's close internal voice and ALICE's remote transmission occupy different perspectives. ALICE is a station-resident intelligence, has no physical actor, and does not operate ordinary gameplay mechanisms for the officer. No other living conversational speaker appears in this set.

The approved ALICE persona remains an adult feminine American voice, slightly brighter/higher, using dry sarcasm to cover fear. These performances retain the pinned Qwen VoiceDesign source and the existing modest timbre adjustment. The officer uses a separate general adult masculine voice, low, quiet and restrained. Neither voice is based on a reference actor or franchise character. This is synthetic performance, not a human recording. Voice continuity, sincerity, understatement and emotional effectiveness require listening.

There are twelve complete two-person exchanges and four essential ALICE finale statements. The exact approved text, factual conditions and subtitle hashes are frozen in `source/production-plan.json`. Officer speech stays observational. It does not invent the player's allegiance, affection, promises or intention to use an item. The four finale lines describe real request/refusal, transmitting evidence, chosen sacrifice and sealed pod states. Speech completion grants none of those states.

## Runtime handoff

The runtime catalog is `audio/survival/exchanges.json`. Each exchange has an ID, family, named eligibility event, required facts, expiry conditions, once-per-run policy, priority and complete ordered lines. Each line contains `speaker` (`officer` or `alice`), exact `text`, `text_sha256`, normal `path`, file `sha256`, `duration`, `clear_path`, `clear_sha256`, `clear_duration` and retained source hash. Durations are seconds. Paths are relative to the assets root.

The named facts are a contract for the integration lead, not a second simulation inside the audio code. The engine owns truthful events and knowledge; the scheduler consumes them. In particular:

- `contact` is essential, once a working link actually permits first contact. It is exempt from optional frequency limits. A poor-link statement is eligible only for the first genuinely degraded but usable link.
- Optional starts require at least 90 real seconds **and** eight paid actions since the last optional start, with at most one optional start in 180 seconds. These are ceilings, not a requirement to fill silence.
- No optional start under a known imminent threat or critical suit state. There is no backlog. Conditions are rechecked before each line and after an interruption.
- Injury is postponed until safe and is discarded after healing. Recovery requires improved real oxygenation. Careful movement must really have been quieter; forcing must really have been audible. Pack dropping must follow an actual capacity compromise.
- The heard-contact exchange requires a real coarse contact known by both speakers. It cannot identify an unseen actor. No presentation query reads concealed actor positions.
- Descent is discussed while it can still be reconsidered, before confirmation. Its line must never advise reconsideration after irreversible descent.
- Radio loss interrupts the remote voice; reconnect requires an actual previous coverage gap. The remaining lines are not queued to spill out when coverage returns.
- Persist started/completed segment history. Pause/focus freezes playback. An already completed line never starts again merely because of mute, focus or load. Discard stale optional segments rather than restarting them.
- Critical truthful effects take mix precedence. Essential text remains in the suit log even if an utterance is interrupted. No voiced line advances a turn, consumes resources, selects an action or unlocks a mechanism.

Normal officer speech is a close, gently filtered internal perspective. Normal ALICE speech uses the current 240Hz/1950Hz transmission recipe with separate interference. Clear mode selects `clear_path` and suppresses radio reception modulation/static, rather than merely setting a noise slider to zero. All native masters and clean derivatives are retained. Do not bake static into speech or use speech processing to delete words.

The separate `radio-static` bed is speech-free by construction. It belongs only to ALICE transmission windows and follows pause, completion, coverage and voice mute. It must not continue under the officer's internal response or in empty gameplay silence.

## World cues and mix

Sixteen new original short cues cover normal/careful/heavy steps, door travel/force, brace placement/recovery, contact probe, power-bank transfer, lamp switch, three machine movement identities, a real ROVER arm event and local power on/off. Their envelopes differ materially: SIRS clips and stops; ADD plants weight then settles an actuator; ROVER makes small irregular traction contacts. The ROVER arm cue is finite and never becomes a real-time fuse countdown.

The new powered and unpowered station beds contain only ventilation, electrical/motor harmonics and hull conduction. They contain no concealed footsteps, creatures, spoken fragments or fake tactical signals. Select them from actual local circuit state and audible topology. Attenuation and uncertainty are calculated before presentation; muffling does not grant knowledge through a sealed wall.

Use the existing score sparingly between mechanical listening passages. Do not automatically loop a combat role around every known enemy. Do not select intensity from unseen actors. Score is silent during speech for the conservative initial mix bound; new measured integration may support a different bounded mix after listening. Do not let suspense imply that a paused decision spends oxygen or advances a fuse.

The technical validation records decoded sample peaks and a four-times oversampled peak estimate for every export. Its conservative full-slider sum assumes the documented voice/effect/body/radio trims and a six-effect cap. It is not a guarantee under arbitrary integration gains. Recheck headroom if those assumptions change. Preserve comfortable independent speech/effects/music settings, subtitles, captions, reduced interference and reduced dynamic range.

Hypoxia may narrow the perceived space and bring the body closer. It cannot forge precise arming/damage/success signals, instruction, radio speech or ALICE's presence. Actual suit readings remain trustworthy.

## Source and reproduction

All changes are isolated under new survival asset/tool paths. Earlier releases, voice masters, score and original reference remain unchanged. No legacy recording, franchise audio, sampled machine, third-party melody or voice reference is incorporated.

Use the existing isolated Qwen authoring environment and the exact already-retained model revision `5ecdb67327fd37bb2e042aab12ff7391903235d3`. `synthesize.py` checks every model file hash, package versions, installed Qwen source hashes and existing license evidence before authoring native 24kHz PCM24 masters. It refuses to overwrite a partial or changed native candidate. Model weights, inference environment and CUDA are authoring tools only.

The source folder retains per-line text, prompt, seed, parameters, duration, native hash and generator/toolchain bindings. `export.py` reuses the existing deterministic ALICE processing functions without modifying their source or shared cache. All newly synthesized station/mechanism tables and envelopes are in that exporter. FFmpeg encodes deterministic Vorbis derivatives; short cues use 44.1kHz mono PCM16. `screen_words.py` uses an existing local small English Whisper model with no prompt or network access. Every recognition difference remains visible for review.

Run from the assigned worktree with the relevant existing isolated interpreter:

```text
tools/media/survival_sound/plan.py
tools/media/survival_sound/synthesize.py --model-dir <pinned local Qwen directory>
tools/media/survival_sound/export.py
tools/media/survival_sound/validate.py
tools/media/survival_sound/screen_words.py --model-dir <local small.en directory>
```

The native synthesis is a preserved authored candidate; deterministic downstream processing is checked separately. A seed is not a promise of bit-identical GPU inference across hardware/software revisions.

Source verification normalizes Python files to UTF-8/LF, so the inherited Windows `core.autocrlf` setting cannot invalidate equivalent source on another checkout. New Python tools explicitly retain LF. Media, native masters, model and license identities always use exact bytes.

Package the catalog, normal/clear voices, SFX and ambience as runtime assets. **Exclude `assets/audio/survival/source/**` from runtime media.** Keep that source/provenance material in the source delivery. No Python, inference weights, cached ASR/TTS tooling or synthesis dependency belongs in the player's runtime.

## Verification and listening gates

Technical scope: exact 28 input/subtitle bindings, native format, file hashes, full decoder/finite/headroom checks, original effect/bed repeatability and independent complete-output ASR screening. Recognition is fallible and cannot certify acting, tone, voice identity or actual listener comprehension. The first ASR invocation found that its dedicated environment did not include `soundfile`; the screen now uses the existing FFmpeg decoder, with no package change.

The initial complete screen recognized the first recovery acknowledgement as "God" rather than "Good" in both modes. A fresh exact-text native take with the same general persona was generated, independently screened, selected and re-exported. The first native, transcripts and export hashes remain under source evidence. The selected take screens as the intended words in normal and clear modes. `accepted-word-screen.json` verifies all 56 final voice hashes: 54 normalized exact transcripts and two name-notation differences where A.L.I.C.E. is recognized as A-L-I-C-E. Original recognition output remains intact. These are screening results, not a replacement for listening. A retake helper's relative-path error was corrected by resolving its input path before recording provenance.

Before artistic acceptance, hear the integrated 15–20-minute slice on normal headphones, ordinary stereo speakers and mono at a comfortable fixed level. Check quiet traversal, an audible movement compromise, lamp darkness, each robot signature, a resource recovery and two organically eligible exchanges. Include a 20-second tactical pause, coverage loss during speech, resume during an unfinished exchange, and a repeated 30–60-minute session. Verify speech clarity against text without using recognition output as the answer key. Note repetition, urge to lower volume, abrupt transitions and any information the listener incorrectly inferred.

The owner must hear consistent characters, physical machinery, understandable true cues, relief after recovery and purposeful silence. Generated/exported, decoded, integrated, actually played, actually heard and owner accepted are separate claims. This production is not an automatic certificate of commercial quality.
