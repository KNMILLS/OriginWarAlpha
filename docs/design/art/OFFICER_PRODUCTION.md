# Officer illustration and motion development

Owner-selected method: Blender-driven animation with an illustrated finish. The painted officer remains the identity/finish target. Exclusive worker scope is the new production officer assets, their tools and this document. Engine integration, original A/B proofs, shared registries/atlas, builds and runtime evidence remain lead-owned. No purchase or campaign-wide expansion is authorized by this bounded pilot.

## Current visual status

**The anatomical-head version is approved only as a development candidate for native evaluation. Commercial finish and owner artistic acceptance remain OPEN.** Pilot1 and pilot2 were rejected: the first mislabeled an extreme stride as idle; the second improved neutral stance but retained a mask/mannequin face. The third construction hypothesis imports a verified CC0 anatomical head inside an original open helmet and U-shaped visor surround. A bounded correction reduced head recline, restored warmer ivory/charcoal overlap values and fitted restrained brows to the real forehead. It remains visibly flatter and simpler than the painted target.

Rejected iterations remain under `assets/art/production/officer/review/iterations/` with original renders, `.blend`, metadata and model/motion/export source. Original A/B proof exports and the prior untracked `.blend1` backup are untouched. The immutable clean head source is checked in under `source/anatomy/`; the exporter loads that relative path, with no private-tool-path dependency.

## Reusable motion result

The opening candidate supplies **76 clips /660 real rendered PNGs** across all four facings. Pistol/suit and unarmed/suit both have idle, start, walk, stop, guarded ready, work, hit and death. Pistol additionally has aim and fire; unarmed has its own emergency melee and never borrows a weapon/fire pose. Walking has36 poses at60Hz and spans **0.5cells over0.6seconds**, so a cell takes1.2seconds. Six-pose start/stop clips last approximately0.167seconds each. These affect presentation only.

`clips.json` retains the named frame contract and explicit actor/weapon/armor/action/facing bindings. `cycleCells` controls distance, with matching documentary `cycleDistanceCells`. Runtime canvases are **288×336**, rendered directly at192raster pixels/referencecell (75% of the384×448 /256px editable master configuration). Logical draw bounds stay1.5×1.75cells; normalized pivots come from the source camera's ground origin. All poses share one calibrated camera. No per-frame image filtering or rescaling paints the finish. Idle/start/stop/work/hit/fire/melee return poses have exact pixel-equal endpoint checks; start/stop also match the actual cycle-contact pose.

Each frame reports anatomical left/right sole anchors, stance/swing classification, projected local pixel/cell coordinates, expected root displacement and root-plus-local foot position. Per-facing ground travel counters screen-axis root movement. The current maximum sampled stance residual is below0.000021 reference pixels. Held60Hz sprites still have an exposure-drift upper bound of3.556master-reference pixels, approximately0.653pixels for a47px cell. This is an analytical source bound, **not measured native skating**. Actual camera/timeline/footage agreement remains lead-owned.

Fire lasts0.5seconds with variable frame exposures; frame2 release is exactly0.14seconds, recovery0.35seconds. Its small muzzle spark is already rendered; avoid another sprite flash/recoil. Work is a left-hand reach while the pistol stays physically attached to the actual right hand; root can retime it to a paid commitment. Hit is restrained recoil. Death is a slumped collapse, not a finished prone/death performance. These acting/readability limitations need native review. Turning remains a static missing-coverage report; no 90/180-degree clips are supplied.

## Visual evidence and timing

The current review folder contains four-facing before/after comparisons at100px and60px opaque officer height, and source-projected `planted-start-walk-stop-south.gif` / `east.gif` previews. These previews use a77px source-preview cell, real exported frames and root travel; they are not native gameplay captures. GIF timing is quantized to10ms. Every production PNG is a Blender scene render with materials/lighting; no per-frame pixel filtering or image-based pose correction is used.

Pilot1 process:39.62seconds; pilot2:49.13seconds. The final opening batch took148.58seconds, including2.31seconds model/calibration and146.27seconds exports. These are process durations, not total authoring effort. Three construction hypotheses and four bounded look corrections are recorded; `source/PROVENANCE.json` records inclusive authoring/review wall time. Exact PNG bytes total42,320,627. Padded trimmed RGBA area before deduplication is69,977,744bytes: a five-page minimum at2048px, before packing waste. Actual lossless packing/residency and the128MiB ceiling remain lead-owned; the preferred four-page budget is not claimed.

## Reproduction and limits

Use pinned official portable Blender4.5.3 LTS with `--background --factory-startup --python tools/media/officer_production/build.py`, then run `python tools/media/officer_production/review.py`. Default rendering is75% runtime resolution; `--master` reproduces the256px reference configuration. `--look-only` limits a new construction test to four idle stills. The copied prior exporter is guarded reference-only and cannot overwrite preserved proofs. Source retains weighted geometry, material nodes, camera and a keyframed walking action; other authored action pose rules are editable Python source, not falsely described as hand-painted animation projects.

The anatomical foundation is Dan Ulrich's **Head (Animation) - Realistic** from official Blender Human Base Meshes1.4.1, explicitly CC0. Only the head and four eye meshes are imported; the source's unrelated Rain Rig/CC-BY text is not a dependency. Exact official URLs, hashes, embedded declaration and clean extraction evidence are retained under `source/anatomy/`. Production applies subdivision, fitting, material and rig changes to a copy. Suit, helmet, equipment, rig and pose rules are original project work. No purchase was made.

Other weapons/armor, turns, full prone death, broader acting polish and campaign art expansion remain out of scope. Root owns native integration and the owner owns artistic acceptance. Technical source checks, source previews, native gameplay, human reaction and commercial finish remain distinct.
