# Audio provenance

Original M1 music: "Signal Through Steel", 96 seconds, 32 bars, 80 BPM; authored melodic/harmonic arrangement and waveform synthesis. Sixteen purposeful mono effects accompany pickups, refill, movement, doors, switches, UI and other events. No legacy/external samples were reused. See [audio manifest](audio/manifest.json) and tools/media/compose_audio.py for source, arrangement, levels and checks.

FFmpeg decoding/duration/no-clipping and PCM headers PASS. The actual Windows application loads one Music stream and 16 Sound objects; native pickup/refill actions invoke production playback. Human/audible artistic review and a full runtime loop/mix review remain OPEN. Do not equate backend loading/play calls with listening. The other five soundtrack roles and additional effects remain M5 tasks.
