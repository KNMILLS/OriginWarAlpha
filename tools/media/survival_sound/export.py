"""Bake approved voices and original survival mechanisms into isolated new paths.

Clean/native ALICE processing reuses the existing reviewed deterministic recipe.
No sampled machinery, borrowed melody, franchise voice or runtime inference.
"""
from __future__ import annotations
import argparse
import hashlib
import json
import math
import subprocess
import sys
from pathlib import Path
import numpy as np
import soundfile as sf

sys.dont_write_bytecode = True
sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from export_expressive_alice import clean_voice, transmitted_voice, static_layer, TONE, TRANSMISSION
from plan import ASSETS, ROOT, build_plan

SR = 44100
TAU = 2 * np.pi


def sha(path):
    return hashlib.sha256(Path(path).read_bytes()).hexdigest()


def source_sha(path):
    return hashlib.sha256(Path(path).read_text(encoding='utf-8').encode('utf-8')).hexdigest()


def db(value):
    return 20 * math.log10(max(1e-12, float(value)))


def edge(x, attack=.006, release=.045):
    x = np.asarray(x, np.float32).copy()
    a, r = min(len(x), round(attack * SR)), min(len(x), round(release * SR))
    if a:
        x[:a] *= np.sin(np.linspace(0, np.pi / 2, a)) ** 2
    if r:
        x[-r:] *= np.cos(np.linspace(0, np.pi / 2, r)) ** 2
    return x


def normalize(x, peak=-16):
    x = np.asarray(x, np.float32).copy()
    x -= x.mean()
    x *= 10 ** (peak / 20) / max(float(np.max(np.abs(x))), 1e-9)
    return edge(x)


def noise(seconds, low, high, seed):
    n = round(seconds * SR)
    rng = np.random.default_rng(seed)
    spectrum = np.fft.rfft(rng.normal(size=n))
    f = np.fft.rfftfreq(n, 1 / SR)
    shape = (1 - np.exp(-(f / max(1, low)) ** 4)) / np.sqrt(1 + (f / high) ** 10)
    shape[0] = 0
    x = np.fft.irfft(spectrum * shape, n)
    return (x / max(float(np.std(x)), 1e-9)).astype(np.float32)


def contact(seconds, seed, weight=1, brightness=1200):
    t = np.arange(round(seconds * SR)) / SR
    x = noise(seconds, 60, brightness, seed) * np.exp(-t * 33) * .22
    for f, gain, decay in [(76, .8, 17), (143, .24, 24), (319, .10, 40)]:
        x += np.sin(TAU * f * t) * gain * np.exp(-t * decay)
    return edge(x * weight, .002, .045)


def place(dst, x, start, gain=1):
    at = round(start * SR)
    count = min(len(x), len(dst) - at)
    if count > 0:
        dst[at:at + count] += x[:count] * gain


def actuator(seconds, seed, weight=1):
    t = np.arange(round(seconds * SR)) / SR
    ramp = np.sin(np.pi * t / seconds) ** 1.4
    fundamental = 68 + 27 * np.sin(np.pi * t / seconds)
    phase = TAU * np.cumsum(fundamental) / SR
    x = (.18 * np.sin(phase) + .035 * np.sin(phase * 3)) * ramp
    x += noise(seconds, 80, 1100, seed) * ramp * .055
    place(x, contact(.16, seed + 1, .4), .01)
    place(x, contact(.22, seed + 2, weight, 800), max(0, seconds - .23))
    return x


def render_sfx():
    cues = {}
    def put(ident, x, description, peak=-16, perspective='structure-conducted'):
        cues[ident] = (normalize(x, peak), {'description': description, 'perspective': perspective,
                                         'loop': False, 'requires_authoritative_event': True})
    for ident, weight, top, peak in [('step', 1, 950, -17), ('step_careful', .45, 520, -22), ('step_heavy', 1.6, 650, -15)]:
        x = np.zeros(round(.45 * SR), np.float32)
        place(x, contact(.28, 882 + round(top), weight, top), .04)
        t = np.arange(len(x)) / SR
        x += .025 * noise(.45, 250, 1700, 8820 + top) * np.exp(-((t - .23) / .07) ** 2)
        put(ident, x, 'Boot contact and subdued pressure-suit equipment settling; ' + ident.replace('_', ' '), peak)
    put('door', actuator(1.14, 303), 'Relay pickup, strained actuator travel, soft seal settling.', -17)
    x = np.zeros(round(1.05 * SR), np.float32)
    place(x, contact(.38, 410, 1.5, 1600), .02)
    place(x, noise(.54, 110, 1900, 411) * np.sin(np.linspace(0, np.pi, round(.54 * SR))) * .12, .26)
    place(x, contact(.25, 412, .7, 900), .74)
    put('door_force', x, 'Weight applied against metal, sliding abrasion and a failed latch releasing.', -14)
    put('brace_place', np.concatenate([contact(.16, 501, .5), np.zeros(round(.075 * SR)), contact(.23, 502, .8, 750)]), 'Brace seating and over-center clamp; two weighted contacts.', -17)
    put('brace_recover', np.concatenate([contact(.12, 503, .3, 1800), np.zeros(round(.045 * SR)), contact(.18, 504, .5, 1100)]), 'Clamp release followed by brace lifting free.', -18)
    t = np.arange(round(.72 * SR)) / SR
    x = noise(.72, 130, 1900, 601) * .035 * np.sin(np.pi * t / .72) ** 2
    for at, f, strength in [(.03, 620, .17), (.21, 465, .11), (.47, 775, .08)]:
        q = np.arange(round(.065 * SR)) / SR
        place(x, np.sin(TAU * f * q) * np.exp(-q * 58), at, strength)
    put('probe', x, 'Contact probe interrogation: small electrical coupling pulses, no reward fanfare.', -21, 'helmet-equipment')
    t = np.arange(round(.88 * SR)) / SR
    x = noise(.88, 75, 1300, 701) * np.sin(np.pi * t / .88) ** 1.5 * .055
    x += .025 * np.sin(TAU * (93 * t + 12 * t * t)) * np.sin(np.pi * t / .88)
    place(x, contact(.12, 702, .4), .02)
    place(x, contact(.12, 703, .3), .69)
    put('bank_transfer', x, 'Finite connector seating, capacitor charge and release; transfer result only.', -20, 'helmet-equipment')
    put('lamp', contact(.14, 711, .2, 1900), 'Small physical lamp switch, no sound implying a world tick deadline.', -24, 'helmet-equipment')
    put('sirs_step', np.concatenate([contact(.13, 811, .35, 1900), actuator(.22, 812, .25)]), 'Precise dry contact and brief clipped servo.', -18)
    x = np.zeros(round(.69 * SR), np.float32)
    place(x, contact(.28, 820, 1.4, 560), .02)
    place(x, actuator(.30, 821, .5), .31)
    put('add_step', x, 'Heavy planted contact followed by a separate lower actuator settling.', -16)
    x = np.zeros(round(.43 * SR), np.float32)
    for i, at in enumerate([.018, .12, .285]):
        place(x, contact(.10, 830 + i, .14, 2200), at)
    put('rover_step', x, 'Uneven small traction contacts; distinct from humanoid machinery.', -20)
    x = np.zeros(round(.58 * SR), np.float32)
    place(x, contact(.12, 841, .35, 1200), .01)
    for at, frequency in [(.14, 1030), (.31, 690)]:
        q = np.arange(round(.085 * SR)) / SR
        place(x, np.sin(TAU * frequency * q) * np.exp(-q * 38), at, .15)
    put('rover_arm', x, 'One finite genuine fuse-arm identity. It is not a real-time countdown and never plays for hallucination.', -15)
    put('power_on', actuator(.88, 901, .6), 'Local relay and motor reaching speed; actual circuit changed.', -19)
    put('power_off', actuator(.74, 902, .25)[::-1].copy(), 'Local motor coasting down and relay release; actual circuit changed.', -20)
    return cues


def render_ambience():
    seconds = 24
    t = np.arange(seconds * SR) / SR
    # Periodic noise and integer-cycle modulators keep rendered loop boundaries continuous.
    fan = noise(seconds, 45, 580, 21151022)
    live = .018 * fan * (.78 + .08 * np.sin(TAU * t / 12) + .04 * np.sin(TAU * t / 8))
    live += .012 * np.sin(TAU * 97 * t) + .004 * np.sin(TAU * 194 * t)
    dead = .005 * noise(seconds, 35, 260, 21151023)
    dead += .002 * np.sin(TAU * 53 * t) * (.5 + .5 * np.sin(TAU * t / 24))
    # No offscreen creature-like transients, footsteps, breathing, alarms or musical threats.
    return {'station_powered': (live.astype(np.float32), 'Low powered ventilation and machinery; actual local circuit state.', True),
            'station_unpowered': (dead.astype(np.float32), 'Very quiet distant hull conduction; no fictional nearby actor.', True),
            'radio-static': (static_layer(), 'Original speech-free radio texture, independent of all voice masters.', True)}


def encode_ogg(path, x):
    path.parent.mkdir(parents=True, exist_ok=True)
    # No shared temporary/cache path and no runtime source sidecars.
    subprocess.run(['ffmpeg', '-nostdin', '-v', 'error', '-y', '-f', 'f32le', '-ar', str(SR), '-ac', '1',
                    '-i', 'pipe:0', '-map_metadata', '-1', '-c:a', 'libvorbis', '-q:a', '4',
                    '-fflags', '+bitexact', '-flags:a', '+bitexact', str(path)],
                   input=np.asarray(x, '<f4').tobytes(), check=True)


def officer_voice(path, clear=False):
    x, rate = sf.read(path, dtype='float32')
    assert rate == 24000 and x.ndim == 1
    n = round(len(x) * SR / rate)
    x = np.interp(np.arange(n) * rate / SR, np.arange(len(x)), x).astype(np.float32)
    if not clear:
        f = np.fft.rfftfreq(len(x), 1 / SR)
        high = 1 / np.sqrt(1 + (85 / np.maximum(f, 1)) ** 6)
        low = 1 / np.sqrt(1 + (f / 4900) ** 8)
        x = np.fft.irfft(np.fft.rfft(x) * high * low, len(x)).astype(np.float32)
    x = normalize(x, -11)
    return np.concatenate([np.zeros(round(.06 * SR), np.float32), x, np.zeros(round(.12 * SR), np.float32)])


def relative(path):
    return path.relative_to(ROOT / 'assets').as_posix()


def metrics(path, x):
    return {'path': relative(path), 'sha256': sha(path), 'duration': round(len(x) / SR, 6),
            'sample_rate': SR, 'channels': 1, 'peak_dbfs': round(db(np.abs(x).max()), 4),
            'rms_dbfs': round(db(np.sqrt(np.mean(x * x))), 4),
            'float_sha256': hashlib.sha256(x.tobytes()).hexdigest()}


def main():
    p = argparse.ArgumentParser()
    p.add_argument('--output-dir', type=Path, default=ASSETS)
    p.add_argument('--effects-only', action='store_true')
    args = p.parse_args()
    output = args.output_dir.resolve()
    assert output.is_relative_to(ASSETS.resolve()), 'Exports must remain inside the isolated survival asset namespace.'
    plan = build_plan()
    assert json.loads((ASSETS / 'source/production-plan.json').read_text(encoding='utf-8')) == plan
    manifest = {key: value for key, value in plan.items() if key != 'persona'}
    # Clarification approved during production: the damaged station transceiver
    # makes first contact degraded but usable, independent of accessibility mix.
    manifest['exchanges'][0]['required_facts'] = list(manifest['exchanges'][0]['required_facts']) + ['radio-link-degraded']
    manifest['assets_status'] = 'Offline production candidate; actual runtime and human listening acceptance separate.'
    manifest['voice_recipe'] = {'alice_clean': TONE, 'alice_transmission': TRANSMISSION,
                                'officer': 'Native retained; clean level normalization; normal close internal voice 85Hz HP/4900Hz LP, no pitch change.',
                                'radio_static': 'Separate layer, never baked into words; clear speech must select clear_path and suppress interference.'}
    manifest['sfx'] = {}
    for ident, (x, description) in render_sfx().items():
        path = output / 'sfx' / (ident + '.wav')
        path.parent.mkdir(parents=True, exist_ok=True)
        sf.write(path, x, SR, subtype='PCM_16')
        manifest['sfx'][ident] = {**description, **metrics(path, x)}
    manifest['ambience'] = {}
    for ident, (x, description, loop) in render_ambience().items():
        path = output / 'ambience' / (ident + '.ogg')
        encode_ogg(path, x)
        manifest['ambience'][ident] = {'description': description, 'loop': loop, **metrics(path, x)}
    if not args.effects_only:
        for exchange in manifest['exchanges']:
            for line in exchange['lines']:
                key = line['source_key']
                source = ASSETS / 'source/voice' / (key + '-dry.wav')
                record = json.loads(source.with_name(key + '.json').read_text(encoding='utf-8'))
                assert sha(source) == record['sha256'] and record['text_sha256'] == line['text_sha256']
                normal = transmitted_voice(source) if line['speaker'] == 'alice' else officer_voice(source)
                clear = clean_voice(source) if line['speaker'] == 'alice' else officer_voice(source, True)
                path = output / 'voice' / (key + '.ogg')
                clear_path = output / 'voice/clear' / (key + '.ogg')
                encode_ogg(path, normal)
                encode_ogg(clear_path, clear)
                line.update(metrics(path, normal))
                line['clear_path'] = relative(clear_path)
                line['clear_sha256'] = sha(clear_path)
                line['clear_duration'] = round(len(clear) / SR, 6)
                line['source_wav_sha256'] = record['sha256']
        manifest['source_plan_sha256'] = sha(ASSETS / 'source/production-plan.json')
        manifest['source_toolchain_sha256'] = sha(ASSETS / 'source/toolchain.json')
        manifest['source_hash_policy'] = 'Python source hashes use canonical UTF-8/LF; asset/model/license hashes always use exact bytes.'
        manifest['exporter_sha256'] = source_sha(__file__)
        manifest['reused_exporter_sha256'] = source_sha(ROOT / 'tools/media/export_expressive_alice.py')
        manifest['provenance'] = 'Original authored text and original deterministic machinery synthesis. General instruction-conditioned synthetic voices from previously pinned Qwen tools/model. No copied sample, reference voice, actor impersonation or runtime model.'
        (output / 'exchanges.json').write_text(json.dumps(manifest, indent=2) + '\n', encoding='utf-8', newline='\n')
    else:
        (output / 'effects-manifest.json').write_text(json.dumps({'sfx': manifest['sfx'], 'ambience': manifest['ambience']}, indent=2) + '\n', encoding='utf-8', newline='\n')
    print(json.dumps({'effects': len(manifest['sfx']), 'ambience': len(manifest['ambience']),
                      'exchanges': 0 if args.effects_only else len(manifest['exchanges'])}))


if __name__ == '__main__':
    main()
