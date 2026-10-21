# Art comparison delivery / 21 October 2026

The first owner-approved production gate is implemented: a playable A/B comparison with a shared industrial room, two officer animation pipelines, editable sources, reproducible exports and native recordings. **Owner selection, the 15â€“20-minute quality slice, campaign-wide production and Crown Trick/Hades artistic acceptance remain open.** No purchases were made.

## Review the actual build

The comparison package includes `CompareArt.cmd`. It starts an isolated staged production-engine room. F6 changes painted/rendered appearance without a world action. WASD/arrows move; Alt+arrow turns the body; Ctrl+arrows aim; F fires; L changes the helmet lamp; Tab opens the physical pack; ESC pauses. Rifle and heavy shell are physically underfoot. A live S.I.R.S. occupies the far room. The lab has ordinary finite resources, real collision, power and senses. It is explicitly labeled staged, and is separate from a generated campaign or pacing trial.

`CampaignArtPreview.cmd` starts a fresh ordinary seed-zero campaign in a separate profile with the same presentation options. The normal survival profile, previous releases, save schema, simulation, procedural generator and story remain preserved.

The shared environment has substantial front/longitudinal bulkheads and pressure doors, a quiet four-variant walking surface, interrupted workstations, service mechanism, descent hatch, locker, service cart, pressure manifold and equipment rack. Lab cover fixtures have actual blocked footprints. Filtering gutters are extruded to prevent black atlas seams. Visible floor lighting blends across legitimately observed cells; hidden terrain, actor visibility and actual circuit power remain authoritative.

## What each candidate actually supplies

| Candidate | Supplied | Limits |
| --- | --- | --- |
| Painted | 24 clips; four-facing pistol walk/idle/aim/fire; rifle/suit and rifle/heavy static silhouettes | South uses four independent new paintings held125ms each, not twelve distinct phases. Other cardinal gait and pistol actions preserve original paintings. Rifle motion remains missing. Anatomy/choreography continuity remains unaccepted. |
| Rendered | Original weighted Blender officer/rig; 52 clips,252 RGBA exports; four-facing pistol/suit walk/idle/ready/aim/fire, rifle/suit and rifle/heavy walk/idle/aim, pistol/heavy walk/idle | Current finish reads as stylized3D and needs a more mature illustrated treatment. Heavy/rifle fire, other weapons/armor, machines and broader actions remain incomplete. Native foot plants need owner judgment. |

Missing actions hold an exact matching equipment idle pose only when one exists and display `STATIC ONLY / motion unavailable`. Completely unavailable equipment states are labeled. No pistol motion is substituted for a rifle. The complete candidate/missing matrix is in `assets/art/proof/coverage.json`, copied into the package as `Evidence/ArtCoverage.json`.

Both methods use the same lab, view, reference-cell scale and proof timeline: one cell per0.5-second full stride, with paid ticks unchanged. Authored exposure holds are preserved. Pistol action clips are retimed to a common0.5-second presentation with release140ms after commitment. Authored release/contact/recovery metadata remains available; repeated foot contacts retain ordering. Free aiming plays the rendered ready transition or the painted clip's actual pre-release preparation poses, without emitting a shot. The proof avoids adding a second officer muzzle effect. F6 can switch the displayed method within the current action without resetting the presentation or simulation.

## Technical and native evidence

- 111 tests pass, zero failures/skips: the full48-core suite and63 desktop tests, including authored exposures/repeated markers, shared release retiming, staged physical equipment, stride timing and concealed-origin protection.
- Exported source PNG paths reproduce pixel-for-pixel in padded runtime atlas crops; held poses and duplicate aliases are declared rather than counted as new drawings. Sixteen2048px atlas pages are used. Actual native resource estimate is311,324,068 RGBA bytes, including preserved baseline media. This bounded proof is not a full-game memory budget.
- Identical16-action native movement/fire streams produce **identical entire saved GameState objects** for both methods: tick16,HP100,reserve344,cells8; all AI, RNG, geometry, inventory and speech receipts match. The separate paused F6 test retains byte-identical save files.
- Native720p motion: painted mean16.67ms,p9517.87ms,p9920.06ms,max34.05ms; rendered mean16.67ms,p9517.96ms,p9920.25ms,max30.27ms. Each sample contains1,680 foreground intervals. These scoped observations support approximately60Hz playback on the tested machine.
- Desktop Duplication recordings disable frame duplication and preserve source timestamps: painted955 frames/16s=59.69fps; rendered1,189/20s=59.45fps. They are **video-only**. The rendered viewing copy removes the first3seconds of capture setup. No audio synchronization or perfect60fps recording is certified.
- Resolution/equipment/owner-display observations and exact delivery identities are recorded in the companion receipt. Uncaptured1080p and3440x1440 samples average16.67–16.70ms,p9517.58–17.79ms, with maxima27.38–34.32ms across600 intervals per case. Synchronous native PNG exports can produce a one-time stall; uncaptured pacing and screenshot-export intervals are reported separately.

Normal-speed native clips and human approval are separate from source contacts, hashes, frame counts and tests. The historical native event-poll crash root cause remains open; this art comparison makes no crash-fix claim.

## Sources and reproduction

Art direction: [ART_BIBLE.md](ART_BIBLE.md). Candidate handoffs: [PAINTED_PROOF.md](PAINTED_PROOF.md), [RENDERED_PROOF.md](RENDERED_PROOF.md).

Original generated sources and exact prompts are retained under `assets/art/proof/environment/source` and `assets/art/proof/painted/source`. Painted rejections remain preserved. The built-in image_gen tool was used for these paintings. Layered OpenRaster catalogs organize the actual flattened source paintings without claiming original brushstroke layers.

The original editable officer is `assets/art/proof/rendered/source/origin-war-officer.blend`; model, materials, rig, equipment and walking action remain editable. `PROVENANCE.json` pins the official Blender4.5.3 archive and verified publisher checksum. Blender is an offline authoring tool; no runtime3D library is introduced. No third-party art pack or paid API was used.

Rebuild specialist exports using their documented scripts, then run:

```text
python tools/media/art_proof/pack.py
python tools/media/art_proof/validate.py
gradlew.bat test verifyAssets :lwjgl3:installDist
```

The packer preserves source pixels, bounds, anchors and exposures, adds extruded gutters and writes atlas/coverage manifests. Runtime resources include packed pages and metadata; authoring masters and rejected candidates stay out of the runtime JAR and remain in the source archive. The ordinary campaign's existing media path remains the default.

## Next production gate

The painted appearance currently fits Origin War's mature illustrated identity more closely; the rig demonstrates stronger construction consistency and reusable equipment motion. The recommended direction is to retain the illustrated finish while improving rig-derived construction/materials or using rig renders as controlled painting references. This is an art-direction recommendation, not an owner selection.

After the owner reviews the concrete comparison and chooses the production method, complete the first15â€“20-minute docking/maintenance slice, its full encountered officer/machine/door states, UI/ALICE treatment and synchronized sound. Campaign-wide expansion remains gated on that slice's actual artistic/readability acceptance. The remaining program includes all ten decks and the established ending; this proof does not reduce its scope.
