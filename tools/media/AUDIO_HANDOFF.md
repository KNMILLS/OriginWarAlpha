# M5 audio content handoff

Task M5-AUDIO; baseline 6db098f132db2347f412effa8d73f2e98b4462db; branch codex/m5-audio. Only audio exports/source/validation and this handoff change. Art, game state, runtime event registration, shared provenance and build files are untouched.

Six composed, arranged, loopable score exports are supplied, using the same original oscillator/noise palette and D-A-C-E signal interval identity. Editable note/beat/harmony tables and orchestration live in compose_audio.py. They use no external recordings, samples, soundfonts, legacy media or audio-generation service.

| Runtime ID | Title | Duration / form | Intended contrast | Proposed relative role gain |
| --- | --- | --- | --- | --- |
| menu | The Signal Before Departure | 72s; three eight-bar phrases, 80 BPM | Clear beacon theme, interlocking clock, lower departure | 0.65 |
| exploration | Signal Through Steel | Existing 96s; four phrases, 80 BPM | Sparse station space, harmonic clock, restrained pulse | 0.48 |
| combat | Steel Against the Breach | 96s; six phrases, 120 BPM | Syncopated bass, metallic percussion, pressure/bridge/return | 0.42 |
| danger | Air Between Heartbeats | 72s; three phrases, 80 BPM | Paired low pulse, air swells, narrowing dissonance; leaves alert space | 0.50 |
| alice | A Choice Freely Made | 100s; five phrases, 96 BPM | Ethical question/answer, reflection, fuller finale development, quiet return | 0.58 |
| ending | Toward the Red Dawn | 80s; four phrases, 96 BPM | Relative-major horizon, counterline and restrained credits resolution | 0.60 |

The gains are starting proposals within the user's music bus, not applied runtime values or a loudness calibration. Crossfade score roles instead of summing complete cues. Keep short real-threat cues above music; duck music for dialogue/critical warning only through the lead-owned director. A danger score should not imitate an armed ROVER or make a false countdown.

Twenty-eight purposeful mono PCM16 effects are supplied. Existing sixteen are retained byte-for-byte. New IDs are melee, rover, explosion, death, victory, hack, breath, pulse, hallucination, error, confirm and drop. The real ROVER cue begins mechanically and accelerates distinct pitched ticks; hallucination is an airy detuned glass wash with no countdown rhythm or mechanical impact. Logical event mapping and truth/perception scheduling remain lead-owned.

Suggested SFX group gains within the user SFX bus: human steps 0.55; mechanisms 0.70; gun/melee/impact 0.80; real ROVER countdown 0.80; oxygen alerts 0.72; UI 0.55; breath/pulse/phantom 0.45. Rate-limit warnings and do not spawn persistent effects per render frame. Check multiple simultaneous sounds on the actual backend before accepting the mix.

Exploration's preserved SHA256 is b232b9d46ec7b98dd43333812068416dfab54cb8376f5eda3ad9655379b1c80e. The source refuses to overwrite that cue if reproduction differs, and compares original M1 SFX before copying. Bitexact OGG encoding and independent source RNGs are used. The repeatability check recomposes and encodes all six cues and all 28 effects a second time and compares source/output hashes in the current environment.

Rebuild and validate with Python 3/numpy and ffmpeg/ffprobe:
- python tools/media/compose_audio.py --verify-repeatability
- python tools/media/validate_audio.py

Only a reusable intermediate WAV, candidate OGG and small effect candidates are written below ignored tools/media/.render. The original M1 intermediate may also remain there. Runtime outputs, full provenance/arrangements/levels and per-file hashes are in assets/audio/manifest.json. Audio-only decoding, loudness, true/sample peak and loop continuity metrics are in assets/audio/validation.json. Boundary metrics identify listening priorities; they cannot prove an inaudible seam.

No audible artistic acceptance, reliable simultaneous mix, runtime voice/loop transition acceptance or campaign scene timing is claimed by authoring/metrics. Root integrates and inspects those. A.L.I.C.E. finale placement, final dynamics, accessibility/slider behavior and speaker/headphone balance remain runtime listening gates. Art direction/directional, attack/death animation and sector dressing are outside this task.
