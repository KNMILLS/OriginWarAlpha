"""Summarize final SH-SOUND artifacts with current, checked file identities."""
from __future__ import annotations
import hashlib
import json
from pathlib import Path
from plan import ASSETS, ROOT


def sha(path):
    return hashlib.sha256(Path(path).read_bytes()).hexdigest()


def main():
    catalog = json.loads((ASSETS / 'exchanges.json').read_text(encoding='utf-8'))
    validation = json.loads((ASSETS / 'source/validation.json').read_text(encoding='utf-8'))
    screen = json.loads((ASSETS / 'source/accepted-word-screen.json').read_text(encoding='utf-8'))
    repeat = json.loads((ASSETS / 'source/reexport-check-portable.json').read_text(encoding='utf-8'))
    assert validation['technical_status'] == repeat['status'] == 'PASS'
    files = {p.relative_to(ASSETS).as_posix(): {'sha256': sha(p), 'bytes': p.stat().st_size}
             for p in ASSETS.rglob('*') if p.is_file() and 'source' not in p.relative_to(ASSETS).parts}
    assert len(files) == 76 and {k: v['sha256'] for k, v in files.items()} == repeat['second_sha256']
    assert len(screen['records']) == 56
    for record in screen['records']:
        assert sha(ROOT / record['path']) == record['sha256']
    for relative, record in validation['decoded'].items():
        assert sha(ROOT / relative) == record['sha256']
    natives = [json.loads(path.read_text(encoding='utf-8')) for path in (ASSETS / 'source/voice').glob('*.json')]
    assert len(natives) == 28
    result = {
        'task': 'SH-SOUND', 'starting_commit': '96a1c6e',
        'scope': '12 complete two-person exchanges plus4 essential ALICE finale statements; offline production for survival slice.',
        'counts': {'exchanges': 16, 'spoken_lines': 28, 'normal_voice_files': 28, 'clear_voice_files': 28,
                   'sfx': 16, 'ambience': 3, 'runtime_audio_files': 75},
        'native_seconds': round(sum(x['seconds'] for x in natives), 6),
        'normal_voiced_seconds_with_tails': round(sum(line['duration'] for e in catalog['exchanges'] for line in e['lines']), 6),
        'runtime_bytes': sum(x['bytes'] for x in files.values()),
        'current_runtime_files': files,
        'validation_sha256': sha(ASSETS / 'source/validation.json'),
        'accepted_screen_sha256': sha(ASSETS / 'source/accepted-word-screen.json'),
        'repeatability_sha256': sha(ASSETS / 'source/reexport-check-portable.json'),
        'technical': 'PASS: exact input/subtitle bindings, pinned model/tools/licenses, native format, all export hashes/decoders/finite values,4xpeak estimates and76 actual second-export byte identities.',
        'asr': {'final_hashes_screened': 56, 'normalized_exact': screen['normalized_matches'],
                'review_flags': [{'source_key': r['source_key'], 'layer': r['layer'], 'recognized': r['recognized']}
                                 for r in screen['records'] if not r['normalized_exact']],
                'interpretation': 'Two remaining name-notation flags spell A.L.I.C.E. as A-L-I-C-E. Recognition is fallible, not proof of intelligibility or accepted performance.'},
        'prior_candidate': 'Original recovery native, first screen, first validation and first export/re-export hashes retained. Retake selected after exact native and final normal/clear screening.',
        'mix_bound': validation['mix_bound'],
        'human_listening': 'NOT_PERFORMED', 'native_runtime': 'NOT_PERFORMED', 'owner_artistic_acceptance': 'OPEN',
        'packaging': 'Ship exchanges.json,voice/**,sfx/**,ambience/**. Exclude audio/survival/source/** from runtime; retain sources in source/provenance delivery. No inference environment/model is a player dependency.',
        'integration': 'Lead owns authoritative eligibility/knowledge, scheduler, event mappings, mixing/lifecycle, settings, packaging and native verification. No shared registry, Java, model, build or old media changed by sound worker.',
        'authoring_sources': {p.relative_to(ROOT).as_posix(): sha(p) for p in Path(__file__).parent.glob('*.py')},
    }
    target = ASSETS / 'source/DELIVERY.json'
    target.write_text(json.dumps(result, indent=2) + '\n', encoding='utf-8', newline='\n')
    print(json.dumps({key: result[key] for key in ['counts', 'native_seconds', 'normal_voiced_seconds_with_tails', 'runtime_bytes', 'human_listening', 'native_runtime']}, indent=2))


if __name__ == '__main__':
    main()
