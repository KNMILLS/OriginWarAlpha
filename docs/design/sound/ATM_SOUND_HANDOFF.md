# ATM-SOUND implementation handoff

Base: `374e27c764d55adaae7a9a3b4003a5ff883de157`. The owner's new direction is the controlling production brief: an occupied suit in vacuum, close heavy breathing/heartbeat, little external sound, a muffled helmet/big-glass perspective, restrained original cosmic organ music, and a dry robotic female A.L.I.C.E. speaking through radio static with fear. No copied score, actor impersonation, ghost voices, new story, or simulation changes.

This is actual replacement content and presentation code. It is **not audible, emotional, owner, or native-runtime acceptance**. The previous library/source remains recoverable at the base commit. Prior source/rights observations in EVIDENCE.md remain scoped; the owner's original authorship and intentional tension are respected, and no legacy recording was copied into this replacement.

## Content and sources

| Group | Actual production |
| --- | --- |
| Six score roles | menu72s, exploration96s, combat96s, danger72s, alice100s, ending80s. Authored additive pipe-organ stops, slow suspended harmonies, incomplete D–A–C–E signal and gaps for listening. No imported melody, score, sample, soundfont or musical performance. Ending leaves physical aftermath before music. |
| Suit | Six12s mono streaming loops: breath and heartbeat at healthy/warning/critical states. Quiet steady body at healthy oxygen, increased rate/presence from true oxygenation, centered rather than positioned in the station. Independent from the music slider. |
| Short cues |34 original mono PCM16 effects; all prior28 replaced. Six additional timbral identities: sirs_step, add_step, rover_step, sirs_gun, add_gun, rover_tick. Filtered contacts/actuators carry through the hull; no outside cinematic gun crack. rover_tick is an exported spare, **not scheduled** because core emits no per-step fuse event. |
| A.L.I.C.E. | Nine genuine synthesized spoken clips,103.129796s total: alice-meet, alice-record, alice-agency, refusal, sacrifice-plan, pod-ready, and three actual alice-talk states. Only existing quoted A.L.I.C.E. speech is spoken; narrator paragraphs and other speakers remain silent. Full authoritative text hashes prevent stale or mismatched dialogue. Longest17.815329s. |

Editable synthesis lives in tools/media/compose_helmet_audio.py, called by compose_audio.py. The older functions remain as historical authoring reference; the entry point renders the new library. prepare_alice_voice.py fetches exact hash-locked tools/model into ignored tools/media/.cache, render_alice_voice.py preserves dry source WAVs and applies deterministic dry radio filtering, slight mechanical sideband/tremor, soft saturation and quiet static. validate_audio.py delegates to validate_helmet_audio.py for the new contracts.

The voice is a published general female TTS model, not a requested impersonation of an actor or fictional character. Training recordings are credited to Linda Johnson/Keith Ito (LJ Speech); voice model work is credited to Bryce Beattie; original Piper software to Michael Hansen. This should never be described as a human performance, wholly novel vocal identity, or an emotional speech model. Fear is an authored processing/pacing target, stronger in refusal/sacrifice/pod-ready, with actual dramatic effectiveness still unreviewed.

## Runtime integration contract

Existing select/update/event/play/volumes/pause/close methods remain. Root owns app, settings, build and license registration.

```java
audio.context(state.oxygenation, suitPresent, state.playerX, state.playerY);
// Active PLAY dialogue with exact speaker A.L.I.C.E.; hold binding during pause/focus loss.
audio.dialogue(state.activeDialogue, Campaign.dialogue(state).text());
audio.dialogue("", ""); // interrupt/clear when leaving that dialogue
audio.update(delta);
audio.event(event, legitimatelyHeard); // root gates with true visibility/local facts
```

Keep suit presence during inventory/journal/dialogue/readings. Title, credits, death/victory and absent focus do not have an occupied live suit. Explicit pause/focus uses pause(true), which pauses music/body/speech and clears one-shots; hold the dialogue binding during that pause so resume retains speech position. Clear only for an actual transition away from the ALICE dialogue. A completed clip never restarts after a volume/focus change. Identical render calls never restart it. A changed/unknown text interrupts prior speech and stays silent; text/subtitles stay authoritative. Mute pauses streams and suppresses new one-shot allocation. All objects are disposed on close.

Remove the old app breath/pulse timers. Compatibility play("breath"/"pulse") deliberately does nothing, preventing duplicated physiology. A quiet nonvoiced hypoxia impression may still call hallucination; it contains no ghost, instruction, damage code, threat transients or ROVER signature.

The old app forwarded every engine event. Root must pass true only for a local event or an event in current true visible[][], unless it implements an explicit legitimate hearing rule. False is discarded before gain, pan or ducking. The one-argument compatibility event method is local-only. Update listener context after obtaining the new snapshot before forwarding its events. No hidden actor drives this director. ROVER arming plays a single finite450ms mechanical signature only when the engine emits the actual arming event. Waiting in real time cannot generate a fuse countdown. No world/RNG/save mutation is possible through these primitive APIs.

Required resource patterns:

```text
audio/music/*.ogg                 # 6
audio/sfx/*.wav                   # 34
audio/ambience/*.ogg              # 6
audio/voice/*.ogg                 # 9
audio/voice/index.json            # text hash bindings, durations, output hashes
audio/runtime.json               # effect durations only
```

Do not include voice/source, the114MB ONNX, Piper/tool binaries, cache, the full authoring manifest, or validation report as runtime media. Those small authoring/provenance materials belong in the game source/evidence; large reproducible tooling remains private cache. Voice index: `{version:1, clips:[{dialogue_id,text_sha256,path,sha256,duration_seconds}]}`. Manifest music/sfx/ambience maps each contain path/SHA/duration/channels/rate/source levels. Source license copies and locked download/evidence hashes are in assets/audio/voice/source/toolchain.json. Root must register the chosen evidence/notices with its own packaging policy, rather than treating tools as bundled runtime dependencies.

## Mix and boundedness

| Layer | Actual starting gain, before sliders |
| --- | --- |
| All output | .70 reserve for summed headroom |
| Score | .68 music; smooth reduction to.40 of that gain during speech/important cues, quick120ms attack and1.2s return |
| Breath / heartbeat | .70/.36 SFX healthy, up to.90/.61 at critical; another.60 while speech is actually playing |
| Speech | .86 SFX, centered, short dry carrier, no long environmental reverb |
| One-shots | policy gain up to.82; common.65/sqrt(active count) bus; at most six active, priority eviction, expiry from exact exported duration |
| Outside event | additional.42/(1+.15×Manhattan distance), pan limited to±.35, true visibility/hearing gate required |

The grouping deliberately leaves score below body/voice rather than raising every layer. Real oxygen/armed threats get priority; UI is short and quiet. Sliders remain independent, with body/voice tied to SFX. This is a starting mix, not a substitute for headphones/speaker listening. At most two score streams, four brief body crossfade streams, one speech stream and six one-shots play concurrently (13 sources), avoiding the earlier16-source exhaustion pattern. Old body pairs and interrupted voices are stopped/disposed; no growing playlist or event queue exists.

## Primary-source research and rights

- [Pinned model card](https://huggingface.co/rhasspy/piper-voices/blob/5041a7a49ae5b3a0f30e6837769eb03fdbb231bd/en/en_US/ljspeech/high/MODEL_CARD): female single-speaker LJ Speech, from scratch, public-domain data. Its training-quality/epoch wording differs from the author's high-model description; these are source metadata, not independently verified training history.
- [Model author's page](https://brycebeattie.com/files/tts/): LJSpeech high is explicitly labeled public domain. [LJ Speech's primary page](https://keithito.com/LJ-Speech-Dataset/) places its text, audio and annotations in the public domain. This selected model avoids lessac-derived training-license ambiguity; no blanket assertion about other Piper voices is made.
- [Pinned voice repository metadata](https://huggingface.co/rhasspy/piper-voices/blob/5041a7a49ae5b3a0f30e6837769eb03fdbb231bd/README.md) declares MIT. [Original Piper2023.11.14-2](https://github.com/rhasspy/piper/releases/tag/2023.11.14-2) is the MIT tool version, not the later GPL fork. It is used offline and is not bundled in the game. Model SHA256 starts5d4f08ba; exact full download hashes and license excerpts/copies are retained. These selected source terms impose no additional generated-audio restriction.
- [libGDX Music](https://libgdx.com/wiki/audio/streaming-music) supports streamed OGG, pause/resume/looping/disposal; [Sound](https://libgdx.com/wiki/audio/sound-effects) exposes per-instance handles/pan/stop. That existing backend is sufficient; no replacement backend is added.
- [Tommy Ettinger digital](https://github.com/tommyettinger/digital) was checked as relevant ecosystem math infrastructure. It supplies numerical utilities rather than a licensed voice or production sound library; importing another runtime synthesis dependency would not improve this offline authored pipeline. No code/assets from it are copied here. Existing SquidSquad runtime dependencies are untouched.

## Actual validation and open review

Five new fake-backend/pure-policy tests passed after compilation: true-state body levels and return from absent suit; hidden-event silence and no wall-clock fuse advancement; all nine real Campaign bindings and changed-text rejection; speech repeat/pause/mute/interruption/completion/disposal; bounded SFX/priority/muted allocation. These are presentation/backend-call tests, not actual device listening.

All55 audio files fully decode, have expected channels/rates/durations and unclipped finite samples. Six independent music rerenders, six body rerenders and all34 SFX source rerenders matched. All nine deterministic Piper second WAV renders matched byte-for-byte; independently reprocessing retained WAVs reproduces the final OGG exports exactly. The initial Piper noise.40/.55 attempt was nondeterministic; both first WAVs and a short receipt remain in voice/source/first-attempt. Corrected noise0/0 is deliberate and reproducible. No failed attempt is described as listening evidence.

assets/audio/validation.json contains exact decoded peaks/LUFS, loop-boundary metrics, source/licensing hashes and a conservative sum-of-peak upper bound for all sliders at1. Body heartbeat boundaries were moved into interbeat silence after the first diagnostic flagged their on-beat file edges. Metrics cannot prove an audible seam, pronunciation, emotional tension or comfort over a whole campaign.

Root acceptance still needs actual resource load/disposal/voice completion, loop/crossfade and simultaneous-event stress, real threshold/recovery/read-menu/focus/mute transitions, truthful ROVER timing/visibility, and listening to all nine speech variants. Owner should compare a healthy suit passage, warning/recovery, a real threat, and ALICE's refusal/finale. No audible or owner approval is claimed by this handoff.
