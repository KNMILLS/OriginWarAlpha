"""Offline native SH-SOUND voice generation using the existing pinned pipeline."""
from __future__ import annotations
import os
os.environ.setdefault('HF_HUB_OFFLINE', '1')
os.environ.setdefault('TOKENIZERS_PARALLELISM', 'false')
import argparse
import hashlib
import importlib.metadata
import json
import random
import sys
import time
from pathlib import Path
import numpy as np
import soundfile as sf
import torch
from qwen_tts import Qwen3TTSModel
from plan import ASSETS, ROOT, build_plan, text_hash


def sha(path):
    digest = hashlib.sha256()
    with Path(path).open('rb') as f:
        for chunk in iter(lambda: f.read(8 * 1024 * 1024), b''):
            digest.update(chunk)
    return digest.hexdigest()


def main():
    p = argparse.ArgumentParser()
    p.add_argument('--model-dir', type=Path, required=True)
    p.add_argument('--output-dir', type=Path, default=ASSETS / 'source/voice')
    p.add_argument('--keys', nargs='+')
    p.add_argument('--take', type=int, default=0)
    args = p.parse_args()
    args.output_dir = args.output_dir.resolve()
    assert args.output_dir.is_relative_to((ASSETS / 'source').resolve()), 'Native sources must remain in survival/source.'
    plan = build_plan()
    plan_path = ASSETS / 'source/production-plan.json'
    assert json.loads(plan_path.read_text(encoding='utf-8')) == plan
    toolchain_path = ROOT / 'assets/audio/voice/source/expressive/toolchain.json'
    toolchain = json.loads(toolchain_path.read_text(encoding='utf-8'))
    assert toolchain['model']['revision'] == plan['model']['revision']
    print('VERIFYING PINNED MODEL', flush=True)
    for entry in toolchain['model']['files']:
        path = args.model_dir / entry['file']
        assert path.stat().st_size == entry['bytes'] and sha(path) == entry['sha256'], entry['file']
    actual_versions = {name: importlib.metadata.version(name) for name in ['qwen-tts', 'torch', 'transformers', 'accelerate', 'numpy', 'soundfile']}
    assert actual_versions['qwen-tts'] == toolchain['qwen_tts_version']
    assert actual_versions['torch'] == toolchain['torch']
    for name in ['transformers', 'accelerate']:
        assert actual_versions[name] == toolchain[name]
    import qwen_tts
    qwen_root = Path(qwen_tts.__file__).parent.parent
    for relative, expected in toolchain['qwen_installed_source_sha256'].items():
        assert sha(qwen_root / relative) == expected, relative
    for relative, entry in toolchain['license_evidence'].items():
        assert sha(ROOT / relative) == entry['sha256'], relative
    args.output_dir.mkdir(parents=True, exist_ok=True)
    provenance = {'model': toolchain['model'], 'actual_versions': actual_versions,
                  'prior_toolchain': str(toolchain_path.relative_to(ROOT)).replace('\\', '/'),
                  'prior_toolchain_sha256': sha(toolchain_path),
                  'checked_model_files': True, 'checked_qwen_source_files': True,
                  'checked_license_files': toolchain['license_evidence'],
                  'authoring_only': 'No model, Python, CUDA, runtime TTS, or network inference required by game.',
                  'speaker_policy': 'General synthetic adult voices; no actor/franchise reference, impersonation or external voice recording.'}
    (ASSETS / 'source/toolchain.json').write_text(json.dumps(provenance, indent=2) + '\n', encoding='utf-8', newline='\n')
    lines = {line['source_key']: line for exchange in plan['exchanges'] for line in exchange['lines']}
    keys = args.keys or list(lines)
    assert len(set(keys)) == len(keys) and all(k in lines for k in keys)
    torch.set_num_threads(8)
    model = Qwen3TTSModel.from_pretrained(str(args.model_dir), device_map='cuda:0', dtype=torch.bfloat16,
                                        attn_implementation='sdpa', local_files_only=True)
    print('MODEL READY', flush=True)
    for key in keys:
        path = args.output_dir / (key + '-dry.wav')
        metadata = args.output_dir / (key + '.json')
        if path.exists() and metadata.exists():
            old = json.loads(metadata.read_text(encoding='utf-8'))
            assert old['text_sha256'] == lines[key]['text_sha256'] and sha(path) == old['sha256']
            print('VERIFIED EXISTING ' + key, flush=True)
            continue
        assert not path.exists() and not metadata.exists(), 'Partial candidate requires explicit diagnosis: ' + key
        line = lines[key]
        assert text_hash(line['text']) == line['text_sha256']
        speaker = line['speaker']
        assert 0 <= args.take <= 20
        seed = (2115 if speaker == 'alice' else 21151021) + args.take
        instruction = plan['persona'][speaker]
        if key == 'recovery-alice' and args.take > 0:
            instruction += ' Her first word is a soft approving acknowledgement, followed by dry relief.'
        if key in ['refusal-alice', 'broadcast-alice', 'sacrifice-alice', 'farewell-alice']:
            instruction = ('An adult woman with a cool low American voice. Clear precise diction. '
                           'She is deeply frightened and trying to remain composed. Her dry irony gives way to sincere quiet resolve. '
                           'Restrained, intimate, deliberate speech; no shouting or melodrama.')
        random.seed(seed)
        np.random.seed(seed)
        torch.manual_seed(seed)
        torch.cuda.manual_seed_all(seed)
        kwargs = {'non_streaming_mode': True, 'max_new_tokens': 512, 'do_sample': True,
                  'temperature': .9, 'top_p': 1., 'top_k': 50, 'repetition_penalty': 1.05,
                  'subtalker_dosample': True, 'subtalker_temperature': .9, 'subtalker_top_k': 50,
                  'subtalker_top_p': 1.}
        print('START ' + key, flush=True)
        start = time.monotonic()
        wavs, rate = model.generate_voice_design(text=line['text'], language='English', instruct=instruction, **kwargs)
        x = np.asarray(wavs[0])
        finite = bool(np.isfinite(x).all())
        seconds = len(x) / rate
        peak = float(np.abs(x).max())
        if not finite:
            np.save(args.output_dir / (key + '-invalid-native.npy'), x)
        sf.write(path, x, rate, subtype='PCM_24')
        record = {**line, 'engine': 'Qwen3-TTS-VoiceDesign', 'model_revision': plan['model']['revision'],
                  'instruct': instruction, 'seed': seed, 'take': args.take, 'language': 'English', 'generation': kwargs,
                  'source_wav': path.name, 'source_rate': rate, 'source_subtype': 'PCM_24',
                  'seconds': seconds, 'finite': finite, 'peak_linear': peak, 'sha256': sha(path),
                  'elapsed_seconds': time.monotonic() - start, 'versions': actual_versions,
                  'plan_sha256': sha(plan_path), 'generator_sha256': sha(__file__),
                  'quality_scope': 'Generated candidate. Exact input/hash/header is not proof of output words, voice consistency or acting quality.'}
        metadata.write_text(json.dumps(record, indent=2) + '\n', encoding='utf-8', newline='\n')
        print(json.dumps({k: record[k] for k in ['source_key', 'seconds', 'finite', 'peak_linear', 'elapsed_seconds']}), flush=True)
        assert rate == 24000 and x.ndim == 1 and finite and .6 < seconds < 30 and peak < 1, 'Native candidate rejected; preserve evidence.'


if __name__ == '__main__':
    main()
