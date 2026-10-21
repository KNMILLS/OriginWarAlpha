# FB1 annotated original-to-candidate art review

Read-only observations at baseline `18938ec`. The owner-stated target is **unsettled/tension**. “Observation” below is directly inspected; “interpretation” is a design judgment, not a measured player response. Existing images are linked in place, not modified.

## 1. Player identity and expression

[Preserved illustration](../../../legacy/core/assets/playerTexture.png) • [Remake actor contact](../../../assets/art/animation/review/actors-4x-on-dark.png) • [Initial remake prompt](../../../tools/media/prompts/station_atlas.txt)

- **Observation:** Original is 889×1023, white/gray helmeted armor, strong black contours/visor shadow, blue eyes and downturned mouth. Remake contact top row is brown-haired/bareheaded teal/steel officer, small simplified face, full-body south/north/east/west frames.
- **Interpretation:** The portrait gives a specific guarded person and the barrier of protective equipment; the replacement reads more like a generic capable game unit. Both can be attractive art, but they communicate different relationships to danger.
- **Proposed correction:** Owner-guided continuity study using the original helmet/visor/white armor, first in a portrait plus one native-size world pose. Do not assume the large portrait itself must be squeezed into a 32px cell.
- **Source detail:** [OriginResources.java](../../../legacy/core/src/io/zipcoder/graphics/OriginResources.java), `getPlayerTexture()`, lines 428–439 loads `playertexture.png`; [OriginWarAlpha.java](../../../legacy/core/src/io/zipcoder/OriginWarAlpha.java), lines 223–231 uses it as the player actor. The actual disk filename differs by case. This is source use evidence, not a new run of that old code.

## 2. Prototype grammar versus intentional final identity

[Original font sheet](../../../legacy/core/assets/Rogue-Zodiac-12x24_0.png) • [2016 design](../../../docs/reference/Origin%20War.docx)

- **Observation:** Alpha creates an 80×24 grid with 11×22 cells, narrow bitmap glyphs, `▓` walls, indexed tile colors and FOV/lightness modifiers. A textured player is layered over that grid. The design says top-down 2D/SquidLib but supplies no finished visual palette or embedded images.
- **Interpretation:** Sparse marks and uncertain dark space can leave room for imagination. However, it would be unsupported to call every font/default glyph a deliberate finished art bible.
- **Proposed continuity:** Recover human/space/material relationships, not every prototype limitation. Keep the modern graphical requirement and use the old implementation as one reference, with owner clarification above inferred style.

## 3. Ordinary full-oxygen room: hierarchy and uncertainty

[Docking runtime capture](../../../docs/screenshots/docking.png) — 1280×720; world rectangle approximately (x24–950, y104–568). • [Clear command capture](../../../docs/screenshots/hypoxia-current-clear.png)

- **Observation:** Docking's main world area is filled with a high-frequency repeated panel/rivet lattice at fairly even brightness. Player and nearby object sit near the center. In the command image, rows of cyan wall panels and repeated chairs/consoles are similarly explicit. The UI is a separate clean dark panel.
- **Interpretation:** The rooms communicate legible industrial function more strongly than vulnerable isolation. Repetition and distributed highlights compete with the small actor and make space feel comprehensively catalogued.
- **Proposed correction:** Keep path geometry and UI fixed. Lower quiet-surface detail, introduce a deliberate focal light and larger uninterrupted shadow/value areas, and place visual history through a few existing-material interruptions. This is a proposed composition study, not evidence that “darker” automatically equals “scarier.”

## 4. Renderer light relationships

[Current renderer](../../../lwjgl3/src/main/java/io/zipcoder/originwar/desktop/OriginWarApp.java), `world()`/map drawing; around lines 261–311 • [Sector prompt](../../../assets/art/sectors/source/prompts/sector-kit.txt)

- **Observation:** Visible floor/wall tint has a minimum brightness of 0.36; explored unseen terrain uses 0.16. Actors are then drawn with white batch tint. This is a real lighting separation chosen for readability. The prompts explicitly assign sector colors and materials.
- **Interpretation:** Separating actors from environment helps tactical reading, but can make figures appear pasted on or independent of the space's light. Uniform readability across the entire visible floor reduces lighting's compositional role.
- **Proposed correction:** Test motivated local presentation light and a small controlled actor rim/value separation. Keep a reliable minimum for actionable silhouettes and preserve actual FOV. No model light/visibility rewrite or hidden-threat reveal is requested.

## 5. Hypoxia: existing unease, visibly labeled effect

[Clear](../../../docs/screenshots/hypoxia-current-clear.png) • [Critical](../../../docs/screenshots/hypoxia-current-critical.png) • [Recovered](../../../docs/screenshots/hypoxia-current-recovered.png)

- **Observation:** Same command space gains a purple edge shade and two bright cyan/blue translucent human-like impressions at critical oxygen. Telemetry remains crisp and truthful. Normal/recovered state removes those impressions.
- **Interpretation:** The contrast clearly marks an altered perception mode. It can also make the unreal forms read immediately as a familiar “ghost effect” rather than an unsettling uncertainty. That emotional effect has not been measured; owner judgment matters.
- **Proposed correction:** Tune the perceptual art language only after approving the normal-oxygen room. Test partial form/edge absence/material echoes rather than simply increasing opacity, shaking or darkness. Keep sensor truth, required-information visibility and reduced-distortion support.

## 6. Surface and animation authoring

[Actor contact](../../../assets/art/animation/review/actors-4x-on-dark.png) • [Human contact](../../../assets/art/animation/humans/review/humans-4x-on-dark.png) • [Packing source](../../../tools/media/pack_animation.py)

- **Observation:** Art comes from generated high-resolution sources cropped/reduced with Lanczos and finally upscaled nearest 4×. Pair-difference/alpha checks prove export properties, not deliberate pixel clusters, expression or acting. New humans have subtle idle pairs; the renderer uses a shared presentation clock with offsets.
- **Interpretation:** Attractive rendered highlights and constantly cycling small poses can feel busier or more toy-like than the source portrait's guarded stillness. This is a subjective risk, not a blanket rejection of generated art or proof every frame is defective.
- **Proposed correction:** Hand-review one small pose at native size for coherent clusters and weight. Define holds/anticipation/frame duration before expanding. A palette-controlled editable workflow gives the owner concrete features to direct, rather than more prompt variants.

## 7. Preserve the UI separation

[Inventory UI200%](../../../docs/screenshots/inventory-ui200.png) • [Dialogue UI200%](../../../docs/screenshots/dialogue-ui200-spoiler.png)

- **Observation:** Reliable telemetry, page controls and enlarged text sit on stable backplates; explicit objectives remain available.
- **Interpretation:** These support comprehension and should not be sacrificed to manufacture unease. Tension can come from atmosphere and implication while the player knows what an action costs.
- **Proposal:** Evaluate world composition with the current readable UI held constant. Any contrast/accessibility changes need their own root-owned verification; this brief does not certify current contrast ratios.

## 8. Ending and emotional contrast

[Current enlarged epilogue](../../../docs/screenshots/ending-ui200-spoiler.png) • [Finale illustration](../../../assets/art/finale/finale.png)

- **Observation:** The remake depicts the station's distant amber destruction, a departing pod and warm Mars horizon; the epilogue text uses a large stable dark backplate. The original design establishes sacrifice, broadcast and pod escape, but supplies no ending illustration to match.
- **Interpretation:** The image can express survival and release, but its heroic beauty could also soften grief or unresolved unease. This is an owner-facing creative choice, not a proven mismatch merely because it has warm light.
- **Proposal:** Keep the canonical events and readable epilogue. Review the balance of loss/relief using the same original-character visual language before commissioning a replacement. No title-screen spoiler or new ending is proposed.

Static visual review only; no new image, playback, runtime or owner emotional acceptance is claimed.
