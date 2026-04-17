# Quest Graph Mod Implementation Plan

## Goal

Build a lightweight Forge 1.18.2 quest graph mod for Minecraft. It should look and feel close to FTB Quests, but use
Minecraft advancements as the only progression backend. The mod intentionally will not implement teams, rewards, custom
progress syncing, server-side quest state, or multiplayer party systems. The only server-side behavior is loading quest
graph config and sending it to compatible clients when they connect.

This is a greenfield implementation. The repository currently contains only the initial Forge MDK-style setup for
Minecraft 1.18.2 with Cake Quests metadata filled in, plus reference examples. The plan should be treated as creating
the actual mod architecture and implementation almost entirely from scratch.

## Fixed Constraints

- Minecraft version: `1.18.2`
- Mod loader: Forge
- Java target: Java 17
- Required library: Architectury API `4.12.94` for Minecraft `1.18.2`
- Project structure: Architectury-style multi-module layout with `common` and `forge` modules only. Cake Quests is
  designed with common/platform separation, but only Forge is supported and no Fabric placeholder module should be
  created.
- Optional config UI compatibility target: Cloth Config `6.5.133` for Forge 1.18.x, but only as an optional dependency.
- Primary offline docs: decompiled-sources/ and examples/
- Primary online docs: <https://docs.minecraftforge.net/en/1.18.x/>
- Progress source: vanilla advancement progress only
- Deployment model: the mod is installed on both client and server for multiplayer graph distribution. The server side
  must be minimal: load graph config, send it to compatible clients on connect, and do nothing else.
- UI scope: graph navigation, tabs, and node cards. Scrollbars are an MVP fallback; FTB Quests-style free panning and
  zoom are preferred if they can be copied or adapted cheaply.

## V1 Feature Scope

- Server-provided quest graph definition files for multiplayer. They may use a datapack-like folder format for
  familiarity, but they are synced by the Cake Quests server mod, not by vanilla datapack sync.
- Quest graph files are loaded from datapacks under a Cake Quests-specific path so other mods or datapacks can
  contribute tabs independently.
- Quest nodes mapped to existing or datapack-added advancements.
- Tab-style categories, intended to group quests by mod or topic.
- Navigable graph canvas. Basic horizontal and vertical scrolling is acceptable for MVP, but free graph movement and
  zoom should be used if adapting FTB Quests code makes that simple.
- Split and merge quest paths through parent/dependency links.
- Clickable nodes that open a short detail card.
- Locked nodes remain visible and inspectable. They use a locked or disabled visual treatment, but are not hidden and
  later revealed by triggers.
- Configurable icons, node colors, node shapes, tab colors, and styled text.
- JSON-only runtime format for v1, with an SNBT compatibility/import path for easier FTB Quests conversion later.

Permanently out of scope: teams, rewards, custom progress networking, server persistence, and multiplayer party logic.
These are not deferred features; they are intentionally excluded from the mod's design. The one exception is the
required connect-time graph config transfer from server to Cake Quests clients.

Deferred feature: in-game quest editing is out of scope for v1, but may be added in a later version if the datapack
format and UI are stable. Zoom is not a primary v1 priority, but it is allowed if it comes naturally from reused graph
navigation code.

## Data Model

Load quest graph tab definitions on the server from datapacks:

```text
data/<namespace>/cakequests/quest_graphs/*.json
```

Each JSON file represents one visual tab/category in the full quest UI. This lets another mod or datapack add only one
additional tab without editing a global book file. The folder is intentionally under `cakequests` so the data type is
owned by this mod while still allowing any namespace to contribute files. Graph files are sent by this mod rather than
by vanilla datapack sync. In multiplayer, clients should receive the active server graph on connect and after datapack
reload. In single-player, the integrated server loads the same datapack files locally.

## Runtime Requirements

Server-side requirements:

- A dedicated server must have the Cake Quests mod installed if it should distribute quest graphs to clients.
- Because Cake Quests declares Architectury API as a required dependency, the server also needs Architectury API
  installed. Cloth Config must not be required server-side.
- The server must have the quest graph definition files in the chosen server-side location.
- If only vanilla advancements are referenced, no custom server datapack is required.
- If custom advancement IDs are referenced, the server must provide those advancements through a normal datapack.
- Any custom advancement criteria must be valid for the server's installed mods and data. This mod does not add new
  server-side criteria.
- The server must not reject, error, or log noisy stack traces when a client connects without the Cake Quests mod.

Client-side requirements:

- The client must have this mod installed.
- The client must have Architectury API `4.12.94` installed.
- Cloth Config `6.5.133` is optional and only needed if an optional config UI is implemented and used.
- In multiplayer, the client receives the quest graph from the server when both sides have Cake Quests installed.
- If the connected server does not have Cake Quests, the client should load fallback graph files from a sensible local
  location such as `config/cakequests/quest_graphs/*.json`.
- Local fallback graphs are guidance-only. When the connected server does not have Cake Quests, disable all advancement
  tracking for the quest UI and show every configured node as unlocked/available.
- In fallback mode, the Cake Quests item registered by the server is not available. The keybind must still open the
  local guidance UI.
- In fallback mode, a vanilla book renamed exactly `CakeQuests` should act as a client-side quest book affordance if
  feasible: use a client-only item model override/resource-pack predicate for the texture and detect a safe interaction
  to open the UI.
- If graph icons reference modded items or assets, the client must have those mods/assets available; otherwise the UI
  should fall back to a missing or generic icon.
- For server-synced graphs, quest graph advancement IDs must match the advancement IDs available from the connected
  server.

Core classes:

- `QuestBookDefinition`: top-level collection and metadata.
- `QuestTabDefinition`: category/tab title, tab color, and ordered nodes.
- `QuestNodeDefinition`: node ID, advancement ID, icon, position, color, shape, text, and dependencies.
- `QuestText`: wrapper for Minecraft component-compatible styled text.

Use `parents` as the internal dependency field. Accept `dependencies` as an alias to simplify conversion from FTB
Quests-style data. Treat one file as one `QuestTabDefinition`; the full client-side book is the merged list of all valid
tab files.

## Project Organization

Use an Architectury project layout similar to FTB Quests, but omit Fabric support entirely:

```text
settings.gradle
build.gradle
gradle.properties
common/
  build.gradle
  src/main/java/net/backstube/cakequests/
    CakeQuests.java
    data/
    net/
    quest/
    client/
  src/main/resources/
forge/
  build.gradle
  src/main/java/net/backstube/cakequests/forge/
    CakeQuestsForge.java
    CakeQuestsForgeClient.java
    net/
  src/main/resources/META-INF/mods.toml
```

`settings.gradle` should include only `common` and `forge`. Do not create a `fabric` module or placeholder. Shared
model, parsing, validation, graph state, UI classes that only depend on Minecraft/Architectury APIs, and packet payload
definitions belong in `common`. Forge-only entrypoints, `mods.toml`, Forge `SimpleChannel` registration, Forge event
registration, keybind registration hooks, and distribution metadata belong in `forge`.

Suggested package ownership:

- `net.backstube.cakequests`: shared constants and common init.
- `net.backstube.cakequests.data`: datapack graph loading, client fallback loading, parsing, validation, and serialized
  definitions.
- `net.backstube.cakequests.quest`: runtime graph model and advancement-backed node state.
- `net.backstube.cakequests.client`: screen, widgets, graph renderer, and client-side graph cache.
- `net.backstube.cakequests.net`: shared packet payloads/codecs.
- `net.backstube.cakequests.forge`: Forge entrypoint, events, reload registration, networking bridge, and client setup.

Dependency shape:

- Root build uses `architectury-plugin` and `dev.architectury.loom`, following the FTB Quests/Architectury 1.18.2
  examples.
- `common/build.gradle` uses `architectury { common() }` and
  `modApi "dev.architectury:architectury:${architectury_version}"`.
- `forge/build.gradle` uses `architectury { platformSetupLoomIde(); forge() }` and
  `modApi "dev.architectury:architectury-forge:${architectury_version}"`.
- The Forge module depends on the common module through
  `common(project(path: ":common", configuration: "namedElements"))` and shadows/remaps the transformed common output
  into the Forge jar.
- `forge/src/main/resources/META-INF/mods.toml` declares `architectury` as mandatory on `BOTH` sides and declares Cloth
  Config only as an optional dependency if a config UI is implemented.
- Build and run commands should target the Forge module: `./gradlew :forge:build`, `./gradlew :forge:runClient`, and
  `./gradlew :forge:runServer`.

## Example Quest Graph

```json
{
  "tabs": [
    {
      "id": "create",
      "title": { "text": "Create", "color": "dark_aqua" },
      "tab_color": "#2D9CDB",
      "nodes": [
        {
          "id": "andesite_alloy",
          "advancement": "yourpack:create/andesite_alloy",
          "title": { "text": "Andesite Alloy" },
          "description": [
            { "text": "Craft your first alloy", "color": "gray" }
          ],
          "icon": { "item": "create:andesite_alloy" },
          "x": 32,
          "y": 48,
          "shape": "diamond",
          "color": "#C89B3C",
          "parents": []
        }
      ]
    }
  ]
}
```

## Architecture

### Data Loading

- Implement reusable parsing and validation in `common`.
- Register the server-side datapack reload path for `cakequests/quest_graphs` from the Forge module, preferably through
  Architectury's reload listener API if it fits the exact reload lifecycle.
- Load one tab/category per JSON file and merge all valid tabs into the active book.
- Implement client fallback loading from `config/cakequests/quest_graphs/*.json` for connections to servers without Cake
  Quests.
- Parse JSON into immutable definition objects.
- Validate IDs, duplicate nodes, missing parent links, missing advancement IDs, invalid colors, and unknown shapes.
- Keep parsing independent from UI classes so import tools can reuse it.

### Graph Sync

- Keep packet DTOs and serialization helpers in `common`; register and send them from the Forge module.
- Add a minimal Forge `SimpleChannel` for graph config transfer only.
- Configure channel compatibility to accept missing Cake Quests clients, including Forge endpoints without the channel
  and vanilla/non-Forge endpoints where applicable.
- Send graph config only to clients that advertise the Cake Quests channel.
- Send once on login and again after datapack reload to already-connected Cake Quests clients.
- Keep packet contents bounded and data-only: graph version/hash plus serialized graph definitions.
- Do not send gameplay state, progress, rewards, team data, player-specific state, or client-to-server quest actions.
- Packet handlers must enqueue client work onto the main thread and fail gracefully on malformed data.

### Advancement Bridge

- Resolve each node's `ResourceLocation` advancement.
- Determine node state from client-known advancement progress:
  - `COMPLETE`: advancement is complete.
  - `AVAILABLE`: all parent nodes are complete and this node is incomplete.
  - `LOCKED`: one or more parent nodes are incomplete.
- State changes affect styling only. They must not remove nodes from the graph or prevent the player from opening the
  node details.
- Do not store custom completion state.
- Treat the server as the authority for advancement existence and progress. Custom advancement definitions must come
  from the server's datapacks; the quest graph only references them.

### UI Layer

- Add a keybind to open the quest screen.
- Add a quest book item that opens the same screen.
- When connected to a server without Cake Quests, keep the keybind functional and support a renamed vanilla book
  fallback if feasible:
  - a vanilla `minecraft:book` with custom hover name `CakeQuests` should use the quest book texture client-side;
  - right-click should open the fallback UI if this can be detected and cancelled client-side without sending unsafe or
    disruptive actions to the server;
  - if right-click cannot be handled safely, use the next safest detectable client-side interaction, such as opening the
    UI while reading/using a matching book from an inventory or lectern context.
- Keep the main UI classes in `common` when they only use Minecraft/Architectury APIs. Keep Forge-only client
  registration code in `forge`.
- Build a custom `Screen` with:
  - left or top tab handles,
  - graph navigation,
  - edge rendering before node rendering,
  - node hover tooltips,
  - click-to-open detail card,
  - FTB Quests-like colors, spacing, and icon treatment.
- Aggressively copy or adapt FTB Quests UI, graph navigation, node rendering, tab handling, and quest book interaction
  patterns, but remove teams, rewards, editing, task logic, sync state, and other unused systems. If a copied subsystem
  is tightly coupled to excluded features, trim it down instead of porting the dependency chain.
- Prefer FTB Quests-style click-drag panning and wheel zoom from the start if the copied/adapted graph code makes that
  practical. If that still turns out costly, use straightforward horizontal and vertical scrolling for the first
  playable version.
- Unlike FTB Quests-style hidden progression, every configured node should be shown from the start. Locked nodes should
  look disabled, muted, or locked, but still show their icon, title, connections, and detail card.
- Render styled text using Minecraft components so colored, bold, and italic descriptions work.

### FTB Conversion Support

- Keep field names close to FTB Quests where practical: `title`, `subtitle`, `description`, `icon`, `x`, `y`, `shape`,
  `dependencies`.
- Implement a small importer later that reads FTB Quest SNBT and emits this mod's JSON format.
- Treat importer work as a separate phase after the runtime format is stable.

## Implementation Phases

### Phase 1: Project Bootstrap

- Convert the current Forge MDK layout into an Architectury-style `common` + `forge` multi-module build. Expect this to
  replace most template code rather than extend an existing implementation.
- Replace MDK example package and `@Mod("examplemod")` with `net.backstube.cakequests` and `cakequests`.
- Add Architectury API `4.12.94` as `architectury` in common and `architectury-forge` in forge.
- Create a Forge entrypoint in `forge/src/main/java/net/backstube/cakequests/forge/CakeQuestsForge.java` that calls
  shared common initialization.
- Create basic client setup and keybind registration from the Forge module.
- Add an empty quest screen that opens in-game.
- Add server-safe common initialization with no world mutation, no saved quest state, and no required client presence.

### Phase 2: Quest Data Loader

- Define quest graph model classes in `common`.
- Add server-side graph loading from datapack path `data/*/cakequests/quest_graphs/*.json`, registered from `forge`.
- Add client fallback graph loading from `config/cakequests/quest_graphs/*.json`.
- Add validation and log readable errors.
- Add at least one sample graph file for manual testing.

### Phase 3: Graph Sync

- Register the optional Forge network channel in the `forge` module.
- Sync the loaded graph to Cake Quests clients after login.
- Resync the loaded graph to connected Cake Quests clients after datapack reload.
- Ensure clients without Cake Quests can join without errors.
- Add defensive size limits and malformed-packet handling.

### Phase 4: Advancement Integration

- Resolve configured advancement IDs.
- Compute node state from advancement progress.
- Make unavailable or missing advancements visible in debug logs.
- Confirm state updates after advancement completion without custom sync.
- In client fallback mode, do not query or display advancement progress. Treat every node as unlocked/available and
  never mark nodes as locked or complete.

### Phase 5: Graph Rendering

- Implement tab list and tab switching by adapting the relevant FTB Quests patterns.
- Implement graph navigation by copying/adapting FTB Quests-style free panning and zoom where practical; fall back to
  two-axis scrolling only if the copy is unexpectedly expensive.
- Render links, nodes, icons, locked/available/complete states, and hover feedback using trimmed FTB Quests visual
  patterns.
- Preserve stable node positions from config; do not auto-layout in v1.

### Phase 6: Details Panel

- Open a quest card when a node is clicked.
- Show title, subtitle, description, icon, advancement ID, and completion state.
- Support colored, bold, and italic component text.
- Keep locked nodes readable and clickable while making their locked state obvious.

### Phase 7: Polish and Compatibility

- Tune visual style toward FTB Quests by copying relevant UI behavior/assets/patterns aggressively and removing unneeded
  feature hooks.
- Add quest book item opener and default keybind opener.
- Add fallback-mode support for a vanilla book renamed `CakeQuests`: client-side texture swap plus the safest available
  interaction to open the guidance UI, with right-click preferred.
- If client-side configuration is added, keep it compatible with Cloth Config `6.5.133` for Forge 1.18.x without making
  Cloth Config a hard dependency.
- Add JSON schema or documented examples.
- Add SNBT-to-JSON importer for common FTB Quests fields.

## Verification Plan

- `./gradlew :forge:build` must pass.
- `./gradlew :forge:runClient` must open the quest UI from a keybind.
- `./gradlew :forge:runServer` must start without client-only classloading errors.
- A server graph definition file must sync to a Cake Quests client and add a visible tab and nodes.
- A datapack reload must resync changed graph tabs to already-connected Cake Quests clients.
- A server datapack with custom advancements plus the server-side Cake Quests graph sync must be sufficient for custom
  progression.
- A client without Cake Quests must be able to join the server without disconnects, server errors, or noisy exception
  logs.
- A Cake Quests client connected to a server without Cake Quests must load local fallback graph JSON and show a
  guidance-only graph.
- In fallback mode, all nodes must be unlocked and advancement tracking must be disabled.
- In fallback mode, the keybind must open the UI and a vanilla book renamed `CakeQuests` should provide a best-effort
  quest book interaction without requiring server-side mod support.
- Parent links must lock and unlock nodes based on advancement completion.
- Detail cards must display styled text correctly.
- Locked nodes must remain visible and their details must be viewable.
- The graph must be navigable at minimum by two-axis scrolling; free panning and zoom are preferred when simple to
  include.
- Invalid graph files must fail gracefully with useful logs.

## Acceptance Criteria

- Quest graph content is supplied by datapack files at `data/<namespace>/cakequests/quest_graphs/*.json`, one tab per
  file, and synced to compatible clients.
- The server-side mod has no responsibilities beyond graph loading, graph validation, and optional graph sync to Cake
  Quests clients.
- Runtime graph definitions are JSON-only in v1.
- The quest screen opens through both a keybind and a quest book item.
- Without server-side Cake Quests, the keybind still opens the fallback UI, and a renamed vanilla book should provide
  the closest safe client-side equivalent to the quest book.
- Nodes render in tabbed graphs with connecting edges.
- All configured nodes are visible immediately; none are hidden until triggered.
- With server graph sync active, node completion exactly follows advancement progress.
- The graph can be navigated horizontally and vertically. Scrollbars satisfy MVP requirements, while FTB Quests-style
  panning and zoom are better if inexpensive to adapt.
- Clicking a node opens a readable quest detail card.
- The data format is close enough to FTB Quests to support a future converter.

## Risks and Decisions

- Client advancement progress availability may require careful timing after login or reload.
- Vanilla and Forge do not sync arbitrary datapack files to clients without server-side mod code. Server resource packs
  are not an acceptable solution because servers may already use their single resource-pack slot for other content.
- The optional network channel must accept absent clients. A missing Cake Quests client should simply receive no graph
  UI, not be disconnected.
- The graph sync packet must be small and bounded; large graph sets may need compression, chunking, hashing, or a clear
  maximum size.
- Client fallback mode intentionally ignores all advancement progress, even if matching server advancements exist. This
  avoids misleading partial tracking when the server is not participating in Cake Quests graph sync.
- Client-only right-click handling for renamed vanilla books must be tested carefully. If opening the UI on right-click
  cannot be done without server-visible side effects, use a less invasive detectable interaction and document the
  limitation.
- SNBT parsing should not be added to the runtime path; keep it as a later offline/import tool.
- FTB Quests visual and interaction parity is a high priority, but copied code must be reduced to the Cake Quests scope.
- Copying graph navigation from FTB Quests may bring dependency or architecture assumptions; trim aggressively and avoid
  importing excluded systems just to preserve original structure.
- Cloth Config integration must remain optional; the mod should load and run without Cloth Config installed.
- Missing advancement definitions should not crash the client.
