"""Verify bindings, files, isolated outputs and reproducible survival exports."""
from __future__ import annotations
import argparse
import hashlib
import json
import math
from pathlib import Path
import numpy as np
import soundfile as sf
from scipy.signal import resample_poly
from plan import ASSETS, ROOT, build_plan, text_hash


def sha(path):
    return hashlib.sha256(Path(path).read_bytes()).hexdigest()


def source_sha(path):
    return hashlib.sha256(Path(path).read_text(encoding='utf-8').encode('utf-8')).hexdigest()


def main():
    p = argparse.ArgumentParser()
    p.add_argument('--report', type=Path, default=ASSETS / 'source/validation.json')
    args = p.parse_args()
    manifest = json.loads((ASSETS / 'exchanges.json').read_text(encoding='utf-8'))
    approved = build_plan()
    assert len(manifest['exchanges']) == 16
    assert sha(ASSETS / 'source/production-plan.json') == manifest['source_plan_sha256']
    assert sha(ASSETS / 'source/toolchain.json') == manifest['source_toolchain_sha256']
    assert source_sha(ROOT / 'tools/media/survival_sound/export.py') == manifest['exporter_sha256']
    assert source_sha(ROOT / 'tools/media/export_expressive_alice.py') == manifest['reused_exporter_sha256']
    assert manifest['global_optional_start_policy'] == approved['global_optional_start_policy']
    check_files = {}
    native_files = {}
    ids = set()
    for e, a in zip(manifest['exchanges'], approved['exchanges']):
        assert e['id'] == a['id'] and e['id'] not in ids
        ids.add(e['id'])
        for name in ['family', 'essential', 'eligibility', 'required_facts', 'expiry_conditions', 'once_per_run', 'priority']:
            expected_value = a[name]
            if e['id'] == 'contact' and name == 'required_facts':
                expected_value = list(expected_value) + ['radio-link-degraded']
            assert e[name] == expected_value, (e['id'], name)
        for line, expected in zip(e['lines'], a['lines']):
            assert line['speaker'] == expected['speaker'] and line['text'] == expected['text']
            assert line['text_sha256'] == text_hash(expected['text'])
            for path_key, hash_key in [('path', 'sha256'), ('clear_path', 'clear_sha256')]:
                path = ROOT / 'assets' / line[path_key]
                assert path.resolve().is_relative_to(ASSETS.resolve()), 'Export escaped survival asset namespace.'
                assert sha(path) == line[hash_key], path
                check_files[path] = ('voice', line['clear_duration'] if path_key == 'clear_path' else line['duration'])
            source = ASSETS / 'source/voice' / (line['source_key'] + '-dry.wav')
            assert sha(source) == line['source_wav_sha256']
            x, rate = sf.read(source, dtype='float32')
            assert rate == 24000 and x.ndim == 1 and np.isfinite(x).all() and np.abs(x).max() < 1
            native_files[source.name] = {'sha256': sha(source), 'seconds': len(x) / rate, 'sample_rate': rate,
                                         'subtype': sf.info(source).subtype}
            assert native_files[source.name]['subtype'] == 'PCM_24'
    assert len(native_files) == 28 and len(check_files) == 56
    for section in ['sfx', 'ambience']:
        for ident, entry in manifest[section].items():
            path = ROOT / 'assets' / entry['path']
            assert path.resolve().is_relative_to(ASSETS.resolve())
            assert sha(path) == entry['sha256']
            check_files[path] = (section, entry['duration'])
    decoded = {}
    for path, (kind, duration) in check_files.items():
        x, rate = sf.read(path, dtype='float32')
        assert x.ndim == 1 and rate == 44100 and np.isfinite(x).all(), path
        assert abs(len(x) / rate - duration) < .01, path
        peak = float(np.max(np.abs(x)))
        true_peak = float(np.max(np.abs(resample_poly(x, 4, 1))))
        assert peak < .36 and true_peak < .38, path
        assert float(np.max(np.abs(x))) > .0005, 'Unexpected silence: ' + str(path)
        decoded[path.relative_to(ROOT).as_posix()] = {
            'sha256': sha(path), 'seconds': len(x) / rate, 'sample_rate': rate, 'channels': 1,
            'sample_peak_dbfs': 20 * math.log10(max(peak, 1e-12)),
            'estimated_4x_true_peak_dbfs': 20 * math.log10(max(true_peak, 1e-12)),
            'finite': True, 'kind': kind,
        }
    # Check pure source render repeatability independently of file decoding.
    from export import render_sfx, render_ambience
    repeatable = {}
    for renderer in [render_sfx, render_ambience]:
        first, second = renderer(), renderer()
        assert first.keys() == second.keys()
        for key in first:
            assert first[key][0].tobytes() == second[key][0].tobytes(), key
            repeatable[key] = hashlib.sha256(first[key][0].tobytes()).hexdigest()
    # Conservative sum, assuming the documented integration trims and six-effect cap.
    worst_voice = max(10 ** (v['estimated_4x_true_peak_dbfs'] / 20) for v in decoded.values() if v['kind'] == 'voice')
    worst_effect = max(10 ** (v['estimated_4x_true_peak_dbfs'] / 20) for v in decoded.values() if v['kind'] == 'sfx')
    estimate = worst_voice * .86 + worst_effect * .65 * math.sqrt(6) + .3163 * .90 * .60 + .2239 * .61 * .60 + .06 * 1.75 + .035
    assert estimate < 1, 'Documented full-slider mixed headroom failed.'
    result = {'task': 'SH-SOUND', 'technical_status': 'PASS', 'exchanges': 16, 'native_performances': 28,
              'decoded_exports': len(decoded), 'native_sources': native_files,
              'decoded': decoded, 'sfx_ambience_float_repeatability': repeatable,
              'mix_bound': {'estimated_linear_sum': estimate,
                            'conditions': 'Master <=1; voice trim .86; maximum six SFX shared .65/sqrt(N); voice ducks existing suit to .60; breath/heart old maximum gains .90/.61; radio bounded .06*1.75; ambience <=.035 peak; score silent during speech. Integration must retain/recheck these assumptions.'},
              'not_proven': ['Native runtime playback', 'Human audibility, acting, identity consistency, fatigue, emotion or artistic acceptance', 'Exact heard words from text hashes', 'Scene headroom if integration changes the documented trims']}
    args.report.parent.mkdir(parents=True, exist_ok=True)
    args.report.write_text(json.dumps(result, indent=2) + '\n', encoding='utf-8', newline='\n')
    print(json.dumps({key: result[key] for key in ['technical_status', 'exchanges', 'native_performances', 'decoded_exports', 'mix_bound']}, indent=2))


if __name__ == '__main__':
    main()
