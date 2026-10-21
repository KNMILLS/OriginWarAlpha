# Runtime notices and source materials

These upstream licenses apply to their named components. They do not assign a new blanket license to Origin War, the original 2016 material, or the owner's game assets. Original game/design credits: Ken Ragonese and Evan Hitchings. New artwork and synthesized score/effects were produced for this implementation; see assets/ASSET_PROVENANCE.md and assets/AUDIO_PROVENANCE.md. Legacy runtime/media are excluded from the modern package.

| Shipped component | Version | Applicable notices |
| --- | --- | --- |
| libGDX core, LWJGL3 backend, desktop natives |1.14.2| Apache2.0: libgdx/LICENSE and AUTHORS; original distinct source grants/copyrights retained in libgdx/SOURCE_HEADERS.txt, including Matthias Mann2008–2010 BitmapFont BSD3 |
| gdx-jnigen-loader |2.5.2| Apache2.0, gdx-jnigen/LICENSE and AUTHORS from exact tag commit be21532209a3735c01c2a804a0e48d1a20dbb0de |
| SquidSquad core/grid/path/place |4.0.13| Apache2.0: SquidSquad/LICENSE; source commit883a335218c8beffc41c95b0263881910928150b. Upstream has no root AUTHORS file despite representative header references. |
| digital |0.10.3| Apache2.0, digital/LICENSE and AUTHORS |
| funderby |0.1.2| Apache2.0, funderby/LICENSE; source attribution2024, see upstream AUTHORS references |
| jdkgdxds |2.1.9| Apache2.0, jdkgdxds/LICENSE and AUTHORS |
| juniper |0.10.8| Apache2.0, juniper/LICENSE and AUTHORS |
| crux |0.1.3| CC0, crux/LICENSE |
| regexodus |0.1.21| BSD3, regexodus/LICENSE; Sergey A. Samokhodkin2001 |
| JLayer |1.0.1-gdx| Exact source headers GNU Library GPL2-or-later; published POM LGPL2.1. LGPL2.1 text and exact source JAR accompany the package. |
| JOrbis/JOgg |0.0.17| GNU Library GPL2-or-later; JCraft/ymnk2000. Original GNU Library GPL2 text is retained under openal/COPYING and lwjgl/openal-openal_soft_license.txt; exact source JAR accompanies the package. |
| LWJGL core/GLFW/jemalloc/OpenAL/OpenGL/STB bindings and natives |3.4.3| lwjgl/LICENSE.md (BSD3) plus all component-specific notices listed below |
| OpenAL Soft, LWJGL-patched build |1.25.2| openal/COPYING, BSD-3Clause, LICENSE-pffft, fmt-11.2.0-LICENSE and gsl-LICENSE; patched source archive includes build configuration |
| OriginWarUI bitmap font; historical LSans15 |Liberation Sans2.1.5 derivatives| font/LICENSE-OFL-1.1; Google2010 / Red Hat2012. Retained exact Regular TTF and export/hash record: assets/ui/manifest.json in game source. OriginWarUI is the renamed60px bitmap used by the current UI; historical LSans provenance https://github.com/libgdx/libgdx/pull/6727. |
| Eclipse Temurin runtime |21.0.12.1+1| temurin/NOTICE, RELEASE_IDENTITY.txt and the entire package runtime/legal directory, including GPL2+Classpath, assembly exception and third-party notices |

LWJGL's native payloads additionally retain libffi/liburing MIT, GLFW zlib, jemalloc BSD2, Khronos MIT and the bundled STB header license blocks under lwjgl/. LWJGL BSD alone does not replace these notices. libGDX's upstream CC-LICENSE is also retained with its original source archive. MANIFEST.json identifies exact collected payloads, primary sources and SHA256 hashes. Scripts under tools/distribution reproduce this collection without executing downloaded code.

The distribution's DependencySources directory contains matching JLayer/JOrbis source JARs, Temurin's published matching source tarball, the patched OpenAL source/build workflow, and full libGDX/LWJGL source archives. SOURCE_MANIFEST.json records immutable identifiers and hashes. OpenAL's shipped Windows x64 DLL SHA256 is ff0671cba224947ddfd5d338ebc1f8ce502be7aea5cbd599d46cd74473dea0e3. Its embedded first-parent identity7f9c6de1162b1d4604b9a2f8bcd6c53021566221 is explained by the pinned build commit b874cc37a1ea0c094e7d369d892d37d2cda29239, whose workflow supplies the actual source patches and compiler options. All installed OpenAL classifier JARs embed that same first-parent identity. Bit-identical recompilation of vendor binaries was not performed.

JARs and native libraries remain separately replaceable. Reverse engineering/debugging needed to modify the LGPL libraries is not prohibited by this project. See REPLACEMENT.md for practical replacement/build instructions. Keep these notices, source materials, runtime/legal and vendor attributions with redistributed copies, under each component's terms.
