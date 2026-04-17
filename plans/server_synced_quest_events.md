# Server-Synced Quest Events Plan

## Goal

Add lightweight server-authoritative quest progress events that quest nodes can use without requiring real Minecraft
advancements. The first supported event types are:

- item pickup by exact item
- item pickup by item tag
- crafted item by exact item

Each quest node may declare multiple event requirements. Each requirement has its own target count. A node completes
only
after all of its event requirements are complete and its parent nodes are complete. This feature is active only when the
server has Cake Quests installed and has synced the quest graph; client-only fallback mode keeps it disabled.

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

Extend node JSON with a Cake Quests-specific `events` array. Keep the existing `advancement` field for
advancement-backed
nodes. Nodes may use either `advancement`, `events`, or both. If both exist, the node is complete when the advancement
is
complete and all event requirements are complete.

Example:

```json
{
  "id": "collect_logs",
  "title": { "text": "Collect Logs" },
  "icon": { "item": "minecraft:oak_log" },
  "x": 120,
  "y": 80,
  "events": [
    {
      "id": "any_logs",
      "type": "item_pickup_tag",
      "tag": "minecraft:logs",
      "count": 16
    },
    {
      "id": "craft_table",
      "type": "item_craft",
      "item": "minecraft:crafting_table",
      "count": 1
    }
  ]
}
```

Supported fields:

- `id`: Requirement ID unique within the node. Optional in JSON, but parser should generate a stable ID from index when
  missing. Stable IDs are needed for progress persistence and sync.
- `type`: `item_pickup`, `item_pickup_tag`, or `item_craft`.
- `item`: Item registry ID for `item_pickup` and `item_craft`.
- `tag`: Item tag ID for `item_pickup_tag`.
- `count`: Required item count. Clamp to at least `1`; reject or warn for invalid values.

Do not call these "fake advancements" in code. Use names like `QuestEventRequirement`, `QuestEventType`, and
`QuestProgressState`. The feature behaves like advancement criteria for authors, but it should not be coupled to
Minecraft's advancement internals.

## Runtime Model

Add common data classes:

- `QuestEventRequirement`
    - `String id`
    - `QuestEventType type`
    - optional `ResourceLocation item`
    - optional `ResourceLocation tag`
    - `long count`
- `QuestEventType`
    - `ITEM_PICKUP`
    - `ITEM_PICKUP_TAG`
    - `ITEM_CRAFT`
- `QuestProgressKey`
    - `String tabId`
    - `String nodeId`
    - `String requirementId`
- `QuestPlayerProgress`
    - stores per-player progress counts keyed by `QuestProgressKey`
    - stores completed node IDs for quick dependency checks
    - persists to server `SavedData`, or to player persistent NBT if the first implementation should stay smaller

Prefer server `SavedData` if team/shared progress may be added later. Prefer player persistent NBT only if this feature
is intentionally per-player and must avoid world-level bookkeeping. Do not introduce teams for this feature.

## Server Index

Build an immutable lookup index when the server graph is loaded or reloaded:

- `Map<Item, List<RequirementBinding>> pickupByItem`
- `Map<Item, List<RequirementBinding>> craftByItem`
- `List<TagRequirementBinding> pickupByTag`
- `Map<String, QuestNodeDefinition> nodesByTabAndId`
- parent completion/dependency lookup

`RequirementBinding` contains the tab ID, node ID, requirement ID, target count, and type. It should not contain mutable
player progress.

Tag requirements need special care for performance. Do not scan all quest nodes on every pickup. Two acceptable designs:

1. On graph load and datapack tag reload, resolve each configured item tag to current item values and expand it into
   `pickupByItem`. This makes event handling O(number of bindings for the picked item).
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

- Key by player UUID unless shared party progress is intentionally added later.
- Include graph hash or graph version with saved progress.
- On graph reload, keep counts for matching `tabId/nodeId/requirementId` where type and target are compatible.
- Drop or ignore saved progress for removed requirements.
- Clamp saved counts to the current target count.

Save after progress changes, but avoid excessive disk churn:

- mark `SavedData` dirty immediately
- let Minecraft's normal save cadence write it
- do not force-save on every pickup/craft event

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
  if the server sends final node states.

In client-only fallback mode:

- Do not register server event tracking.
- Do not display event progress as active or completable.
- Keep fallback graphs guidance-only, matching the current behavior.
- Treat event requirements as inert metadata if they appear in local fallback JSON.

## Implementation Phases

1. Data model and parser
    - Add event requirement records and enum.
    - Extend `QuestNodeDefinition` JSON parsing/serialization.
    - Validate IDs, types, item IDs, tag IDs, counts, and requirement limits.
    - Update `docs/quest_graph_format.md`.

2. Server progress index
    - Build immutable requirement indexes from the active server book.
    - Rebuild indexes on graph/datapack reload.
    - Add fast empty-index checks.

3. Progress storage
    - Add per-player server progress state.
    - Persist counts and completed node states.
    - Handle login/logout/reload lifecycle.

4. Forge event bridge
    - Listen for `PlayerEvent.ItemPickupEvent`.
    - Listen for `PlayerEvent.ItemCraftedEvent`.
    - Update progress through the indexed server service only.

5. Progress networking
    - Add snapshot and delta packets.
    - Register packet IDs after the existing graph sync packet.
    - Send snapshot after graph sync.
    - Send deltas only after real quest progress/state changes.

6. Client progress UI
    - Add client-side progress store keyed by graph hash.
    - Update node state lookup to prefer server progress when server graph sync is active.
    - Show counts for selected event requirements.

7. Testing
    - Add unit tests for parsing, validation, index construction, and progress capping where possible outside Minecraft.
    - Manually test pickup exact item, pickup tag item, crafting result count, graph reload, login snapshot, and
      fallback mode.

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

- Per-player progress only, or a future-compatible `QuestProgressOwner` abstraction for later shared progress?
- Should mixed advancement/event nodes require both sources, or should `events` replace `advancement` when present?
- Should exact item matching include optional NBT matching later, or stay registry-ID-only for reliability and speed?
- Should craft-by-tag be supported in the first pass? It is technically easy with the same tag-expanded index, but the
  requested scope only mentions item pickup by tags.
