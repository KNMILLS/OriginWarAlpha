# SquidSquad ecosystem in the atmosphere pass

Owner request,20 October2026: lean into the compatible SquidSquad resources while making the station feel like a dark, low-power institutional interior. This survey inventoried all317 public repositories returned from [Tommy Ettinger's account](https://github.com/tommyettinger?tab=repositories), including forks, and the six [Yellowstone Games repositories](https://github.com/orgs/yellowstonegames/repositories). Inventory is a catalog review, not a claim to have audited every repository's source. The retained catalog identifies every repository and its stated license; repository-level metadata is not an asset license grant.

## Concrete runtime use

| Resource / pinned version | Application |
| --- | --- |
| [SquidGrid4.0.13](https://github.com/yellowstonegames/SquidSquad/tree/v4.0.13/squidgrid) | Existing authoritative FOV; new separate `LightingManagerRgb`/`Radiance` field casts colored fixture light against real wall/door resistances. Render lighting is always clipped to the engine's current visibility and explored terrain. |
| [SquidSmooth4.0.13](https://github.com/yellowstonegames/SquidSquad/tree/v4.0.13/squidsmooth) | Added desktop module. `VectorGlider` interpolates player and legitimately visible actor positions; a new action retargets from the current visual position. It never changes a coordinate in the simulation. |
| [digital0.10.3](https://github.com/tommyettinger/digital) | Existing dependency; `Interpolations.smooth` eases the visual tracks. SquidGrid's `Radiance` uses its continuous wobble for power instability. Reduced distortion disables fixture flicker. |
| SquidPlace / SquidPath4.0.13 | Keep the existing generated station and path interfaces. The visual pass preserves routes, terrain costs, seeded generation and saved games. |
| SquidCore / jdkgdxds / juniper | Existing compatible shared primitives remain pinned. No presentation timing or audio call consumes the live engine RNG. |

The light renderer combines the SquidGrid field with a soft, directional pixel-space beam. It uses strict per-cell vision masks for occlusion, smooth material illumination, a small near-field wash and restrained visor-edge darkening. Fixture brightness can change while the player considers a turn; paid actions and physiological changes still occur only in the engine.

## Other relevant projects assessed

| Project | Finding and use boundary |
| --- | --- |
| [TextraTypist](https://github.com/tommyettinger/textratypist), [FreeTypist](https://github.com/tommyettinger/freetypist), [fontwriter](https://github.com/tommyettinger/fontwriter) | Rich text, animated text and distance-field fonts. Relevant for future authored text effects. The current readable font/measurement path remains coherent through this pass; radio speech does not delay access to subtitles or choices. |
| [colorful-gdx](https://github.com/tommyettinger/colorful-gdx) | Perceptual color tools/shaders. This pass uses SquidGrid's RGB manager directly with libGDX textures; SquidCore already supplies the color helpers required by that path. |
| [anim8-gdx](https://github.com/tommyettinger/anim8-gdx) | Exports animated PNG/GIF and palette images. It is an export tool, not a movement-animation engine; runtime motion uses SquidSmooth. Recorded game frames can be encoded independently without adding an unused runtime dependency. |
| [ColorWeaver](https://github.com/tommyettinger/ColorWeaver), [lesscolors](https://github.com/tommyettinger/lesscolors) | Palette authoring/reduction tools. Art sources and export rules remain explicit in the art manifest; no blanket license assumption for example artwork. |
| [NoiseDemos](https://github.com/tommyettinger/NoiseDemos), [make-some-noise](https://github.com/tommyettinger/make-some-noise), [noisegen](https://github.com/tommyettinger/noisegen), [noisefarm](https://github.com/tommyettinger/noisefarm), [MultiTileBlueNoise](https://github.com/tommyettinger/MultiTileBlueNoise) | Noise tooling/textures. Relevant to material variation and subtle continuous effects. Power instability already comes from the pinned SquidGrid implementation; no new random visual threat or simulation-noise stream is introduced. |
| [SquidGlyph](https://github.com/yellowstonegames/SquidSquad/tree/v4.0.13/squidglyph) / SquidLib demos / Epigon | Useful grid, lighting and game examples. Their glyph/older engine paths are references; the modern textured station retains the current SquidSquad model contracts. |
| [gdx-liftoff](https://github.com/libgdx/gdx-liftoff), [awesome-libgdx](https://github.com/tommyettinger/awesome-libgdx) | Setup and resource discovery. This is an existing pinned project, so the pass extends its build and records exact introduced dependencies. |

Technical references: [LightingManager usage](https://github.com/yellowstonegames/SquidSquad/blob/v4.0.13/squidgrid/src/main/java/com/github/yellowstonegames/grid/LightingManager.java), [RGB lighting](https://github.com/yellowstonegames/SquidSquad/blob/v4.0.13/squidgrid/src/main/java/com/github/yellowstonegames/grid/LightingManagerRgb.java), [VectorGlider](https://github.com/yellowstonegames/SquidSquad/blob/v4.0.13/squidsmooth/src/main/java/com/github/yellowstonegames/smooth/VectorGlider.java), [SquidSmooth dependency contract](https://github.com/yellowstonegames/SquidSquad/blob/v4.0.13/squidsmooth/build.gradle). API calls were checked against the actual resolved4.0.13 JARs and compiled with the pinned libGDX1.14.2/JDK21 setup.

All new references, game assets and generated voice source permissions are recorded separately by the dedicated art and sound workers. Inspiration references describe an emotional/material target; they do not grant rights to copy franchise artwork, recordings, performances or melodies.
