# Cake Quests Graph Format

Quest graphs are JSON files loaded from datapacks at:

```text
data/<namespace>/cakequests/quest_graphs/*.json
```

The main Cake Quests config is loaded from:

```text
data/<namespace>/cakequests-main.json
```

It may define display names for item tags used by quest events:

```json
{
  "title_label": "Cake Quests",
  "subtitle": "Example quests",
  "tag_names": {
    "minecraft:logs": {
      "translate": "tag.item.minecraft.logs",
      "fallback": "Any Logs"
    },
    "minecraft:planks": {
      "translate": "tag.item.minecraft.planks",
      "fallback": "Any Planks"
    }
  }
}
```

Each `tag_names` entry supplies a normal Minecraft translation key plus a plain fallback. If the key does not exist in
the active language, Cake Quests uses `fallback`. If a tag has neither a usable `tag_names` entry nor an existing
language translation, it is displayed as `Any #namespace:path`.

Each file defines one visual chapter/tab. A wrapper object with a single `tabs` array is also accepted, but the runtime
merges files as separate tabs.

```json
{
  "id": "getting_started",
  "enabled": true,
  "title": { "text": "Getting Started", "color": "gold" },
  "tab_color": "#D49A35",
  "nodes": [
    {
      "id": "wood_choice",
      "or": true,
      "events": [
        {
          "id": "pickup_logs",
          "type": "item_pickup",
          "item": "#minecraft:logs",
          "count": 16
        },
        {
          "id": "craft_planks",
          "type": "item_craft",
          "item": "minecraft:oak_planks",
          "count": 16
        }
      ],
      "title": { "text": "Wood Supply" },
      "subtitle": { "text": "Any route works" },
      "description": [
        { "text": "Gather logs or craft planks.", "color": "gray" },
        {
          "text": "Open oak planks in JEI.",
          "color": "aqua",
          "jei": "minecraft:oak_planks"
        },
        { "image": "cakequests:textures/gui/examples/wood.png" }
      ],
      "icon": { "item": "minecraft:oak_log" },
      "x": 140,
      "y": 80,
      "shape": "circle",
      "color": "#A9ADB5",
      "parents": []
    }
  ]
}
```

Fields:

- `id`: Unique chapter or node ID within its scope.
- `enabled`: Chapter flag directly below `id`. Defaults to `true`; `false` makes the chapter behave as nonexistent.
- `title`, `subtitle`: Minecraft component-compatible text.
- `description`: Array of text components, JEI link entries, and image entries. JEI links use
  `{ "text": "Shown text", "color": "aqua", "jei": "namespace:item" }`; they render underlined and open Just Enough
  Items on clients that have JEI installed. Image entries use `{ "image": "namespace:path.png" }`.
- `tab_color`, `color`: `#RRGGBB` or `#AARRGGBB`.
- `or`: Optional node boolean. Defaults to `false`. If `true`, any event requirement can complete the node.
- `events`: Server-synced event requirements for the node.
- `events[].type`: `item_pickup`, `item_craft`, or `check`. `check` renders a manual checkmark button at the end of
  the details text and completes the node through a server sync packet when clicked.
- `events[].item`: Item registry ID or item tag ID prefixed with `#`.
- `events[].count`: Positive target count.
- `icon.item`: Item registry ID. Missing items render as a book.
- `x`, `y`: Stable graph coordinates. No auto-layout is applied.
- `shape`: `circle`, `square`, `diamond`, or `challenge`.
- `parents`: Visual-only node IDs in the same chapter. Parents guide the UI but do not block completion.
- `dependencies`: Accepted as an alias for `parents` to simplify FTB Quests conversion.

All item, tag, icon, and image IDs are `namespace:path` resource locations. Namespaces may come from Minecraft or any
mod loaded on both the server and client.
