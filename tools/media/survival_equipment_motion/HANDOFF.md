# Equipment-motion integration handoff

Base: `ee8d8a3` (the survival root plus footer correction). Exclusive worker output: new equipment-motion assets and the matching exporter/validator tree only. Shared engine, Java, atlas/audio registries, previous source and existing clip manifests are untouched.

## Load and select

Load `art/survival/equipment-motion/regions.json` using the same named-region reader as `art/survival/regions.json`; then load `art/survival/equipment-motion/clips.json` through the existing motion loader. The new regions are complete independent PNGs with the same normalized sizing/pivot fields.

For officer state `unarmed`, `rifle` or `scattergun`, choose `officer.<equipment>.<north|east|south|west>` as the whole painted character. Keep this static during movement and combat until genuine matching action art exists. Do not play a pistol clip or attach a gun over it. Normal translation and authoritative lighting still apply, but no synthetic sine bob or extra recoil.

Pistol state keeps existing genuine pistol walking and E/S/W firing. The new `officer.fire.north` and `officer.aim.north` complete the missing cardinal pistol action lookup. Configure release timing from the clip marker rather than a second hardcoded timer. The north source includes the visible flash on release; avoid duplicating it with another large sprite flash. Existing small scene illumination remains a separate lighting event.

No machine facing source was supplied in this bounded pass. Do not relabel the old three-quarter machine cycle as cardinal coverage.

## Evidence and review

Worker build and validator PASS: twelve unique static regions, four unique north drawings, one documented aim alias, transparent alpha, stable sole anchors, scale/canvas/pivot checks, preserved source/reference hashes, editable archive CRC and repeatable export. Four generated masters were inspected full-size before extraction; both exported contact sheets were then inspected. No native window was launched.

Root owns native checks: equip/drop all three states, confirm empty hands and distinct long/short gun silhouettes across all four facings while stationary/moving; fire pistol north and inspect flash/audio marker timing, body scale, pivot and foreground/lighting. Test pause/focus continuity. Static equipment motion limitations must remain in the qualification record and player delivery claims.

Generation: built-in image_gen, transparent output, four bounded edits. Exact prompts and reference/output identities are in `source/GENERATION.json`; source copies are retained in the workspace. No CLI fallback, external pack, legacy asset or runtime generator was used. Commercial quality and owner artistic acceptance are not claimed.
