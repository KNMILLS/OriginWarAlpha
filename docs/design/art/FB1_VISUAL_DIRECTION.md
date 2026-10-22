# FB1 visual direction: unsettled and tense

Status: researched proposal for owner review, not an asset replacement or accepted creative direction. Baseline: `18938ecab4ffdc6124fead2c735bc49b408afd5b`. Art only. The owner identifies themself as the original art/sound designer and explicitly states that the intended feeling was **unsettled, with tension**, across the work. That is the controlling creative intent; the original assets are evidence to learn from and potentially restore, not a category to discard.

## What is established, and what is interpretation

The preserved [2016 design](../../../docs/reference/Origin%20War.docx) calls for top-down 2D, SquidLib graphics/animation, a Martian orbital science station, and the named human/android/robot cast. It contains no embedded art or explicit 32px, teal/amber, lighting, or palette specification. The old prototype's glyph presentation is an implementation fact, not proof that ASCII was the desired final art style.

The actual [playerTexture.png](../../../legacy/core/assets/playerTexture.png) is a large 889×1023 RGBA, outlined, shaded illustration: white/gray helmet and armor, dark visor shadow, visible blue eyes, a tense/downturned expression and angular brows. The old renderer loads it as the player image; this establishes its alpha use, while the owner remains authoritative on its intended character identity. The black outline and visor separate a vulnerable human face from protective machinery. Reading that separation as isolation/vulnerability is my interpretation, consistent with the owner's stated intent.

The remake initial [atlas prompt](../../../tools/media/prompts/station_atlas.txt) prescribed native 32px pixel art, controlled steel/teal/amber, a bareheaded brown-haired player, ivory A.L.I.C.E., and small fully readable sprites. Later actor/human production referenced that generated remake sheet. These are new implementation choices, not a restoration of the 2016 illustration. Preserving files and documenting provenance did not establish visual continuity or owner approval of those choices.

[Annotated comparisons](FB1_COMPARISON.md) distinguish directly inspected pixels/code from interpretation. No sound findings are included here.

## Recommended direction: a human inside an indifferent station

Recover the original image's most recognizable identity: the protective helmet/white armor, visor shadow and expressive human eyes. Let the station feel functional but indifferent to that person. Tension should already exist at full oxygen; hypoxia intensifies it rather than introducing the first unsettling image. This is a proposed direction, not a claim that the owner intended every detail below.

| Relationship | Concrete proposed treatment | Guardrail |
| --- | --- | --- |
| Character and silhouette | Test an owner-selected portrait crop from the preserved illustration unchanged, alongside one source-faithful helmeted world sprite. Keep the helmet rim, dark visor mass and white armor legible; do not reduce the face to a generic hair/skin dot. Preserve distinct android/biped/quadruped silhouettes. | Do not invent new costume, enemy lore or a spawn for West. A portrait may carry expression that cannot fit at 32px. |
| Palette and value | Build one shared value ladder: near-black void, muted structural steel, pale worn armor, a few purposeful instrument accents. Keep cyan/amber as restrained functional light, not a decorative edge on most objects. Draft swatches must be sampled/reviewed against original art, not declared canon. | White armor can remain visible without becoming an emissive beacon. Environment can be ominous without making doors or cover ambiguous. |
| Light and space | Compose uneven pools of light from actual lamps/screens, quiet negative space, and falloff toward uncertain corners. Keep some legible unlit structure. Break the current even visible-floor value before adding more detail. | Presentation only: preserve actual FOV, collision, target range and reliable sensor information. Avoid using darkness to hide a required route or telegraph. |
| Surface and age | Reduce small repetitive floor highlights; give large surfaces quiet wear and a few meaningful interruptions. Establish one readable history per room through existing station materials, misplaced equipment or inactive workspaces. | No blanket grunge/noise, gratuitous gore, alien biology or a new story. An empty area can carry more tension than many evenly spaced props. |
| Animation and stillness | Give humans weight with restrained breathing, a guarded head turn or held pause. Machines can remain almost still before a purposeful motion. Use anticipation/hold/release rather than continuous synchronized bobbing. | Never make threat timing less truthful. Telegraphs and recovery remain readable. Animation cannot change world turns/RNG. |
| UI and perception | Keep suit readings, controls and dialogue on stable high-contrast surfaces. Let world composition, silhouette and implication carry unease. Critical hypoxia may distort the world while the HUD remains trustworthy. | This is not a request to dim text, remove the journal/map, disguise real threats as scenery, or turn all phantoms into obvious fluorescent ghosts. |

The current 32px contracts can support a first comparison. They are a delivery constraint, not a creative principle: do not increase the tile size or redesign the renderer until the owner can compare the same scene and identity at actual display size.

Tension also needs contrast. A shelter, a human face or the ending can allow relief without becoming visually triumphant by default. The current warm Mars/destruction vista is a new authored resolution; its balance of grief, survival and hope needs the owner's judgment. This brief does not turn “all of it” into a requirement for constant darkness or constant visual agitation.

## Research and practical use

- **Atmosphere needs a stated emotional target.** Thomas Grip's first-person [9 Years, 9 Lessons on Horror](https://frictionalgames.com/2019-10-9-years-9-lessons-on-horror/) argues for anticipation, implication and narrative context, and evaluating horror against its chosen principles. My application is to ask whether an ordinary full-oxygen room feels uneasy, rather than treating more decorations or more scares as success. This does not authorize removing Origin War's combat or changing its genre.
- **Motivate light before increasing brightness.** Frictional's current [SOMA point-light documentation](https://wiki.frictionalgames.com/page/HPL3/SOMA/Level_Design/Point_Lights) distinguishes fixture appearance, local illumination, radius and falloff, and recommends testing from actual player positions. The transferable 2D proposal is a small, source-shaped presentation light mask inside legitimate visibility; no HPL engine migration or 3D lighting stack is proposed. This is an application/inference from their tool documentation.
- **Keep palette edits deliberate and editable.** [Aseprite color modes](https://www.aseprite.org/docs/color-mode/) explain palette-indexed control; its [animation workflow](https://www.aseprite.org/docs/animation/) supports layered cels, frame durations and onion-skin review. Use those concepts for a small authored color ramp and genuine pose refinement. No Aseprite installation, purchase, or availability is assumed; an equivalent editable source workflow can serve the same purpose.
- **Separate mood from information loss.** Microsoft's [XAG102 contrast guidance](https://learn.microsoft.com/en-us/xbox/accessibility/xbox-accessibility-guidelines/102) supports stable backplates, configurable contrast and clear important cues. Its standard-text 4.5:1 and large-text 3:1 references are useful review targets for the UI, not a mandate to give every world surface equal contrast.

These are primary developer/tool sources read as text on 2026-10-20. No external artwork was copied, no video/audio was played, and no technique has yet been proven in this game by this review. The sources support techniques; they do not establish what this owner finds unsettling.

## Bounded next production choice

**Recommended first comparison: continuity-led identity plus one room, not a whole replacement atlas.**

1. Owner/lead selects the original portrait's identity details to retain, and confirms whether the helmeted look should carry into the world sprite. Keep the preserved original unchanged.
2. On one frozen docking/corridor scene, make at most two review variants: current candidate; a continuity-led helmet/portrait and value/light/surface pass. Scope: one player pose, one matching portrait treatment, three floor/wall surfaces and one light fixture. No registry/model or map-layout change in the art task.
3. Compare at native 32px and actual 64px display, full color and grayscale, with the same HUD/actors/seed. Check paths, door/cover separation, pickups and R.O.V.E.R. countdown independently from mood. Keep before/after captures at normal oxygen; then test one critical/recovered state so the normal look is not mistaken for the hypoxia effect.
4. Owner creative gate: identify the version that feels more unsettled/tense, which specific original qualities it retains, and what feels too safe, noisy or unintentionally cute. A declared preference is more useful than file-count or changed-pixel metrics. Only then extend the selected language to the remaining sectors/cast.
5. Production would retain layered/source files, palette roles and frame timing, with root-owned runtime checks. Static approval, actual in-game readability, animation quality and owner emotional acceptance stay separate.

A second realistic option is to keep the current costumes and focus the first room solely on authored value/light/material hierarchy. That costs less continuity work but leaves the strongest original identity break unresolved. A wholesale ASCII reversion or unreviewed photorealistic/3D shift is not recommended by this evidence.

## Limits and evidence missing

This pass inspected the original design, old renderer/resource mappings, preserved illustration/font sheet, remake prompts/export sources/atlases and existing runtime captures. It did not launch the alpha/remake, animate a preview, generate/repaint an asset, run a build, or review sound. Original layered illustration sources, a complete 2016 visual style guide and an original running-build screenshot were not found in the narrowly inspected preserved tree. No original mood can be reconstructed perfectly from a single image and prototype code.

The code comment above `getPlayerTexture()` still describes a stock tentacle, but its body loads the custom portrait; that stale comment is not evidence that the portrait is third-party. Owner authorship should be recorded at the appropriate asset level, with bundled font/icon provenance handled separately when relevant. Existing generic “legacy rights unverified” language should not be used to dismiss the owner's own artwork or creative decisions.
