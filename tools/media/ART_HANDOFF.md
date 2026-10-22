# M5 art handoff

Task M5-ART; baseline74ea660966d6d76542bae76506d20e21426babc5; branch codex/m5-art. Only the owned animation/sector/finale folders, pack_animation.py, validate_art.py and this handoff change. Existing station atlas/source/manifest, audio, shared registries, model, renderer, build and top-level docs remain unchanged. Root owns GameArt registration, real game captures and acceptance.

The new artwork was produced with the built-in OpenAI image_gen tool from recorded prompts, using existing original project-generated art as identity/material references. No external art pack, stock or legacy media was used. Original reference extracts, selected source rasters, first failed/partial sheets and prompts are preserved beneath the corresponding source folders. Editable prompts and region/anchor/export code are provided; generated raster sources are not layered Aseprite projects. These are original project-created deliverables; release-license selection remains owner-controlled.

The actor export is assets/art/animation/actors.png, exactly1024×1024, eight columns/eight rows of128px cells. actors-native.png is256×256 with32px cells. Each native cell is enlarged4× with nearest-neighbor sampling.

| Runtime rows | Exact columns0–7 |
| --- | --- |
|0 player | north2, east2, south2, west2 |
|1 A.L.I.C.E. | north2, east2, south2, west2 |
|2 S.I.R.S. | north2, east2, south2, west2 |
|3 A.D.D. | north2, east2, south2, west2 |
|4 R.O.V.E.R. | north2, east2, south2, west2 |
|5 | playerAttack2, playerDeath2, SIRSattack2, SIRSdeath2 |
|6 | ADDattack2, ADDdeath2, ROVERarmed2, ROVERexplosion2 |
|7 | camera2, turret2, operator2, Constructor2 |

Every pair contains actual illustrated pose differences; no copied cell was relabeled as animation. Source inspection checked back views, horizontal facings, human limbs, quadruped ROVER anatomy, unarmed A.L.I.C.E., recoil/collapse/wreck phases and machine movement. Walking pairs have fixed per-pair size and bottom-center anchor. Death pairs are sequential collapse/down states; they should not loop as walks. Explosion phases retain source alpha; other isolated sprites clear alpha below32 to remove extraction noise.

First failures remain visible and documented in animation/manifest.json. Initial A.L.I.C.E. had mixed directions; its valid south pair alone is selected, with freshly generated focused rear/east/west pairs. Initial ROVER north looked forward; its other six frames remain selected and a new rear pair supplies north. The rear pair has head/neck at the top and an amber lower rear service lamp; confirm its orientation reads clearly at game scale. Machine source initially included painted gray ground patches; a focused built-in edit removed them. No invalid direction cells were silently relabeled.

Sector export assets/art/sectors/sectors.png is1024×512, eight columns/four rows; sectors-native.png is256×128. Slot groups are0–4 floors,5–9 walls,10–14 furniture,15–19 consoles/lights,20–24 clutter,25–29 story anchors. Every five-slot group is docking/security, habitation/maintenance, medical/research, defense/industry, command/evacuation. Slot30 is escapePod;31 is MarsWindow.

The five sectors differ in panel structure, furniture/equipment, clutter and color/material identity. Floors sample genuine opaque interiors of generated floor cells. Props have transparent empty space and no rectangle floor backdrop. The selected sector source did not honor equal vertical row spacing; the first equal-row export included neighboring fragments. Inspected alpha trough boundaries now select each complete source row explicitly. Source PNGs remain unchanged and the packing recipe is reproducible.

Lead-authorized finale addendum: assets/art/finale/finale.png is an opaque1024×5122:1 pixel-art background, exported2× nearest from finale-native.png at512×256. It depicts Mars's warm horizon, one departing original-design pod and distant station destruction, with dark upper-center sky for the ending epilogue. Root selected ending-only use so the title does not spoil the sacrifice. The unchanged generated source and original prompt are retained. This static illustration does not establish broadcast/destruction/pod event animation, audio timing, overlay readability or actual canonical-ending acceptance.

Rebuild/verify with Python3, numpy and Pillow:
- python tools/media/pack_animation.py
- python tools/media/validate_art.py --verify-repeatability

Each folder includes slot/source manifests, hashes and static validation. Review folders contain dark-backed4× contact sheets and two-phase direction/action GIFs. Native and4× contact sheets were inspected after source fixes and packing.

Limits requiring real game review: cycle cadence, combat/death timing, feet/occlusion, sector lighting and repeating-floor seams. Art uses elevated illustrated three-quarter depth matching the existing style, not a strict plan projection. Attack/death poses currently face south; the contract does not include every directional attack/death or new West/Carmack directional sets. Sector dressing supplements existing corners/doors/terrain rather than replacing every combination. Story dressing is not evidence of implemented story behavior. No runtime, human acceptance or complete-final-art claim is made.
