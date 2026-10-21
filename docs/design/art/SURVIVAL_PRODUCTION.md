# SH-ART illustrated survival production

Task SH-ART, baseline `96a1c6e`, exclusive branch/worktree `codex/survival-art` / `C:/OriginWarReboot/worktrees/survival-art`. Authority: the owner-approved `docs/directives/SURVIVAL_HORROR_20261021.md`. This supersedes the old native-pixel medium. No shared registry, Java, model, build, audio, legacy or historical art file is changed by this worker.

## Delivered original kit

Six original built-in OpenAI image_gen master sheets supply **42 named regions**: six floor materials; four officer facings; twelve structural/functional props; six different machine silhouettes; ten physical supplies/equipment pieces and two paper/tool props; two A.L.I.C.E. communication portraits. The owner's original helmet illustration is retained unchanged as the officer identity reference, not relabeled as third-party art. No external art pack, franchise asset, photograph, copyrighted character or benchmark screenshot is imported.

Direction: drawn and painted 2D, mature white pressure-suited officer, industrial enamel/steel/rubber, restrained instrument accents, indifferent functional station. The raised architecture and flat walking surfaces remain distinct by form and material. Machines have distinct upright/quadruped/platform/mounted silhouettes. A.L.I.C.E. is an adult humanlike communicated avatar, with attentive and concerned expressions; she is never a world sprite. Her sparse scanline treatment belongs to the portrait, not a blanket interference overlay.

The current generated master PNGs are flattened originals. The editable OpenRaster catalog keeps each region on its own named layer, preserving authorable arrangement without falsely claiming original brushstroke or skeletal source. Large original master cutouts are available to the animation specialist; static poses do not establish twelve-phase locomotion, weapon-state variants, impacts or animation quality.

## Integration contract

`assets/art/survival/regions.json` contains `version`, `referenceCellPixels:256`, `pivotOrigin:"bottom-left"`, `pathRoot:"assets"` and a `regions` array. Every region contains required `name`, libGDX-relative `path`, `widthCells`, `heightCells`, normalized `pivotX`/`pivotY`, and `layer`. Additional source/crop/hash/pixel-size metadata is documentary. Runtime consumers should ignore unknown metadata.

The logical square grid is independent from raster resolution. Draw sizes are proportional trimmed dimensions in grid cells. Never stretch every region into a one-cell square. Officer facings share 1.3-cell height; S.I.R.S. is 1.4 high; Constructor is two cells wide. Ground floors use bottom-left pivot (0,0); cutouts use bottom-center contact pivot (.5,0). Layers: `ground`, foot-depth-sorted `world`, and screen-space `avatar`. Contact positions, collision footprints and visible-state disclosure are lead-owned; draw bounds do not imply multiple occupied cells.

Use linear filtering with appropriate atlas padding if the lead subsequently packs these separate PNGs. The images are production cutouts, not fixed-grid atlas slots. Keep grounded objects/actors sorted by their contact depth, with correctly separated foreground/cutaway behavior. The open door's central aperture is genuinely alpha-transparent. Glass currently has an illustrated tinted pane and must not be treated as an empty opening.

The six diffuse floors are security graphite, habitation sage linoleum, medical mineral, industrial grating, command blue-gray composite and low-gravity anchors. Their internal panel seams are not a gameplay grid. No emissive mask, normal map, power-state variant, animated door panel layer or full-sector architecture family is claimed in this initial kit. Lighting should be applied by the actual observed circuit/lamp state, not inferred from decorative painted highlights.

## Source and reproducibility

Unmodified image outputs, exact prompts, copied owner reference and hashes live under `assets/art/survival/source`. `GENERATION.json` identifies original output files, source role, prompt, selection and limitations. All first outputs are retained; there were no rejected generation alternatives in this task. `machine-initial.png` is the selected first output, not a hidden rejected candidate. Sources have variable original dimensions; extraction copies the largest connected sprite silhouettes plus their original antialias fringe, then proportionally resamples to the agreed 256px reference-cell coordinate system. No production pixel is drawn in code.

Rebuild/validate instructions are under `tools/media/survival_art/README.md`. The isolated builder replaces no historical packer or registry. Validation checks 42 names, source/prompt/output hashes, dimensions, six opaque floors, 36 true-alpha cutouts, distinct officer facings, open aperture and byte-repeatable production exports. The editable catalog also has deterministic ZIP timestamps and reproduces byte-for-byte.

## Actual inspection and acceptance limits

Every generated master was inspected. Exported `review/all-regions-contact.jpg` and `review/technical-room-composite.jpg` were independently viewed after extraction. Isolation successfully separates overlapping *bounding boxes* on the rifle/scattergun sheet without copying neighboring sprites. Original alpha backgrounds are preserved; the machine sheet contains RGB values under alpha zero, which are not an opaque backdrop. Officer cardinal silhouettes, white suit/harness, functional equipment and machine distinctions remain legible at contact scale. The technical room demonstrates relative size and common materials only.

Remaining: native 720p/1080p/owner-display gameplay; real light and power changes; corner/occlusion and repeated-floor composition; contact shadow; smooth animation; complete machine facings and state variants; item/weapon silhouette continuity; complete departmental dressing; portrait placement and emotional performance; performance/capture; unfamiliar-player readability; owner artistic approval. The officer keeps a sidearm in its four static views, so using these poses while visibly unarmed or carrying a long gun is a known integration limitation until the animation/equipment art pass. First-person impersonation or a physical A.L.I.C.E. actor is not authorized by this avatar source.

Commercial-quality acceptance depends on the integrated 15–20-minute slice, normal-speed motion and sound, not generated detail or these structural checks. The lead remains the sole runtime/integration owner.
