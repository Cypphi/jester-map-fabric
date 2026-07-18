# Jester Map: temporary dungeon-map research

Status: working notes, not a final specification. Inspected 2026-07-18.

This document records the useful ideas found in the local OdinClient, Stella, and Skyblocker reference repositories, plus the current upstream Odin map implementation that OdinClient modifies. It describes behavior and architecture; it is not permission to copy reference code or data.

## Executive summary

A strong dungeon map should treat detection and rendering as separate systems. Detection builds a small, authoritative dungeon model from several imperfect inputs. Rendering consumes an immutable snapshot of that model and never scans the world itself.

The Catacombs clear map is fundamentally a graph over at most 36 room cells and 60 connections:

- Physical room cells lie on a 32-block grid. A cell is effectively a 31-block room plus its one-block connection boundary. Common implementations anchor the dungeon around `(-200, -200)` and sample cell centers around `(-185, -185)` through `(-25, -25)`.
- The Hypixel map item is 128 by 128 pixels. A room is normally 16 or 18 pixels wide and adjacent room origins are separated by room width plus a four-pixel connection area.
- A logical room can own one, two, three, or four cells. Its cell set determines `1x1`, `1x2`, `1x3`, `1x4`, `L`, or `2x2` geometry.
- The map item is best for live topology, visibility, room type, checkmarks, doors, and remote player decorations.
- Loaded chunks are best for identifying exact room names, resolving rotations/local coordinates, and discovering information before or independently of a map update.
- Tab-list, entity, chat, action-bar, scoreboard, and dungeon lifecycle observations supply party identity, deaths, secret counts, score inputs, and reset boundaries.

The recommended Jester design is a hybrid: Odin-style map topology plus cheap chunk-column fingerprints for the fast path, with a more discriminating incremental matcher available when a fingerprint is unknown or ambiguous. Keep “what the client knows” separate from “what the selected visibility policy is allowed to show.”

## What each reference contributes

| Reference | Strong ideas | Limitations to avoid |
| --- | --- | --- |
| Upstream Odin / OdinClient | Clean 36-cell topology, packet-driven map parsing, chunk-load room fingerprints, room/door visibility, room prediction, and simple live player fallback. OdinClient demonstrates that hidden-room knowledge and presentation policy are separable. | Rendering is composed from hard rectangular fills. A single Java string hash is fast but needs collision/unknown handling. OdinClient itself does not own the scanner; it injects into upstream Odin. |
| Stella | The most complete end-to-end hybrid in the local references: calibrated map, world scanner, component merging, room metadata, rotation, player interpolation, clear attribution, predictions, and score integration. | Several systems are tightly coupled to global objects and config. Some scanner logic is explicitly adapted from GPL-3 code. Its renderer is still rectangle-based and performs lots of small draw calls. |
| Skyblocker | Robust physical/map coordinate calibration, map flood-fill for multi-cell rooms, resource-backed structural matching, rotation candidates, double-checking, secret tracking, event-driven texture updates, and clear reset behavior. | Room matching is heavier, currently centered on the room occupied by the player, and its map display mostly modifies/reuses the vanilla 128-pixel texture. Some room assets derive from GPL-3 DungeonRoomsMod data. |

## Detection model

### 1. Establish the run and coordinate systems

Reset every run-owned object on world/server change, dungeon exit, and boss transition. Do not let room, map-id, or teammate state leak between runs.

Maintain three explicit coordinate spaces:

1. World space: Minecraft `x/z` block or floating-point coordinates.
2. Dungeon grid space: integer cells `0..5` in each axis, indexed as `z * 6 + x`.
3. HUD/map space: calibrated pixel or normalized coordinates used only for presentation.

Skyblocker finds the physical entrance from Mort and snaps it to the 32-block grid. Odin and Stella use the known Catacombs grid origin directly. Jester should support both: use a known grid transform as the normal path, then verify it against an observed entrance/map transform. A disagreement should be diagnostic state, not silent coordinate corruption.

The map transform must be calibrated from actual pixels rather than permanently assuming a floor layout. Odin and Stella locate the entrance-colored run, measure its width (`16` or `18`), derive the four-pixel gap, then compute the room-origin modulo. Skyblocker starts near the self map decoration, finds the entrance region, walks to its northwest corner, and measures its width. The latter is a useful fallback when a full linear scan is inconclusive.

### 2. Parse map topology incrementally

Process map packet/color changes, not every render frame. A full 16,384-byte pass is already cheap when packet-driven, but the resulting domain update should touch only changed observations.

Centralize the observed packed map-color constants. The references currently use:

- normal `63`
- entrance/green check `30` (context distinguishes them)
- puzzle `66`
- trap `62`
- miniboss/champion `74`
- fairy `82`
- blood/red check `18` (again contextual)
- unknown/unexplored `85`
- white check `34`
- question/wither-door black `119`

For each possible room cell, sample a stable room pixel and its center. For each east/south connection, sample the connection center and a side pixel. A non-empty narrow center with empty sides is a door; a filled bridge between adjacent same-colored cells means those cells belong to one logical room.

Build logical rooms using a small flood fill over the 6-by-6 cell grid. Merge all connected cells of the same observed room type whose connecting bridge is filled. Store one `RoomId` per logical room and point every owned cell at that ID; never clone room state into each cell.

Doors are graph edges, not miniature rooms. Store their two endpoint cell indices, orientation, observed type, open/closed state, and discovery state. There are at most 60, so dense arrays are practical and predictable.

Map checkmark interpretation is contextual:

- no recognizable center mark after discovery means uncleared;
- `34` means white clear;
- `30` means green clear;
- `18` at the center means failed, while `18` as the room body is blood;
- `119` can represent an unexplored/question state or a wither door depending on the sampled location.

State transitions should be monotonic where Hypixel semantics allow it. For example, a green room should not regress because one partial packet temporarily lacks its checkmark. Record observation source and sequence/time so reconciliation rules are testable.

### 3. Identify exact rooms from loaded world data

Odin and Stella use a very fast “room core” fingerprint. At one stable `x/z` column in a loaded room component they scan vertically, find the roof, serialize block identities from roughly Y 140 down to 12, ignore known unstable blocks such as chests and planks, then hash the sequence. Metadata maps one or more core hashes to a room name, type, shape, secret count, crypt count, and other facts. Multiple hashes per room are needed because multi-cell rooms can expose different component columns.

This is an excellent fast path because the total work is bounded and naturally triggered by chunk load. It should be implemented as Jester-owned code with explicit normalization rules and tests. Do not rely on only the 32-bit hash:

- keep a compact stronger digest or the normalized signature for collision diagnostics;
- validate the matched metadata against observed room type and geometry;
- support multiple signatures per room and data revisions;
- report unknown signatures without spamming chat/logs;
- never rescan a settled component unless its chunk/data generation changes.

Skyblocker demonstrates a stronger fallback. It stores sorted structural block signatures for known rooms, begins with candidates for each valid rotation, and scans a bounded area around the player. Every informative block eliminates candidates whose rotated local signature lacks that block. When one candidate remains, it continues for ten more informative blocks before confirming. Doorway blocks are excluded because they are not reliable room identity.

Jester should not copy Skyblocker’s room assets or implementation. The architectural lesson is useful: ambiguous/unknown fast fingerprints should enter a candidate-elimination state rather than becoming `Unknown` forever. To keep this fast and thread-safe, capture only the required immutable block observations on the client thread and perform pure candidate filtering off-thread. Do not read the live Minecraft world from an arbitrary worker.

### 4. Resolve room shape, rotation, and local coordinates

Shape comes from the logical room’s component set:

- one component: `1x1`
- two: `1x2`
- three: straight `1x3` or `L`
- four: straight `1x4` or `2x2`

For straight and L-shaped rooms, component geometry narrows or completely determines orientation. For `1x1` and symmetric `2x2` rooms, Odin/Stella probe blue terracotta at roof-level corners. Once the canonical corner and quarter-turn rotation are known, expose two pure transforms: world-to-room-local and room-local-to-world. Secret routes and future overlays should depend only on those transforms, not duplicate rotation switches.

Use a special-case metadata rule only when the world truly lacks the normal marker (the fairy-room fallback is an example in both Odin-family designs). Keep such exceptions in the room identity/rotation boundary.

### 5. Discovery and visibility are different state

The model may know a room from loaded chunks before Hypixel makes it visible on the map. Odin represents viewability as “walked into or map-discovered,” while OdinClient’s CheaterMap mixins optionally override that at render time and darken hidden rooms/doors.

Jester should model this deliberately:

- `knowledge`: unknown, topology-only, typed, identified;
- `discovery`: undiscovered, hinted, map-visible, entered;
- `clearState`: uncleared, white, green, failed;
- `presentationPolicy`: which knowledge/discovery levels may be rendered and how.

That prevents detection rules, user policy, and visual styling from becoming the same boolean. It also makes a legitimate/default policy easy to audit.

### 6. Teammates and motion

Use the tab list to create a stable five-slot party roster with name, UUID when known, class, and alive/dead state. Map decorations do not directly identify teammates. All three implementations rely on the convention that the frame decoration is self and remaining decorations correspond to living teammates in party order. Treat this as a best-effort association and log a bounded diagnostic when counts diverge.

For position:

- prefer the live player entity when loaded, because it gives precise world position and yaw;
- otherwise use the associated map decoration;
- convert both through the same world/grid/HUD transform;
- interpolate position and yaw from timestamped samples, with wrap-aware angle interpolation and a snap threshold for teleports or stale samples.

The renderer should receive already-smoothed marker poses. It should not know about map decorations, entities, or tab order.

## Recommended Jester architecture

The data flow should be one-way:

`Fabric/Minecraft adapters -> observations -> DungeonRun reducer -> immutable MapSnapshot -> layout -> render commands`

Suggested shared packages and responsibilities:

- `dungeon.domain`: `DungeonRun`, `DungeonGrid`, `DungeonRoom`, `DungeonDoor`, stable IDs, states, and coordinate transforms.
- `dungeon.map`: map calibration, pixel sampling, topology flood-fill, checkmark/door observations, and teammate decoration observations.
- `dungeon.world`: bounded chunk scan scheduling, core fingerprinting, fallback observations, and rotation probes behind a narrow world-reader interface.
- `dungeon.registry`: versioned Jester-owned room metadata and fingerprints. Loading/validation is separate from matching.
- `dungeon.party`: tab roster, decoration association, entity override, and interpolation.
- `dungeon.state`: a single reducer/reconciler that owns run lifecycle and produces snapshots. Inputs may arrive from different hooks, but mutations converge here.
- `dungeon.presentation`: visibility policy, map layout, labels, animation targets, and render-command construction.
- `render.minecraft`: the thin 26.x backend and Fabric HUD hook.

Mixins and Fabric callbacks should only extract data, translate Minecraft objects to shared observations, and submit them. They must not own room merging, fingerprint policy, configuration, or HUD layout.

The render snapshot should contain only plain values: room contours and styles, door paths, labels, marker poses, score/status text, and animation keys. Publish it atomically after reconciliation so rendering never locks the scanner.

## Rendering direction: a modern HUD, not colored rectangles

Jester should explicitly reuse and adapt Cypphi’s rendering code from `~/Projects/SPECTRUM/spectrum-client`. In particular, the useful foundation is:

- `src/main/java/dev/cypphi/spectrum/ui/render/Renderer2d.java`
- `RenderBackend2d`, `ShapeCommand`, `ShapeType`, and `ClipRect`
- `src/client/26x/java/dev/cypphi/spectrum/ui/render/MinecraftRenderBackend2d.java`
- the extraction lifecycle in `HudRenderEventBridge.java`

Port the required pieces into Jester as an internal rendering subsystem; Jester should not require Spectrum to be installed. Preserve the shared-command/thin-version-backend split. The current 26.x backend is extraction-safe and already provides antialiased rounded shapes, outlines, shadows, ellipses, polygons, lines, and clipping.

The existing rounded-rectangle primitive is a starting point, not the final room renderer. Drawing one rounded rectangle per cell would leave seams and still look like a softened version of the old maps. Add a continuous orthogonal-contour/path primitive:

1. Trace the outer boundary of a logical room’s occupied grid cells.
2. Remove internal cell edges.
3. Round both convex and concave corners consistently.
4. Tessellate one fill/outline mesh per logical room, or render it through an equivalent signed-distance/mask path.

Doors should be short rounded capsules or tapered connectors integrated visually with adjacent room contours. Player markers should use circular/cutout heads with a subtle class ring, soft elevation shadow, and smoothly interpolated pose; the local player can have a restrained directional accent rather than a vanilla map arrow.

Recommended layer order:

1. soft panel shadow;
2. translucent rounded HUD surface, optionally using the available Minecraft backdrop blur path;
3. room shadows/ambient separation;
4. continuous room fills with a restrained palette and state-dependent luminance;
5. doors/connectors;
6. checkmarks, secret progress, and room labels;
7. teammate markers and leap names;
8. compact score/status strip.

Animation should be semantic and short: spring/ease room discovery opacity and scale, cross-fade state colors, animate marker motion from samples, and avoid perpetual glow/noise. Cache room contour meshes by component set and style geometry. A color/checkmark change must not rebuild topology.

The future custom font renderer should fit behind a small text measurement/draw interface. For now, use Minecraft text only through that interface so replacing it does not affect room layout or state code.

## Standalone configuration system

The Jester config system should be entirely standalone: no Odin, Stella, Skyblocker, Spectrum module/config, Cloth Config, AutoConfig, or MidnightLib dependency. Rendering code may be reused from Spectrum, but configuration must remain a Jester-owned subsystem with no renderer or Minecraft types in its persisted model.

Suggested design:

- Immutable records such as `JesterConfig`, `MapHudConfig`, `PaletteConfig`, `MarkerConfig`, and `VisibilityConfig`.
- `ConfigDefaults` as the single authoritative default owner.
- `ConfigCodec` for JSON DTO encoding/decoding.
- `ConfigMigrator` keyed by a required schema version.
- `ConfigValidator` that clamps numeric values, repairs invalid enums/colors, and returns diagnostics.
- `ConfigRepository` for load/save/reload only.
- `ConfigService` holding the current immutable snapshot and notifying explicit listeners after a successful replacement.
- UI code edits a draft and commits it through the service; it never mutates the live record field by field.

Persist to `config/jester_map/config.json`. Save atomically by writing a sibling temporary file, flushing/closing it, and replacing the destination. Debounce rapid editor changes, keep a recoverable backup of the last valid file, and fall back to defaults with one clear diagnostic when parsing fails. Tests should cover defaults, malformed input, validation, every migration, round trips, and atomic-save failure behavior.

Stable serialized keys are part of the public configuration API. Visual defaults and room/map data versions are separate from the config schema version.

## Performance rules

- No world scanning or topology work during HUD extraction.
- Parse map state on relevant packet/update events; coalesce bursts by map revision.
- Scan each relevant loaded room component once. Maintain a bounded queue and a per-run scanned-component set.
- Never scan the entire dungeon volume. Core columns are the fast path; fallback observations are targeted and incremental.
- Avoid Stella/Skyblocker-style live-world access from general async workers. Snapshot on the client thread, compute on workers, and apply results back through the reducer.
- Use primitive/dense collections for the fixed 36-cell/60-edge graph where it improves clarity.
- Cache immutable room geometry and label layout. Batch render commands by layer/material where the backend permits it.
- Add counters/timers behind a debug flag: map parses, components scanned, unknown/ambiguous fingerprints, snapshot revisions, contour rebuilds, render command count, and CPU time.

## Testing strategy

Most of the system can be tested without Minecraft:

- captured/synthetic 128-by-128 map arrays for calibration, rooms, doors, and checkmarks;
- property tests for world/grid/map transforms and rotations;
- component sets for every supported shape and contour output;
- room-observation reconciliation and non-regressing clear state;
- fingerprint normalization and metadata collision/unknown cases;
- teammate interpolation, yaw wrapping, stale samples, and teleports;
- visibility policies against the same underlying knowledge state;
- config load, validation, migration, and atomic persistence.

Keep a small redacted capture format for real dungeon observations so regressions can be reproduced without launching Minecraft. Integration checks should verify only adapters, packet extraction, chunk-load scheduling, and HUD backend wiring across all three Stonecutter targets.

## Licensing and provenance warning

Jester Map is MIT. The local OdinClient checkout and current upstream Odin are BSD-3-Clause, while Stella and Skyblocker are LGPL-3.0. Stella’s scanner files also state that portions are adapted from GPL-3.0 DungeonScanner.js, and Skyblocker attributes many skeleton/waypoint assets to GPL-3.0 DungeonRoomsMod.

Therefore:

- use the references to understand protocols, invariants, failure modes, and architecture;
- write Jester’s implementation independently;
- do not copy Stella scanner code or Skyblocker skeleton/waypoint datasets into this MIT project;
- create and document Jester-owned room metadata/fingerprints, or separately resolve the licensing/distribution model before importing any dataset;
- preserve required notices if any BSD-licensed implementation is directly adapted rather than independently rewritten.

## Proposed implementation order

1. Pure domain grid, rooms/doors, transforms, lifecycle reducer, and tests.
2. Standalone config defaults/codec/migration/repository and tests.
3. Map calibration and packet-derived topology/checkmark observations using fixtures.
4. Chunk-load fingerprint scanner, Jester room registry, rotation/local transforms, and diagnostics.
5. Party roster, entity/map fallback positions, and interpolation.
6. Port the Spectrum 2D command renderer/backend and add continuous room contours.
7. Build the polished HUD, visibility policies, labels, markers, and score strip.
8. Add fallback identity matching only if real captures show the core fingerprint path is insufficient.

## Inspected sources

Local snapshots:

- OdinClient `da4922944c31bcd4f3704b91c55830f0bc0ac77a`: `CheaterMap.kt`, `DungeonScanMixin.java`, and `MapRendererMixin.java`. Its actual map model is supplied by upstream Odin.
- Stella `e085528718ce39b5ddad9d8b39b644619d0b2ea8`: `api/dungeons/map`, `api/dungeons/utils`, `api/dungeons/players`, `Dungeon.kt`, and `features/stellanav`.
- Skyblocker `bdb1a273466914a235640e446d71d31519fff07c`: `DungeonMap*.java` and `skyblock/dungeon/secrets/{DungeonManager,DungeonMapUtils,Room}.java`.
- Spectrum Client `856c58de79d1eec88cb581ce3645581ccc89b4bc`: shared 2D render commands and the 26.x extraction backend listed above.

Upstream Odin map source inspected from the `main` branch on 2026-07-18:

- <https://github.com/odtheking/Odin/tree/main/src/main/kotlin/com/odtheking/odin/features/impl/dungeon/map>
- `DungeonScan.kt`, `MapScan.kt`, `WorldScan.kt`, `MapRenderer.kt`, `SpecialColumn.kt`, and the `tile` package.
