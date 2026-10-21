# SH-ART production extraction

Run `python tools/media/survival_art/build.py` from any directory, then `python tools/media/survival_art/validate.py`. Requirements: Python 3.11+, Pillow, numpy, scipy. The technical review labels use the Windows Arial font; they are not game art or a bundled font dependency.

The builder performs alpha-connected-component selection, copies each selected original's antialias fringe, crops, proportionally resamples and exports. It never draws/repaints production art. Floor crops are six exact 512px squares. Full source rasters and exact prompts are immutable, hash-checked production evidence. Component IDs and crop boxes are in the region manifest.

`editable-kit.ora` is an editable OpenRaster catalog with one supplied cutout per named layer at the 256px reference-cell scale. It does not reconstruct nonexistent brushstroke/rig layers. Original generated PNG masters and owner reference remain separate. Animators should use those larger original masters to preserve detail, and may produce their own genuinely layered character projects.

Runtime images use smooth downsampling and real alpha; do not route them through historical nearest-neighbor pixel-art packers. The new manifest is isolated from prior registries. The integration lead owns runtime loading, texture filtering/padding, depth sorting, lighting, collision and clip integration.

The contact and room composite are technical compositing reviews, explicitly not native game screenshots. The validator certifies integrity/reproduction only. No actual gameplay, animation, human artistic acceptance or commercial quality is certified here.
