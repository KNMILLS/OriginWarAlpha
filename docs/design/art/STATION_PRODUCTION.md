# Opening station hardware

Development candidate for the owner-selected Blender-driven illustrated pipeline, beside the retained painted station kit. This delivery covers hardware used on the first two decks. It is not acceptance of commercial artistic quality or native animation.

## Authority and identity

`docs/directives/SURVIVAL_HORROR_20261021.md` controls functional industrial horror, tangible equipment, reliable cues and real local state. The newer owner-selected illustrated Blender pipeline replaces the earlier hand-painted officer method. Floors, walls and workplace dressing retain the painted environment kit.

The original project SIRS, camera, pressure door, hatch and switch images were inspected before construction. Their dark metal, worn sage and institutional ivory, asymmetric forearm weapon, cabinet access panels and physical pressure equipment guide this original model. No outside mesh, texture or animation was used. No franchise asset was copied. DUSKERS and ROUTINE remain experiential direction references, rather than source assets.

## Development export

- SIRS: four independently rendered cardinal neutral bearings, a real weighted mechanical armature, a recessed single sensor, asymmetric right-arm cannon, rear service cabinet and structural hips/knees/feet.
- North door: closed pressure leaves and physically retracted open aperture, with dark instrument recesses. The aperture is transparent; there is no painted background hiding a closed door. Jambs overhang only the known adjacent wall positions; the declared mechanical footprint remains one door cell.
- Runtime PNGs are rendered directly at 192 pixels per logical reference cell. Separate 256-pixel-per-cell authoring masters and the editable `.blend` are retained. Production art is not upscaled by the export tool.
- `review/pilot-sirs-door-100px-60px.jpg` shows actual pixels at two small sizes. The development SIRS look and root-translated gait pilot were approved by the lead for bounded opening expansion. The door frame and leaf aperture were physically widened and raised for clearance, preserving the threshold pivot and one-cell mechanical footprint. Actors were not scaled to fit. `north-door-actor-clearance.jpg` checks both actual actor sprites at common cell scale.
- SIRS full set adds calibrated locomotion, stationary start/settle, anticipation, fire/recovery, hit and a rigid hinged collapse. Camera has four bearings and separate on/off/aim/disabled states. Doors have north/east orientation, opening/closing, a physical lock/brace and forced-open broken release. Switch power/lever variants, two one-cell workplaces and a wall placard complete the bounded kit.

## Runtime contract

`clips.json` contains explicit actor/action/facing bindings. SIRS actor is `sirs`, with actions `idle`, `walk`, `anticipate`, `fire`, `hit`, `destroyed`. Camera actor is `camera`, with actions `on`, `off`, `aim`, `disabled`. Door actor is `door`, with actions `closed`, `open`, `opening`, `closing`, `locked`, `braced`, `damaged`.

North doors have a horizontal frame (walls left/right, passage north/south). East doors have a separate model render rotated 90 degrees under the same fixed camera (walls above/below, passage east/west). The threshold remains on the map north/south axis. Its authored structural cutaway retains both low jamb bases and threshold while omitting the tall jambs/header. Retracted leaves are hidden only inside those cutaway housings. Closed leaf obstruction remains visible. The intact geometry stays editable in the source. An east door never borrows a north image or changes camera to impersonate a north doorway.

Core alone selects bearing, movement, anticipation, attack, damage, circuit power and door state. Neutral and anticipation artwork do not perform a looping tactical scan. The actual SIRS aiming flag starts anticipation. `fire` has an authored `release` marker for sound/effect alignment. Camera `off` and `disabled` remain dark. An attack flash, if used, occurs on the release frame only. No constant false powered indicators are baked into the diffuse door art.

`damaged` is a forced-open door with broken release hardware and cannot visually reseal. `braced` is physically closed with a real obstruction brace. Painted lock labels may identify the lock without asserting powered emissive state. Mechanism power/release variants must be selected from actual circuit and mechanism state.

Each region declares logical cell bounds, bottom-left pivot, actual raster size, path and SHA-256. SIRS pivot is the projected world ground/body-gravity anchor. Door pivot is the projected center of its threshold. Root owns atlas packing, state selection, runtime timing and native evidence. No runtime 3D is introduced.

Drawbounds acquire transparent quarter-cell padding from evaluated geometry when an action exceeds the neutral canvas. Camera orthographic extent follows the larger canvas dimension, preserving actual pixels per world unit. This fixes release/collapse clipping without shrinking the machine or changing the world anchor. Master renders use identical logical bounds at 256 pixels/cell.

The final SIRS gait advances 0.5 cell over a 0.25-second cycle, matching the actual 0.5 second/cell machine speed. Twelve unique poses are sampled at 48 Hz and have left/right contact markers at frames 0/6. Four cardinal projected source calibrations convert local stance travel into actual root countertravel. This is a compact, rapid mechanical gait; its native weight and small-scale clarity remain review items. Authored `start`/`stop` are stationary two-placement clips. They bridge neutral feet and gait phase 0 support positions; they do not prove seamless interruption at arbitrary gait phases. Runtime root placement, contact timing, transition selection and any interrupted tail still need direct engine review.

No body sine bob is added. Chassis clearance during placement is solved from actual authored foot targets and fixed-length legs; the feet are not clipped or links stretched to pass reach checks. The cabinet rotates rigidly during collapse. The fire flash is a modeled volume visible on release frame 2 only; it is not an idle light or borrowed recoil overlay.

## Rebuild and evidence

Run the bundled Blender executable in background with `--python tools/media/station_production/build.py -- --full`, then run `review.py` and `validate.py` with Python/Pillow. `--pilot` renders the six-still look pilot; `--motion-pilot` adds south/east locomotion. The `.blend` preserves modeled objects, rigid weights, materials, armature, named source actions, light/camera and authoring settings. The builder plus `motion.py` is the reproducible editable construction source; it performs offline model rendering, never code-painted bitmap repair.

The final doorway corrections used `--full --door-cutaway-only`: they regenerated every door image and retained the byte-identical full05 non-door images after hash checks. This is a narrow iteration shortcut for this unchanged non-door construction. Ordinary `--full` renders every asset. Complete full05 rendering took approximately 197 seconds by the retained log timestamps; the original phase timer was replaced during a subsequent iteration. The first cutaway correction took 62 seconds; subsequent release corrections have their own retained timers/logs. These figures exclude authoring and failed/interrupted earlier renders. They are not total task cost.

`source/PROVENANCE.json` records the exact project reference paths, source tool, script hash and blend hash. Render logs retain the actual tool output. `review/iterations/pilot01` preserves the first construction/export failure: four bearings initially did not rotate the weighted objects and the display palette was too pale. Pilot 02 parents the weighted meshes to the rig and converts the intended palette into linear node colors. The four resulting renders differ; this check is structural evidence, not an artistic grade.

Two rigid-leg reach rejections are retained in `review/iterations/rigid-reach-rejections`. A guessed chassis settle did not preserve reach during stationary start/stop. The corrected hypothesis solves the maximum allowable chassis height from both foot placements before posing fixed-length linkages. No acceptance threshold was relaxed and no unreachable target was silently clamped. `pilot02-approved-look` preserves the actual approved still source before expansion.

`review/iterations/clipping-and-east-door` retains clipped release/collapse renders and the unreadable literal edge-on east frame. Final export adds honest transparent drawbounds and the described foreground cutaway. Intermediate render logs remain source evidence, including interrupted exports; only a completed final export and the current manifest are authoritative.

The damaged-door corrections replaced an incorrectly world-rotated floating lever with an explicitly modeled angled lever attached to its release housing. In the east structural cutaway, the high release housing is cut away with its jamb; an attached snapped release stud is visible at the retained low base. No high hardware is left floating after its support is omitted. The door remains physically open and its aperture stays clear.

No purchases, external uploads or source replacements were made. Technical validation, native play, human readability/motion review and owner artistic approval remain separate. Source review boards and calibrated metrics do not certify native motion or commercial finish.

## Bounded remaining scope

The authorized opening export includes a one-cell inspection desk and one-cell maintenance bench intended only for already occupied COVER footprints, and a wall-mounted department placard without a floor footprint. Existing painted locker, rack, service cart and life-support modules are reused. No ADD, ROVER or Constructor campaign kit is authorized here. Integration, native motion, full opening composition and owner finish review remain with the lead/owner.
