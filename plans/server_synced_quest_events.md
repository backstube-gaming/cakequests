# Server-Synced Quest Events Plan

## Goal

Add lightweight server-authoritative quest progress events that quest nodes can use without requiring real Minecraft
advancements. The first supported event types are:

- item pickup by exact item
- item pickup by item tag through `item: "#namespace:tag"`
- crafted item by exact item

Each quest node may declare multiple event requirements. Each requirement has its own target count. A node completes
only
after all of its event requirements are complete and its parent nodes are complete. This feature is active only when the
server has Cake Quests installed and has synced the quest graph; client-only fallback mode keeps it disabled.

Quest progress is permanently per-player. Cake Quests should not add parties, teams, shared quest progress, or a future
progress-owner abstraction for this feature. Every stored count, completion state, admin command, and sync packet should
target one or more concrete player UUIDs.

## Confirmed 1.18.2 Hooks

Local Forge 40.3.12 sources confirm these server-side events:

- `net.minecraftforge.event.entity.player.PlayerEvent.ItemPickupEvent`
    - `getPlayer()`
    - `getStack()` returns the picked-up stack clone and count.
    - `getOriginalEntity()` exposes the source `ItemEntity`.
- `net.minecraftforge.event.entity.player.PlayerEvent.ItemCraftedEvent`
    - `getPlayer()`
    - `getCrafting()` returns the crafted result stack.
    - `getInventory()` exposes the crafting matrix.
- `net.minecraftforge.event.entity.player.PlayerEvent.ItemSmeltedEvent` exists and is a good follow-up candidate.

FTB Quests 1.18 task types include item, XP, dimension, stat, kill, location, checkmark, advancement, observation,
biome,
structure, gamestage, fluid, and Forge energy. For Cake Quests, only direct Forge/Minecraft events with low overhead
should be considered.

## Data Format

Extend node JSON with a Cake Quests-specific `events` array. Keep the existing `advancement` field for normal
advancement-backed nodes. Nodes may use either `advancement`, `events`, or both. If both exist, the node is complete
when the advancement is complete and all event requirements are complete.

Add an `enabled` flag to chapter/tab JSON directly below `id`. It defaults to `true`. If `enabled` is `false`, the
chapter is treated as nonexistent by the mod: do not validate its nodes, do not index its event requirements, do not
sync
it to clients, do not persist new progress for it, and do not show it in the quest book. Existing saved progress for a
disabled chapter may remain on disk but should be ignored until the chapter is enabled again.

Example:

```json
{
  "id": "collect_logs",
  "enabled": true,
  "title": { "text": "Collect Logs" },
  "tab_color": "#D49A35",
  "nodes": [
    {
      "id": "logs",
      "events": [
        {
          "id": "any_logs",
          "type": "item_pickup",
          "item": "#minecraft:logs",
          "count": 16
        },
        {
          "id": "craft_table",
          "type": "item_craft",
          "item": "minecraft:crafting_table",
          "count": 1
        }
      ],
      "title": { "text": "Collect Logs" },
      "subtitle": { "text": "Gather and craft" },
      "description": [
        { "text": "Pick up 16 logs and craft a crafting table.", "color": "gray" }
      ],
      "icon": { "item": "minecraft:oak_log" },
      "x": 120,
      "y": 80,
      "shape": "circle",
      "color": "#A9ADB5",
      "parents": []
    }
  ]
}
```

Supported chapter field:

- chapter `enabled`: Optional boolean immediately after chapter `id`; defaults to `true`. `false` disables the entire
  chapter as if it did not exist.

Supported event requirement fields:

- `id`: Requirement ID unique within the node. Optional in JSON, but parser should generate a stable ID from index when
  missing. Stable IDs are needed for progress persistence and sync.
- `type`: `item_pickup` or `item_craft`.
- `item`: Item registry ID for exact matching, or `#namespace:tag` for item tag matching. Tag syntax is only valid for
  `item_pickup` in the first implementation. This follows the common Minecraft command/datapack convention where a
  leading `#` means "tag" rather than "single registry entry".
- `count`: Required item count. Clamp to at least `1`; reject or warn for invalid values.

Prefer names like `QuestEventRequirement`, `QuestEventType`, and `QuestProgressState` in Cake Quests code. The feature
behaves like advancement criteria for authors, but does not need to create real datapack advancements.

It is acceptable to reuse vanilla advancement concepts or implementation pieces if that reduces complexity and stays
isolated from the real advancement system. Any such reuse must meet these constraints:

- Do not require pack authors to define advancement JSON files for these event requirements.
- Do not register visible advancements that appear in the vanilla advancements tab.
- Do not modify, revoke, or grant real server advancements as part of event progress.
- Do not let malformed Cake Quests event requirements break vanilla advancement loading.
- Keep failure local to Cake Quests validation and logging.

If those constraints make vanilla advancement reuse awkward, use a small Cake Quests-owned progress model instead. A
clean custom model is preferred over a fragile partial integration.

## Runtime Model

Add common data classes:

- `QuestEventRequirement`
    - `String id`
    - `QuestEventType type`
  - `QuestItemTarget target`
    - `long count`
- `QuestEventType`
    - `ITEM_PICKUP`
    - `ITEM_CRAFT`
- `QuestItemTarget`
    - exact item `ResourceLocation`
    - or item tag `ResourceLocation`, parsed from a leading `#`
- `QuestProgressKey`
    - `String tabId`
    - `String nodeId`
    - `String requirementId`
- `QuestPlayerProgress`
    - stores per-player progress counts keyed by `QuestProgressKey`
    - stores completed node IDs for quick dependency checks
  - persists per player to server `SavedData`, or to player persistent NBT if the first implementation should stay
    smaller

Prefer whichever persistence option is simpler and most reliable for per-player state. Do not design storage around
parties, teams, shared ownership, or future migration to shared progress.

## Server Index

Build an immutable lookup index when the server graph is loaded or reloaded:

- `Map<Item, List<RequirementBinding>> pickupByItem`
- `Map<Item, List<RequirementBinding>> craftByItem`
- `Map<String, QuestNodeDefinition> nodesByTabAndId`
- parent completion/dependency lookup

`RequirementBinding` contains the tab ID, node ID, requirement ID, target count, and type. It should not contain mutable
player progress.

Tag item targets need special care for performance. Do not scan all quest nodes on every pickup. Two acceptable designs:

1. On graph load and datapack tag reload, resolve each `item: "#namespace:tag"` target to current item values and
   expand it into `pickupByItem`. This makes event handling O(number of bindings for the picked item).
2. If tag expansion is deferred, maintain `Map<TagKey<Item>, List<RequirementBinding>>` and test only the tags attached
   to the picked stack's item, never every configured tag.

Prefer option 1 because quest graphs are small and reload-time work is cheaper than per-pickup work. Rebuild the index
after datapack reload because tags can change.

## Event Handling

Register Forge event listeners from the Forge module only when running with the server mod active. Ignore client-only
fallback graphs entirely.

Item pickup flow:

1. Listen to `PlayerEvent.ItemPickupEvent`.
2. Return immediately unless `event.getPlayer()` is a `ServerPlayer`.
3. Return immediately if the event index has no pickup requirements.
4. Read `ItemStack stack = event.getStack()`.
5. Return if empty.
6. Lookup exact and tag-expanded bindings by `stack.getItem()`.
7. For each binding, skip if the node is not currently available or the requirement is already complete.
8. Add `stack.getCount()` capped at the requirement target.
9. If progress changed, persist and enqueue one progress sync for that player.

Craft flow:

1. Listen to `PlayerEvent.ItemCraftedEvent`.
2. Return immediately unless `event.getPlayer()` is a `ServerPlayer`.
3. Return immediately if the craft index is empty.
4. Read `ItemStack stack = event.getCrafting()`.
5. Lookup bindings by `stack.getItem()`.
6. Add `stack.getCount()` capped at the target.
7. Persist and sync only when progress changed.

Do not poll inventory, do not run per-player tick checks for these first event types, and do not inspect every quest
node
inside event listeners.

## Completion Rules

Node state should become server-authoritative when server sync is active:

- `LOCKED`: one or more parents are incomplete.
- `AVAILABLE`: parents complete, but this node's advancement/event requirements are incomplete.
- `COMPLETE`: parents complete and all configured progress sources are complete.

For event-only nodes, completion is based only on the event requirement counts. For nodes with no `advancement` and no
`events`, keep current behavior or treat them as always available; decide explicitly in implementation and document it.

When a requirement changes:

1. Recompute the node state.
2. If the node completes, recursively re-evaluate children for availability.
3. Send client updates for only the affected node and any children whose visible state changed.

## Network Sync

Keep graph definition sync separate from progress sync:

- Existing graph sync: server sends graph JSON/config on login and reload.
- New progress snapshot: server sends compact progress state after graph sync, on login, respawn if needed, and reload.
- New progress delta: server sends only changed requirement counts and node states after an event progresses/completes.

Packets:

- `QuestProgressSnapshotPacket`
    - graph hash
    - tab/node state map
    - incomplete event requirement counts that are greater than zero
- `QuestProgressDeltaPacket`
    - graph hash
    - changed requirement counts
    - changed node states

The client must not receive packets for every item change. It receives a progress delta only when a quest requirement
actually changes or a node state changes. If the picked/crafted item has no indexed requirement, no packet is sent.

Use the graph hash already present in `GraphSyncPacket` to discard stale progress packets after reloads.

## Persistence

Persist counts on the logical server:

- Key by player UUID only.
- Include graph hash or graph version with saved progress.
- On graph reload, keep counts for matching `tabId/nodeId/requirementId` where type and target are compatible.
- Drop or ignore saved progress for removed requirements.
- Clamp saved counts to the current target count.

Save after progress changes, but avoid excessive disk churn:

- mark `SavedData` dirty immediately
- let Minecraft's normal save cadence write it
- do not force-save on every pickup/craft event

## Admin Commands

Add server commands for operators to repair broken quest book states, test quest flows, and debug support reports. These
commands must operate on per-player Cake Quests progress only and must not grant, revoke, or reset real Minecraft
advancements.

Register commands through Forge's server command registration event and require an appropriate permission level, such as
permission level 2 or higher.

- `/cakequests reset @p`
    - Reset all Cake Quests progress for the selected player or players.
    - Clear event counts and completed node state, then send a fresh progress snapshot.
- `/cakequests grant id @p`
    - Mark the node or event requirement identified by `id` complete for the selected player or players.
    - Use a documented ID resolution rule, preferably `tab_id/node_id` for a node and
      `tab_id/node_id/requirement_id` for one requirement.
    - Recompute dependent node states and send only the resulting progress/state delta.
- `/cakequests revoke id @p`
    - Remove completion for the node or event requirement identified by `id`.
    - Clamp affected requirement counts to `0` unless the command is explicitly revoking only node completion.
    - Recompute dependent node states and send only the resulting progress/state delta.
- `/cakequests grant-to id @p`
    - Set progress for the identified node or event requirement to its target count without changing unrelated
      requirements.
    - This is useful for repairing partially broken state where an admin wants the selected step complete but does not
      want a broad node grant to hide other missing requirements.

Command output should report exactly what changed: selected player count, resolved ID, old progress, new progress, and
whether any node state changed. Invalid IDs should fail with a clear message and suggestions when possible.

## Quest Authoring Instructions

Create a dedicated instruction file for other AI tools that need to create Cake Quests chapters with as few mistakes as
possible. Recommended path: `docs/quest_chapter_authoring_instructions.md`.

The instruction file should define one fixed chapter format and tell authors to follow it exactly:

- Top-level field order:
    - `id`
    - `enabled`
    - `title`
    - `tab_color`
    - `nodes`
- `enabled` must be written explicitly, normally as `true`.
- Each node should use a fixed field order:
    - `id`
    - `advancement` and/or `events`
    - `title`
    - `subtitle`
    - `description`
    - `icon`
    - `x`
    - `y`
    - `shape`
    - `color`
    - `parents`
- Event requirements should use only `type`, `item`, and `count`, with item tags written as `"#namespace:tag"`.
- IDs should be short, lowercase, stable, and descriptive. Prefer `snake_case`.
- Parent references must point to node IDs in the same chapter.
- Do not invent fields outside the fixed format unless Cake Quests documents them.

The file should include layout guidance so generated quest chapters fit the screen:

- Use a regular grid. Start with 120 to 160 pixels of horizontal spacing and 80 to 120 pixels of vertical spacing.
- Keep related nodes near each other and keep parent-child edges short.
- Avoid ultra-long linear graphs that require excessive horizontal or vertical scrolling.
- Prefer small branches from milestone nodes over one straight chain.
- For multi-step chains, snake the path around in rows or columns so it uses the visible screen area better.
- Keep branches readable: avoid crossing edges, avoid placing sibling nodes on top of each other, and leave enough room
  for hover/selection UI.
- Use chapter-local sections, such as early game, tools, farming, machines, and exploration, instead of one giant path.
- If a chain has more than about 6 to 8 nodes, split it into branches or wrap it into a second row.
- Put the main progression path near the center and optional side objectives above or below it.

The instruction file should also include a minimal valid example chapter and a checklist that AI authors can self-check:

- `enabled` is present and true unless intentionally disabled.
- Every parent exists.
- Every node has an icon.
- Every event requirement has a positive count.
- Every `#tag` item target starts with exactly one `#`.
- Coordinates do not place multiple nodes on the same point.
- The graph has branches or wrapped chains instead of a single long line.

## Performance Requirements

This feature must have effectively zero negative server performance impact when unused:

- If no server graph has event requirements, listeners return after one boolean/index-empty check.
- Event listeners must use prebuilt maps and avoid scanning tabs/nodes.
- Tag checks must be expanded or indexed at reload time.
- No per-player ticking for item pickup/craft tracking.
- No inventory scans for pickup/craft tracking.
- No network packet unless progress changes.
- No allocation-heavy work in hot event handlers; bindings should be immutable and reusable.
- Cap packet sizes and reject unreasonable graph event counts during validation.

Recommended validation limits:

- max event requirements per node: 16
- max event requirements per graph: 4096
- max count per requirement: `Integer.MAX_VALUE` or a documented lower cap
- require `id` uniqueness within each node after generated fallback IDs are assigned

## Client Behavior

When connected to a Cake Quests server:

- The client uses the server-synced graph and server-synced progress.
- The UI reads node state from the new client progress store, not from client-side event detection.
- Advancement progress can remain a client bridge only for legacy behavior, but mixed advancement/event nodes are
  cleaner
  if the server sends final node states. If Cake Quests reuses vanilla advancement internals for event evaluation, that
  reuse should remain server-side and invisible to the client.

In client-only fallback mode:

- Do not register server event tracking.
- Do not display event progress as active or completable.
- Keep fallback graphs guidance-only, matching the current behavior.
- Treat event requirements as inert metadata if they appear in local fallback JSON.

## Implementation Phases

1. Data model and parser
    - Add event requirement records and enum.
    - Extend `QuestNodeDefinition` JSON parsing/serialization.
   - Add chapter/tab `enabled` parsing with default `true`.
   - Skip disabled chapters before node validation, event indexing, sync, and display.
   - Validate IDs, types, `item` targets, leading-`#` tag syntax, counts, and requirement limits.
    - Update `docs/quest_graph_format.md`.

2. Quest authoring documentation
    - Create `docs/quest_chapter_authoring_instructions.md`.
    - Document the fixed chapter/node/event field order.
    - Document node spacing, branch-first layout guidance, and snake/wrap guidance for longer chains.
    - Include a minimal example and self-check checklist for AI-generated chapters.

3. Server progress index
    - Build immutable requirement indexes from the active server book.
    - Rebuild indexes on graph/datapack reload.
    - Add fast empty-index checks.

4. Progress storage
    - Add per-player server progress state.
    - Persist counts and completed node states.
    - Handle login/logout/reload lifecycle.

5. Admin commands
    - Add `/cakequests reset @p`.
    - Add `/cakequests grant id @p`.
    - Add `/cakequests revoke id @p`.
    - Add `/cakequests grant-to id @p`.
    - Reuse the same progress mutation service as event handling so commands and events produce identical sync deltas.

6. Forge event bridge
    - Listen for `PlayerEvent.ItemPickupEvent`.
    - Listen for `PlayerEvent.ItemCraftedEvent`.
    - Update progress through the indexed server service only.

7. Progress networking
    - Add snapshot and delta packets.
    - Register packet IDs after the existing graph sync packet.
    - Send snapshot after graph sync.
    - Send deltas only after real quest progress/state changes.

8. Client progress UI
    - Add client-side progress store keyed by graph hash.
    - Update node state lookup to prefer server progress when server graph sync is active.
    - Show counts for selected event requirements.

9. Testing
    - Add unit tests for parsing, validation, index construction, and progress capping where possible outside Minecraft.
   - Test that disabled chapters are absent from validation, indexing, sync, and UI state.
   - Manually test pickup exact item, pickup `#tag` item, crafting result count, graph reload, login snapshot, and
      fallback mode.
   - Manually test every admin command against single-player and multiplayer selectors.

## Recommended Follow-Up Event Types

These are useful FTB Quests-style task types that Forge 1.18.2 can intercept with low overhead:

- `item_smelt`
    - Use `PlayerEvent.ItemSmeltedEvent`.
    - Same item/count model as craft.
    - Low overhead and reliable for furnace-like smelting outputs that fire the vanilla/Forge event.
- `xp_gained` or `xp_level_reached`
    - Use `PlayerXpEvent.XpChange` or player level checks on XP events.
    - Useful early-game quest type; avoid per-tick level polling.
- `dimension_enter`
    - Use `PlayerEvent.PlayerChangedDimensionEvent`.
    - Boolean/count target with dimension ID.
    - Very cheap and reliable.
- `advancement_done`
    - Use `AdvancementEvent`.
    - Keeps existing advancement support but lets the server sync one unified progress model.
- `entity_kill`
    - Use living death event with player damage source attribution.
    - Useful, but needs careful source attribution and fake-player filtering.
- `block_break`
    - Use block break event.
    - Useful for mining quests, but can be high frequency. Only enable if an indexed block requirement exists and avoid
      broad tag scans.
- `location_enter`
    - FTB Quests supports location/biome/structure tasks, but these are usually tick or movement based.
    - Avoid for the lightweight first implementation unless checked only on dimension change, login, respawn, or
      explicit
      player action.

Do not prioritize fluid, energy, task-screen submission, or inventory-scan item submission for this feature. They
require
capability integration, block entities, polling, or UI flows and are outside the lightweight event-synced scope.

## Open Decisions

- Should mixed advancement/event nodes require both sources, or should `events` replace `advancement` when present?
- Should exact item matching include optional NBT matching later, or stay registry-ID-only for reliability and speed?
- Should `item_craft` also allow `item: "#namespace:tag"` later? The same tag-expanded index can support it, but the
  first implementation should keep tag matching limited to pickup unless there is a concrete quest author need.
