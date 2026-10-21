# Decisions

- D001: Preserve the two unversioned 2025 Godot experiments privately; neither is a SquidSquad baseline. Do not import later lore/mechanics.
- D002: SquidSquad 4.0.13 supersedes the directive's candidate 4.0.12 after tagged API/POM verification. Use squidgrid, squidplace and squidpath. libGDX 1.14.2/LWJGL3; Gradle 8.14.3; portable JDK 21.
- D003: Original 215-commit baseline is recoverable and restore-verified. GH007 blocks the archive branch; the annotated tag is remotely verified. Preserve the blocker; do not alter privacy settings or historical authors.
- D004: Keep original active build/source/assets under legacy/ and exclude it from modern Gradle. Rights for legacy media remain unverified, so new gameplay uses new media.
- D005: Presentation receives copied state and independent RNG. The save DTO includes authoritative simulation plus deterministic perception scheduling; graphics never owns resources or turns.
- D006: JDK 17 exists through an inherited JAVA_HOME despite no Java PATH entry. Modern builds use isolated JDK 21; no global toolchain changes.
- D007: First source-archive command used an incompatible PowerShell/native argument form and failed before writing an archive. Corrected to a separate output argument; extracted files subsequently match all 582 tracked hashes. Preserve the first failure in private execution history.
- D008: GL framebuffer rows require explicit PNG Y-flipping. Retained the inverted first capture, fixed export orientation and inspected a fresh actual runtime capture. This does not imply a rendering-layout problem or gameplay acceptance.
- D009: Native 32px world art defaults to2x physical presentation for readable silhouettes, with Z toggling1x overview. Camera coordinates are pixel-aligned; UI is independent.
- D010: Share engine state validation with save acceptance so malformed primaries reach backup fallback. A new expedition initializes both save candidates with its own identity; never retain another run's backup. Regressions cover both cases.
- D011: Overlay entry removes underlying hitboxes and cancels pending walking. World-affecting inventory choices return to gameplay to expose consequences. Enable backend pauseWhenLostFocus explicitly rather than relying on its false default.
# M3/M4 presentation and authored content

Perception is a pure projection of saved perception seed/tick and oxygenation. It never advances any RNG or adds real actors/items/quest writes. Slow vignette/drift and visible empty-cell impressions preserve reliable suit/inspection controls; reduced distortion removes drift. Empty legal shots will still cost ammo/time in M2, avoiding a free reality test from rejection. Authored campaign choices are engine-owned mutations; dialogue itself is free, while a real oxygen transfer costs a world turn. Both Constructor outcomes must consume actual engine defeat state.

Independent review corrected inventory/new-run and deck-target carryover, long-text paging and enlarged layouts. Native framebuffers, not intermittently occluded desktop helper screenshots, establish OpenGL visual evidence. Successful audio playback/loop counters remain separate from actual listening.

Final presentation requirement pass: K opens a whole explored-deck map (unknown cells and unseen threats withheld), with mouse access in Pause. A paged offline expedition record exposes outcome/seed/ticks/build/resources/discoveries/Constructor/crew/fleet statistics, accessible in Pause and after victory/death. These screens read copied truth and pause world actions. No invented persistent score currency, leaderboard service or metaprogression. Optional --view=map/record after normal start/resume supports the same screens for isolated framebuffer verification; it changes no world state.
