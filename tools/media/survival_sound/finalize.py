"""Bounded SH-SOUND candidate selection and final screen assembly."""
from __future__ import annotations
import argparse
import hashlib
import json
import shutil
from pathlib import Path
from plan import ASSETS, ROOT


def sha(path):
    return hashlib.sha256(Path(path).read_bytes()).hexdigest()


def adopt():
    screen = json.loads((ASSETS / 'source/recovery-take2-native-screen.json').read_text(encoding='utf-8'))
    assert len(screen['records']) == 1 and screen['records'][0]['normalized_exact']
    selected = ASSETS / 'source/voice-take2'
    active = ASSETS / 'source/voice'
    candidate = json.loads((selected / 'recovery-alice.json').read_text(encoding='utf-8'))
    previous = json.loads((active / 'recovery-alice.json').read_text(encoding='utf-8'))
    assert candidate['text_sha256'] == previous['text_sha256']
    assert sha(selected / 'recovery-alice-dry.wav') == candidate['sha256'] == screen['records'][0]['sha256']
    assert sha(active / 'recovery-alice-dry.wav') == previous['sha256']
    rejected = ASSETS / 'source/rejected/recovery-take1'
    assert not rejected.exists()
    rejected.mkdir(parents=True)
    for name in ['recovery-alice-dry.wav', 'recovery-alice.json']:
        shutil.copy2(active / name, rejected / name)
        shutil.copy2(selected / name, active / name)
    record = {'selection': 'Recovery take2 selected after independent unprompted native recognition of exact approved wording.',
              'first_take_sha256': previous['sha256'], 'selected_sha256': candidate['sha256'],
              'text_sha256': candidate['text_sha256'], 'human_listening': 'NOT_PERFORMED',
              'first_take_issue': 'First normal and clear ASR screens interpreted opening Good as God. A comprehension ambiguity, not a proven audible error.',
              'native_screen_sha256': sha(ASSETS / 'source/recovery-take2-native-screen.json')}
    (ASSETS / 'source/recovery-selection.json').write_text(json.dumps(record, indent=2) + '\n', encoding='utf-8', newline='\n')
    print(json.dumps(record, indent=2))


def combine():
    initial_path = ASSETS / 'source/word-screen.json'
    supplemental_path = ASSETS / 'source/recovery-final-screen.json'
    initial = json.loads(initial_path.read_text(encoding='utf-8'))
    supplemental = json.loads(supplemental_path.read_text(encoding='utf-8'))
    assert len(initial['records']) == 56 and len(supplemental['records']) == 2
    replacements = {(r['source_key'], r['layer']): r for r in supplemental['records']}
    records = [replacements.get((r['source_key'], r['layer']), r) for r in initial['records']]
    assert len(records) == 56 and len({(r['source_key'], r['layer']) for r in records}) == 56
    for record in records:
        assert sha(ROOT / record['path']) == record['sha256'], 'A screen does not match the final export: ' + record['path']
    report = {'scope': initial['scope'], 'model': initial['model'], 'faster_whisper': initial['faster_whisper'],
              'assembly': 'Unchanged final hashes retain first complete screen; replaced recovery normal/clear hashes use fresh independent final screening.',
              'screen_sources': {str(p.relative_to(ASSETS)): sha(p) for p in [initial_path, supplemental_path]},
              'records': records, 'normalized_matches': sum(r['normalized_exact'] for r in records),
              'review_required': sum(not r['normalized_exact'] for r in records)}
    output = ASSETS / 'source/accepted-word-screen.json'
    assert not output.exists()
    output.write_text(json.dumps(report, indent=2) + '\n', encoding='utf-8', newline='\n')
    print(json.dumps({'screened_final_hashes': len(records), 'normalized_matches': report['normalized_matches'],
                      'review_required': report['review_required']}, indent=2))


if __name__ == '__main__':
    p = argparse.ArgumentParser()
    p.add_argument('action', choices=['adopt-recovery', 'combine-screen'])
    args = p.parse_args()
    adopt() if args.action == 'adopt-recovery' else combine()
