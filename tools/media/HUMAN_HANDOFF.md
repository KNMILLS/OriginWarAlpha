# M5 human idle animation

Prepared on `codex/m5-human-idles` from `874e205cc79152a833cc7b0c8b308538ae42e907`. Owned additions are only `assets/art/animation/humans/**`, `tools/media/pack_humans.py` and this handoff. No existing atlas, registry, renderer, model, audio, shared documentation or build file was changed.

## Exact integration contract

`assets/art/animation/humans/humans.png` is RGBA 1024 × 128, eight columns × one row. `humans-native.png` is RGBA 256 × 32; each native cell is 32 × 32 and the runtime export is exact nearest-neighbor 4×.

| Columns | Family | Frames |
| --- | --- | --- |
| 0–1 | WEST | relaxed / breathing-hand-head adjustment |
| 2–3 | CARMACK | relaxed / breathing-hand-head adjustment |
| 4–5 | SURVIVOR | relaxed / breathing-hand-head adjustment |
| 6–7 | OPERATOR | exact existing actor cells 60–61 |

The human pairs are quiet front/down conversation idles, not walking or attacks. West retains his navy officer cap/coat and amber insignia, Carmack his brown hair/white coat/teal shirt, and the survivor an amber safety helmet and dark steel/amber workwear. West can be consumed as an offstage radio/dialogue avatar without inventing a station spawn. The lead owns all world/dialogue renderer consumption, main-clock timing and acceptance.

## Sources and provenance

One built-in OpenAI image_gen call produced the selected original six-cell sheet. The first output was usable; there are no discarded generation failures. `source/humans-generated.png` retains its unchanged generated RGBA raster; `source/prompt.txt` retains the complete prompt, and `source/station-humans-reference.png` retains nearest-scaled extracts of the existing original station slots 52, 53 and 54. Local source-directory attributes preserve these exact source/prompt bytes across Windows checkout/archive newline conversion. The existing original actor atlas supplies the two operator frames without raster processing. No external artwork, stock pack or legacy media was used. Release licensing remains owner-controlled.

`pack_humans.py` crops a normalized 3 × 2 source grid, shares scale within each pair, and uses the lower foot/leg geometry to anchor at native (16, 30). It fits the new silhouettes to at most 28 pixels tall, clears alpha below 32, and preserves the remaining alpha. It then exports native and nearest4 PNGs, a dark-backed contact sheet and a 500-ms-per-phase review GIF. `manifest.json` records every source hash, crop, scale, placement, palette and reuse slot. Generation is not deterministic; packing from retained source rasters is byte-repeatable.

## Actual checks

Bundled Python `tools/media/pack_humans.py --verify`: PASS. Exact RGBA dimensions, eight occupied cells, native-to-runtime nearest4 equality, transparent empty space, alpha cutoff, no edge clipping for new humans, foot anchors, unchanged reference atlas hashes, all source hashes and independent second packing byte identity pass. All four pairs have changed color and solid-mask geometry; new human pairs change 354–408 native pixels and 33–39 solid-mask pixels, rather than repeating identical cells. Both operator native frames match existing cells exactly.

Inspected the source, `humans-native.png` and `review/humans-4x-on-dark.png` for human anatomy, outfit identity, pair differences, foot placement, transparent separation and readability. `review/humans.gif` is an integration preview, not runtime evidence. The owned asset set is approximately 1.4 MiB including source and review files.

| Output | SHA-256 |
| --- | --- |
| `humans.png` | `f42694c33c46c32fc3ca2f2612e8bae0f7b40e5f94a60183688daceb27103044` |
| `humans-native.png` | `f19adcbb4dbdc0a75a830ea8d0cd3d388387c4d5e09edd20d1ddef3c37628e0a` |

No game build, runtime capture, audible review, world/dialogue animation timing or human acceptance was performed by this worker. The quiet two-frame south-facing idles complete this bounded human animation content; other facings/walk cycles are outside the requested stationary NPC scope. Runtime consumption and actual in-game scale/readability remain lead checks.
