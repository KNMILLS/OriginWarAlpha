"""Offline, unprompted ASR screening; never a certificate of acting or heard words."""
from __future__ import annotations
import os
os.environ.setdefault('HF_HUB_OFFLINE', '1')
import argparse
import difflib
import hashlib
import importlib.metadata
import json
import re
import subprocess
from pathlib import Path
import numpy as np
from faster_whisper import WhisperModel
from plan import ASSETS, ROOT, build_plan


def sha(path):
    digest = hashlib.sha256()
    with Path(path).open('rb') as f:
        for chunk in iter(lambda: f.read(8 * 1024 * 1024), b''):
            digest.update(chunk)
    return digest.hexdigest()


def words(text):
    text = text.replace('A.L.I.C.E.', 'ALICE').replace('’', "'")
    text = re.sub(r"(?<=\w)'(?=\w)", '', text)
    return re.findall(r'[a-z0-9]+', text.lower())


def main():
    p = argparse.ArgumentParser()
    p.add_argument('--model-dir', type=Path, required=True)
    p.add_argument('--layers', nargs='+', choices=['normal', 'clear', 'native'], default=['normal', 'clear'])
    p.add_argument('--keys', nargs='+')
    p.add_argument('--native-dir', type=Path, default=ASSETS / 'source/voice')
    p.add_argument('--output', type=Path, default=ASSETS / 'source/word-screen.json')
    args = p.parse_args()
    args.native_dir = args.native_dir.resolve()
    assert not args.output.exists(), 'Preserve first screens; use a fresh output for additional evidence.'
    model = WhisperModel(str(args.model_dir), device='cpu', compute_type='int8', cpu_threads=8, local_files_only=True)
    entries = []
    lines = [line for e in build_plan()['exchanges'] for line in e['lines']]
    for line in lines:
        key = line['source_key']
        if args.keys and key not in args.keys:
            continue
        for layer in args.layers:
            path = args.native_dir / (key + '-dry.wav') if layer == 'native' else ASSETS / (
                ('voice/clear/' if layer == 'clear' else 'voice/') + key + '.ogg')
            raw = subprocess.run(['ffmpeg', '-nostdin', '-v', 'error', '-i', str(path), '-ac', '1',
                                  '-ar', '16000', '-f', 'f32le', '-'], capture_output=True, check=True).stdout
            x = np.frombuffer(raw, dtype='<f4').copy()
            segments, info = model.transcribe(x, language='en', beam_size=5, condition_on_previous_text=False,
                                              vad_filter=False, word_timestamps=True, initial_prompt=None)
            segments = list(segments)
            actual = ' '.join(segment.text.strip() for segment in segments)
            expected_words, actual_words = words(line['text']), words(actual)
            exact = expected_words == actual_words
            diff = [dict(operation=op, expected=expected_words[i:j], recognized=actual_words[k:l])
                    for op, i, j, k, l in difflib.SequenceMatcher(None, expected_words, actual_words).get_opcodes() if op != 'equal']
            record = {'source_key': key, 'layer': layer, 'path': str(path.relative_to(ROOT)).replace('\\', '/'),
                      'sha256': sha(path), 'expected': line['text'], 'recognized': actual,
                      'normalized_exact': exact, 'differences': diff,
                      'segments': [{'start': s.start, 'end': s.end, 'text': s.text,
                                    'avg_logprob': s.avg_logprob, 'no_speech_prob': s.no_speech_prob} for s in segments]}
            entries.append(record)
            print(key + ' [' + layer + '] ' + ('MATCH ' if exact else 'REVIEW ') + actual, flush=True)
    result = {'scope': 'Complete unprompted offline ASR screen; fallible transcription does not prove exact audible words, speaker continuity, emotion or listener comprehension.',
              'model': {'local_directory_name': args.model_dir.name, 'files': {path.name: sha(path) for path in args.model_dir.iterdir() if path.is_file()}},
              'faster_whisper': importlib.metadata.version('faster-whisper'),
              'records': entries, 'normalized_matches': sum(x['normalized_exact'] for x in entries),
              'review_required': sum(not x['normalized_exact'] for x in entries)}
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps(result, indent=2) + '\n', encoding='utf-8', newline='\n')
    print(json.dumps({'screened': len(entries), 'matches': result['normalized_matches'], 'review': result['review_required']}))


if __name__ == '__main__':
    main()
