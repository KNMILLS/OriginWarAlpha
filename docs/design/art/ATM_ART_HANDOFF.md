# ATM-ART implemented candidate

Baseline `374e27c764d55adaae7a9a3b4003a5ff883de157`. This is the owner-authorized material/helmet/animation implementation following the earlier FB1 proposal. All changes are art assets, art-only tools, provenance, this handoff and `GameArt.java`. Simulation, audio, placement, lighting, HUD and action timing remain lead-owned.

## Shipped source and exports

The new materials are worn institutional enamel, sage paint, graphite utility panels and quiet blue-gray command steel. Screens remain dark; tiny status cores are separate presentation geometry. There are no supernatural forms or franchise assets. The original helmet portrait supplies the enclosed white/gray helmet and dark visor identity; the bareheaded teal officer is replaced in every player walk/attack/death cell consumed by the renderer.

| Runtime atlas | Replaced slots | Content |
|---|---|---|
| `art/station.png`,1024×1024 | 0–19,29,30,32–35 | Five floors; wall/corner/glass; closed/open/locked doors; low/zero-gravity and hazard floors; cover/switch/hatch/terminal; command console/tools; four south player poses |
| `art/sectors/sectors.png`,1024×512 | 0–29 | Five floors, five walls, five blocking furniture pieces, five wall consoles, five clutter objects, five story terminals |
| `art/animation/actors.png`,1024×1024 | 0–7,40–43 | Four cardinal player pairs; aim/recoil and collapse/prone |
| `art/animation/directional-actions/attack.png`,1024×384 | 0–7 | Player N/E/S/W ready/recoil pairs |
| `art/atmosphere/player-walk.png`,512×512 | New4×4 | Rows N/E/S/W; four gait poses per row |

All existing runtime cells remain128px. New native cells are64px, exported nearest2×. Untouched prior32px cells are expanded2× to the new native grid and remain **pixel-identical at runtime**. Existing human, robot, ALICE and finale artwork is preserved. Historical per-pass manifests explicitly point to the authoritative [ATM manifest](../../../assets/art/atmosphere/manifest.json); their old crop records remain history.

Actual baseline `OriginWarApp.world()` consumption was checked: sector0–4 floors,5–9 walls and15–19 seeded wall consoles are replaced; station7/11/12/13 glass/low-gravity/zero-gravity/hazard overlays are also replaced. No old yellow checker/hex floor survives through those paths. Station0–6 fallback material cells are replaced too. This is a source mapping check, not a runtime capture.

## Renderer integration contract

`GameArt` retains all existing public APIs and adds these presentation-only methods. No clock, RNG, model or simulation state is accessed.

| Method | Caller contract |
|---|---|
| `playerWalking(batch,facing,progress,x,y,size)` | Facing0N,1E,2S,3W. `progress` is one full gait cycle; fractional phase selects one of four actual poses. Optional file absent → existing two-frame actor fallback. Caller owns motion and cadence. |
| `door(batch,locked,openFraction,x,y,size)` | Fraction0closed,1open; finite values clamp. Stationary open-frame jambs with clipped inner panel halves slide at intermediate fractions. Closed locked/unlocked art remains exact at0. Caller owns state/event timing. |
| `emissive(batch,kind,phase,x,y,size)` | Call **only for legitimately visible fixtures**, after world darkening. Kind0terminal,1switch,2hatch jambs,3wall strip. Fractional `phase` is caller-supplied and may be offset per fixture. Tiny core rectangles, no opaque glow halo or light spread. Batch color restored. |

Emissive coordinates are native64 pixels, bottom-left origin: terminal lines `(24,49,14,1)` and `(24,47,9,1)`, amber status `(40,40,2,2)`; switch `(35,42,2,2)`; hatch jambs `(3,31,2,2)` and `(58,31,2,2)`; wall strip `(24,53,16,1)`. These correspond to inspected final fixture pixels. Root owns SquidGrid shadowed falloff and FOV gating. Door inner crop is normalizedx.14–.86,y.13–.87, centered split atx.50. New texture uses nearest filtering and is disposed in `close()`.

Lead must add `art/atmosphere/player-walk.png` to the runtime resource whitelist and512×512 dimension gate. The mixed audio/art `validate_media.py` still had a native32×4 station assumption at this branch baseline; the lead was notified and owns its adjustment. `validate_art.py` delegates to the new art-only validator when the ATM manifest exists. Do not run the old atlas packers to regenerate current atlases; use the command below.

New sector20–22 cargo/cart/IV props read as solid objects. They are suitable for authored dressing/blocking placement, not arbitrary walkable-floor scatter. Lead has elected to skip those in seeded walkable scatter and retain flat papers/tools where appropriate; this worker does not change collision or placement.

## Sources and rights record

Selected built-in OpenAI `image_gen` rasters, retained first passes, the exact owner portrait copy and untouched baseline native atlases are under `assets/art/atmosphere/source/`. The [generation record](../../../assets/art/atmosphere/source/GENERATION.json) identifies source output names and selections. Source and output SHA256 values, actual uneven row boundaries and crop choices are in the ATM manifest; the packer only crops, resamples, cleans detached alpha fragments and packs. It does not paint the game art in Python. Prompt records distinguish readable request briefs from the exact final terrain tool prompt. The original `legacy/core/assets/playerTexture.png` remains unchanged; the owner reference is not relabeled as third-party CC0 art or assigned a new blanket license. No legacy recording or other legacy runtime asset is introduced.

Primary free-use alternatives were inspected before selecting a coherent original pack: [Kenney Space Station Kit](https://kenney.nl/assets/space-station-kit) (CC0,3D), [Kenney Sci-Fi RTS](https://kenney.nl/assets/sci-fi-rts) (CC0,2D RTS), and [rubberduck's32×32 Sci-Fi Platformer extension](https://opengameart.org/content/extension-for-sci-fi-platformer-tiles-32x32) (CC0,side view). **None is imported.** Their differing projection/style would require adaptation and would not restore the owner's helmet identity. The [official SquidSquad repository](https://github.com/yellowstonegames/SquidSquad) documents libGDX compatibility and squidgrid's FOV/lighting tools. This pack uses ordinary PNG/TextureRegion resources compatible with that renderer; no added library is needed. Research accessed2026-10-20. Mood references are not visual source assets.

## Verification and bounded limits

`python tools/media/validate_art.py --verify-repeatability` PASS:14 source PNG hashes, five current atlas layouts, exact nearest2× pixels, all56 changed station/sector slots as declared, unchanged cells preserved, transparent open door/reserved blank,16 distinct gait poses with common foot anchor, alpha cutoff, plus retained human/finale format/scale checks. Current results are in [validation.json](../../../assets/art/atmosphere/validation.json).

Independently inspected exported images: [before/after materials](../../../assets/art/atmosphere/review/material-before-after.png), [3×3 terrain repetition](../../../assets/art/atmosphere/review/terrain-tiling.png), [sector props](../../../assets/art/atmosphere/review/sectors-2x.png), [station](../../../assets/art/atmosphere/review/station-2x.png), [player gait](../../../assets/art/atmosphere/review/player-walk-2x.png), [attacks](../../../assets/art/atmosphere/review/attacks-2x.png), and [full actors](../../../assets/art/atmosphere/review/actors-2x.png). Review found and corrected two mismatched-scale mixed-source poses and detached row-neighbor fragments; the raw first attempts remain. Final floors have no white gutters or old checker texture; glass is dark; props retain transparent negative space; helmet/hair silhouette and cardinal facings are legible at the exported contact scale.

Not claimed: game launch/build, actual light/FOV/occlusion acceptance, door opening timing, feel of motion, emotional tension or owner artistic approval. West is explicitly mirrored east, including weapon handedness. North/south are selected generated gait poses rather than a rigged anatomical cycle; the compact carried-sidearm/arm silhouette varies between phases. The tiny portrait-derived face does not prove individual blue eye pixels at native scale. North aim/recoil extents differ from walking and need an actual motion-size check. Player poses, repeated floors in every zone, visible fixture cores and opening doors should receive lead-owned in-engine captures before acceptance. The optional gait can fall back without changing turn costs or saves.
