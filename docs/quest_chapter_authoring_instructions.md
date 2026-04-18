# Quest Chapter Authoring Instructions

Use this fixed format when creating Cake Quests chapters. Do not add undocumented fields.

Top-level field order:

1. `id`
2. `enabled`
3. `title`
4. `tab_color`
5. `nodes`

Set `"enabled": true` explicitly unless the chapter is intentionally disabled.

Node field order:

1. `id`
2. `or`, only when true
3. `events`
4. `title`
5. `subtitle`
6. `description`
7. `icon`
8. `x`
9. `y`
10. `shape`
11. `color`
12. `parents`

Event requirements use only `id`, `type`, `item`, and `count`. Use `item_pickup`, `item_craft`, or `check`.
For `check` requirements, use `"item": "minecraft:air"` and `"count": 1`; the details panel renders a manual checkmark
button that completes the quest when clicked. Write item tags as `"#namespace:tag"`. Use full `namespace:path` IDs for
Minecraft and modded items.

If a chapter uses item tags, add readable tag names to `data/<namespace>/cakequests-main.json` under `tag_names` when
the
active language does not already provide one:

```json
"tag_names": {
  "minecraft:logs": {
    "translate": "tag.item.minecraft.logs",
    "fallback": "Any Logs"
  }
}
```

Descriptions may contain text components and images:

```json
[
  { "text": "Craft a starter machine.", "color": "gray" },
  {
    "text": "Open the recipe in JEI",
    "color": "aqua",
    "jei": "minecraft:furnace"
  },
  { "image": "cakequests:textures/gui/examples/starter_machine.png" }
]
```

JEI links are underlined in the details panel and open the linked item in Just Enough Items when JEI is installed on the
client. If JEI is absent, clicking the link does nothing.

Use `"shape": "challenge"` for manual unlock or challenge-style nodes. It uses the
`challenge_frame_obtained/unobtained` node textures.

Layout guidance:

- Use a regular grid with 72 to 96 pixels horizontal spacing and 80 to 120 pixels vertical spacing.
- Avoid one ultra-long straight line.
- Prefer branches from milestone nodes.
- Snake longer chains into a second row or column after about 6 to 8 nodes.
- Keep parent-child edges short and avoid crossing edges.
- Place optional objectives above or below the main path.
- Parent links are visual-only; they do not block completion.

Checklist:

- `enabled` is present.
- `or` is omitted unless the node is intentionally any-of.
- Every parent ID exists in the same chapter.
- Every node has an icon.
- Every event requirement has a positive count.
- Every item or tag target uses a full `namespace:path` ID.
- Every tag target starts with exactly one `#`.
- Description image IDs are valid texture resource locations.
- Coordinates do not stack multiple nodes on the same point.
- The graph branches or wraps instead of becoming one long line.
