# MAT2 material separation

Implemented candidate on exact baseline `039bebc8e0d36ab2497bd648c46da6de67d0fe2b`, answering the owner's feedback: “The walls and floors look too similar.” This pass changes materials only. No renderer, GameArt, model, collision, navigation, audio, placement, door mechanism or animation timing is changed.

The previous docking frame used framed metal rectangles on both floor and wall. The medical/habitation walls repeated the floor's pale planar material; the industrial/command pair also shared dark steel panel construction. I inspected actual prior integrated `.evidence/atmosphere-v2/station.png` and the packaged `atmosphere-rc1-runtime/operator-userdata/screenshots/sector-2.png` through `sector-5.png`, covering all five material families. These are prior runtime evidence; the comparisons below are explicitly labelled source composites.

## Material change

Walls now read as cool raised bulkhead blocks: a projecting upper cap, tall vertical ribs/recessed faces, and a near-black lower foot. Floors read as warm flat walking surfaces: matte graphite, muted linoleum, clinical mineral, fine rubber grip and quiet command composite. Their seams are flush and fine; heavy floor bezels, large framing, vents and raised strips are removed. Structure, relief and broad values establish the distinction alongside color.

| Atlas | Changed cells | Preserved |
|---|---|---|
| `art/sectors/sectors.png` | 0–4 floors;5–9 walls, all five families | 10–31 furniture, consoles, clutter, story terminals, pod/Mars |
| `art/station.png` | 0–4 fallback floors;5wall;6corner;7glass;11low-gravity;12zero-gravity;13hazard | 8–10doors,14–19cover/switch/hatch/terminal, all items/props/actors/effects |
| Other art | None | All actor/human/gait/attack/finale and historical source PNGs remain exact bytes |

Native64/runtime128 cell coordinates and atlas dimensions remain unchanged. Corner/glass repeat the raised wall cap/ribs/foot language. Low/zero-gravity and hazard tiles use the new flat warm floor language, restrained chevrons/flush anchor sockets/fine drain slots, with no thick wall-like perimeter frames. Door/console integration and all emissive core coordinates remain unchanged.

The renderer overlays sector15–19 on sector5–9 at wall fixtures. I inspected these pixels and composited all five over their new wall: these are isolated console/equipment silhouettes with transparent surrounding space, **not opaque old-wall backgrounds**. Transparent area is45.46%,54.64%,59.91%,40.43%,42.68%; every corner is alpha0. Their own equipment backplates are retained as physical fixture objects. New bulkhead caps/ribs/feet remain visible around them. Therefore15–19 require no raster change; preserving them exactly also preserves screen/indicator positions. The validator independently checks their unchanged cells and transparent corners, and the [fixture comparison](../../../assets/art/material-separation/review/wall-fixture-integration.png) shows full/25% composites.

## Source and reproduction

Three new original built-in OpenAI `image_gen` sources are retained under `assets/art/material-separation/source/`: `floors.png`, `walls.png`, `special.png`. [Exact prompts](../../../assets/art/material-separation/source/prompts), [generation record](../../../assets/art/material-separation/source/GENERATION.json), source SHA256 values and all crop boxes are preserved. There was one source generation per sheet; no rejected generation was hidden or discarded. Special-sheet white outer padding is excluded using inspected y66:657. Floor cells use centered square source crops to preserve fine grain/grip proportions; wall cells retain their complete cap-to-foot height.

No external/franchise asset is imported. Existing owner identity artwork, props and all earlier provenance remain unchanged. Python performs deterministic cropping/resampling/packing and labelled technical comparisons, without painting production artwork. `art/.gitattributes` already preserves exact source/manifest/export bytes. Frozen native “before” copies and76 original art PNG identities are stored with this pass for independent comparison.

Rebuild current art with `python tools/media/pack_atmosphere.py`: the historical ATM pack runs first and automatically applies `pack_material_separation.py` when the MAT2 sources exist. The material layer can also be run directly. Current manifests point to the MAT2 override; historical source descriptions remain available. `python tools/media/validate_art.py --verify-repeatability` runs current full ATM checks and the new independent MAT2 checks. Do not use an old standalone packer to replace the current layered output.

## Inspected comparisons and checks

- [Materials before/after/full/25%](../../../assets/art/material-separation/review/materials-before-after-dark.png): all five new walls retain their cap/rib/foot silhouette after darkening; floors remain flat and frameless.
- [Special paths](../../../assets/art/material-separation/review/special-before-after-dark.png): corner/glass read as raised barriers; all three movement surfaces read as flat ground.
- Frozen rooms [docking](../../../assets/art/material-separation/review/frozen-room-zone-0.png), [habitation](../../../assets/art/material-separation/review/frozen-room-zone-1.png), [medical](../../../assets/art/material-separation/review/frozen-room-zone-2.png), [industrial](../../../assets/art/material-separation/review/frozen-room-zone-3.png), [command](../../../assets/art/material-separation/review/frozen-room-zone-4.png): identical room geometry, player pose, props, special tiles, open door and numerical light field on both sides. Beam/visor formulas match the source shader; the base field is fixed and all cells are visible. These are synthetic asset comparisons, **not game screenshots or live SquidGrid/FOV tests**.

The [current validation](../../../assets/art/material-separation/validation.json) PASS covers exact source/output hashes, native RGBA/nearest2x runtime formats, byte-repeatability of the layered pack, exactly11station/10sector changed cells, all75 other native cells preserved independently,72 unrelated original PNGs unchanged byte-for-byte, fixture alpha, and the full current ATM asset checks. Floor/wall mean RGB ratios are1.80–4.36 across five zones; cap/foot contrast is4.8–5.1×. The same25% multiplier preserves those value ratios. These numbers describe broad material structure, not a perceptual or owner-approval score.

All five frozen room pairs, material and special contacts, and wall-fixture integration were independently inspected after export. Real game shader/FOV lighting, all-sector runtime captures, navigation/package regression and owner artistic approval remain lead-owned acceptance. No game launch/build, UI input, audible listening or artistic approval is claimed by this worker.
