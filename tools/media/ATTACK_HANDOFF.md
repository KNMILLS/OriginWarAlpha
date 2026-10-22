# M5 directed attacks

Prepared on `codex/m5-directed-attacks` from `e190c36d48655c90cb70ba9ebef3982d0f1efaed`. This delivery only adds `assets/art/animation/directional-actions/**`, `tools/media/pack_attacks.py` and this handoff. Existing art, audio, registries, renderer and shared documentation remain read-only.

## Runtime layout

`assets/art/animation/directional-actions/attack.png` is a transparent RGBA atlas, 1024 × 384 pixels, exported by exact nearest-neighbor 4× scaling from `attack-native.png` (256 × 96). Each native cell is 32 × 32. There are eight columns and three rows:

| Row | Actor | Columns 0–1 | Columns 2–3 | Columns 4–5 | Columns 6–7 |
| --- | --- | --- | --- | --- | --- |
| 0 | PLAYER | North | East | South | West |
| 1 | SIRS | North | East | South | West |
| 2 | ADD | North | East | South | West |

Every pair contains a braced ready pose and a firing/recoil pose. North, east and west are actual newly illustrated directions, not rotated or relabeled front poses. The six south cells retain exact native RGBA pixels from existing `actors-native.png` slots 40/41 (PLAYER), 44/45 (SIRS), and 48/49 (ADD).

## Source and export

The built-in OpenAI image generator created original illustrations using only the project's selected original actor references. No external asset pack, stock samples or legacy raster was used. Source rasters, reference extracts and the full prompts are retained under `directional-actions/source/`; project release-license selection remains owner-controlled.

`compact-north.png` supplies three actors × two phases; `compact-east-west.png` supplies four phases/directions × three actors. These are the runtime-selected source sheets. Initial `player.png`, `sirs.png` and `add.png` sheets are retained as first-pass evidence but are not selected: extended barrels/flash tips produced inconsistent body scale and crossed a nominal source split. Focused compact-pose generation corrected those defects without erasing the first sources.

`pack_attacks.py` records the selected source crop, scale, alpha threshold and placement for every new frame. Each pair shares one scale. The lower leg/foot band sets a native anchor of (16, 30), so a growing muzzle flash does not shift the actor's center. New frames clear alpha below 32 and preserve other alpha; exact south reuse receives no raster processing. Generated source reduction creates native 32-pixel frames, followed by crisp nearest-neighbor 4× export. Editing prompts or source rasters and repacking is supported. AI generation itself is not deterministic; export from the retained rasters is byte-repeatable.

## Verification and evidence

Ran the bundled Python with `tools/media/pack_attacks.py --verify`: PASS. This checks exact dimensions/RGBA, nearest 4× equality, all 24 occupied cells, transparent background, no alpha values 1–31, genuine pixel changes in all 12 pairs, six exact south cell comparisons, previous actor atlas hash, all source raster hashes and an independent second packing's byte identity. Native changed-pixel counts across the pairs range from 277 to 576. `validation.json` records the actual results, and `manifest.json` records per-cell provenance and packing details.

| Output | SHA-256 |
| --- | --- |
| `attack.png` | `558653f8113d6d1e29a6f21a92b0db203cb166442d5a92b6f2a57215070fe68f` |
| `attack-native.png` | `cbdfd8beb8b44f8fe5ed7fcff0cc625f722f8a9da07315264b012f79fd3a46a2` |

Inspected the source sheets, native transparent atlas, and `review/attacks-4x-on-dark.png` for body/gun direction, anatomy, silhouette, phase differences, cell alignment and transparency. `review/attacks.gif` provides a two-phase contact animation at 170 ms per phase for integration review. The owned asset directory is approximately 6.0 MiB including retained first passes, references and review evidence.

These are static export checks. No game build, launch, target-facing behavior, animation timing, FOV/occlusion, hit synchronization or runtime acceptance is claimed. The lead owns registry/renderer integration and in-game review. The compact two-phase braced perspective can change silhouette height/width relative to walking and needs comparison at actual game scale. Weapon-specific rifle/scattergun/melee variants and additional death facings are outside this bounded refinement; existing deaths remain unchanged.
