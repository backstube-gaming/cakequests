# Cake Quests Graph Format

Quest graphs are JSON files loaded from datapacks at:

```text
data/<namespace>/cakequests/quest_graphs/*.json
```

Each file defines one visual tab. A wrapper object with a single `tabs` entry is also accepted for easier conversion
from book-style examples, but the runtime merges files as separate tabs.

```json
{
  "id": "getting_started",
  "title": { "text": "Getting Started", "color": "gold" },
  "tab_color": "#D49A35",
  "nodes": [
    {
      "id": "stone_age",
      "advancement": "minecraft:story/mine_stone",
      "title": { "text": "Stone Age" },
      "subtitle": { "text": "Vanilla" },
      "description": [
        { "text": "Mine stone with your first pickaxe.", "color": "gray" }
      ],
      "icon": { "item": "minecraft:cobblestone" },
      "x": 140,
      "y": 40,
      "shape": "square",
      "color": "#A9ADB5",
      "parents": ["root"]
    }
  ]
}
```

Fields:

- `id`: Unique tab or node ID within its scope.
- `title`, `subtitle`, `description`: Minecraft component-compatible text.
- `tab_color`, `color`: `#RRGGBB` or `#AARRGGBB`.
- `advancement`: Existing server advancement ID used as the only progression source.
- `icon.item`: Item registry ID. Missing items render as a book.
- `x`, `y`: Stable graph coordinates. No auto-layout is applied.
- `shape`: `circle`, `square`, or `diamond`.
- `parents`: Node IDs that must be complete before this node is available.
- `dependencies`: Accepted as an alias for `parents` to simplify FTB Quests conversion.

Client fallback graphs use the same file format under:

```text
config/cakequests/quest_graphs/*.json
```

Fallback mode is guidance-only: all nodes are available and advancement progress is not queried.
