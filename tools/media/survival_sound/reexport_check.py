"""Check real second exports against retained first hashes, without redoing TTS."""
from __future__ import annotations
import hashlib
import argparse
import json
import subprocess
import sys
from pathlib import Path
from plan import ASSETS


def hashes():
    return {path.relative_to(ASSETS).as_posix(): hashlib.sha256(path.read_bytes()).hexdigest()
            for path in ASSETS.rglob('*') if path.is_file() and 'source' not in path.relative_to(ASSETS).parts}


def main():
    p = argparse.ArgumentParser()
    p.add_argument('--report', type=Path, default=ASSETS / 'source/reexport-check.json')
    args = p.parse_args()
    report = args.report
    assert not report.exists(), 'Preserve first comparison; do not overwrite evidence.'
    first = hashes()
    assert 'exchanges.json' in first and len(first) == 76
    subprocess.run([sys.executable, str(Path(__file__).with_name('export.py'))], check=True)
    second = hashes()
    identical = first == second
    differences = sorted(key for key in first.keys() | second.keys() if first.get(key) != second.get(key))
    report.write_text(json.dumps({'scope': 'Actual second complete downstream exports from retained native voice masters. This does not test bit-identical GPU voice synthesis.',
                                  'status': 'PASS' if identical else 'FAIL', 'file_count': len(first),
                                  'first_sha256': first, 'second_sha256': second, 'differences': differences}, indent=2) + '\n',
                      encoding='utf-8', newline='\n')
    assert identical, differences
    print(json.dumps({'downstream_reexport': 'PASS', 'identical_files': len(first)}))


if __name__ == '__main__':
    main()
