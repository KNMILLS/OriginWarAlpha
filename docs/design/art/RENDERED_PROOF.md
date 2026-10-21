# AP-RENDERED: editable rig to illustrated sprite comparison

This is an isolated visual production test under the owner-authorized art proof. It does not replace the current art direction or certify commercial illustration/animation quality. The active survival rules, saves and renderer are unchanged by this branch.

## What is actually made

An original Blender officer model has tailored cream pressure layers, dark flexible joints, a sealed smoked visor, harness, oxygen cylinders and supply hose, modeled fasteners/stencils/worn edges, boots, glove parts and weapon geometry. A real weighted armature drives independently positioned limbs and equipment; the editable `.blend` retains the twelve-pose walking action. It is procedurally modeled mesh artwork with authored pose rules, not generated hand-painted frames. It contains no external model, texture, animation, stock photograph or franchise asset. The owner player reference and current turnaround were viewed for identity; neither is represented as an imported third-party model.

The bounded refinement darkened the visor, replaced ball-like shoulder armor with chamfered layered shells, narrowed hips, lengthened the leg silhouette and added thin enamel/black greave and gauntlet plates. The first rejected rest-roll outputs and the pre-refinement south/east images remain in `assets/art/proof/rendered/review/`. The current style still reads as stylized 3D rendering. It should be judged against the painted proof in the same native room, not promoted by technical validation.

## Stable runtime contract

`assets/art/proof/rendered/clips.json` uses the existing frame path, `widthCells`, `heightCells`, normalized bottom-left pivot, and frame duration fields. Its explicit bindings select actor, weapon, armor, action and facing. Frames are transparent 384×448 canvases at 256 pixels per reference cell, preserving a common camera and projection-derived foot anchor. The neutral officer is approximately 1.3 cells tall in opaque pixels. Alpha padding is intentional.

There are forty clips and 196 real renders: four facings each for pistol/suit walking, idle, ready, aim and fire/recovery; rifle/suit walking, idle and aim; pistol/heavy walking and idle. Walking has twelve sampled poses at 24fps, one full cycle per half-second. The proof-specific `cycleDistanceCells` is **1**, not the preserved production candidate's two-cell cycle. Shared proof presentation should therefore translate one cell over half a second. Paid simulation time must remain unchanged. Native planted-foot agreement remains unaccepted; anatomically plausible in-place movement is not proof of zero skating during square-grid translation, especially under cardinal foreshortening.

Pistol fire has four 70ms poses, release at frame 2 (140ms), recovery at frame 3 (210ms) and total duration 280ms. The small release spark is already drawn in frame 2; do not add another sprite flash or recoil. Ready is a separate four-pose transition. Bone-local equipment orientation was explicitly corrected after a rendered aiming sample exposed an upward-pointing muzzle.

Unarmed, scattergun, light armor, heavy rifle, machine, hit, work, interaction, death and door states are not supplied here. Missing combinations must remain visible in the proof-coverage report. The lead owns importer/selection, timeline, atlas integration, scene lighting, native footage and public integration.

## Reproduction and checks

Portable Blender 4.5.3 LTS is installed privately at `C:/OriginWarReboot/.tools/blender-4.5.3-windows-x64`; its ZIP matched the official publisher SHA256. No software purchase, system installation or third-party asset purchase was made. Python 3.11, Pillow and NumPy produce faithful review contact sheets and structural checks; they do not paint or repair production pixels.

From this checkout, run the portable Blender with `--background --factory-startup --python tools/media/art_proof_rendered/build.py -- --full`, then run `python tools/media/art_proof_rendered/review.py`. `--pilot` supplies four pistol idle facings and the south walking cycle for a bounded look review. The native source and images use real scene lighting; the file's full model, material nodes, weights, equipment objects, camera and walking action remain editable.

The structural check verifies manifest hashes, canvas/pivot agreement, true alpha, safe uncropped bounds, twelve distinct locomotion images and actual changing silhouette masks. Review GIFs quantize a 500ms cycle to ten-millisecond units; they are previews, not evidence of native 60Hz presentation or synchronized sound. The worker viewed actual rendered south/north/east stills, action samples and exported contacts. Artistic quality, native equipment grips, foot plants, runtime performance and owner choice remain OPEN.

## Rights and delivery

This candidate uses only original authored model/material/pose source and the owner's project identity guidance. Blender is a free GPL authoring application; its binaries are not part of the player runtime. Exact tool URLs/checksums, source/script hashes and original-source declarations are in `assets/art/proof/rendered/source/PROVENANCE.json`. Restricted commercial asset sources must not later be placed in the public repository or source ZIP without explicit license permission.
