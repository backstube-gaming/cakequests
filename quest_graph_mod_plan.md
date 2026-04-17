# Quest Graph Mod Plan (Forge 1.18.2)

This document outlines the architecture and implementation plan for a simplified FTB Quests–like mod using advancements as the progression backend.

---

## Scope (v1)

- Datapack-driven quest graph
- Advancement-based completion only
- Tabbed UI with graph layout
- Clickable nodes with details panel
- No teams, rewards, or sync logic

---

## Architecture

### 1. Data Layer

- Load JSON from: data/<namespace>/quest_graphs/\*.json
- Classes:
    - QuestBookDefinition
    - QuestTabDefinition
    - QuestNodeDefinition

### 2. Advancement Bridge

- Resolve node → advancement
- Compute state:
    - COMPLETE
    - AVAILABLE
    - LOCKED

### 3. UI Layer

- Custom Screen
- Scrollable graph canvas
- Tabs + node rendering
- Details panel

### 4. Integration

- Keybind to open UI
- Optional item to open UI

---

## Datapack Format (Example)

```json
{
    "tabs": [
        {
            "id": "create",
            "title": { "text": "Create", "color": "dark_aqua" },
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

---

## Rendering Plan

- Pannable canvas (drag)
- Draw edges → nodes → tooltips
- Node states:
    - Locked
    - Available
    - Complete

---

## Progress Logic

- Use player advancement progress only
- No custom persistence

---

## FTB Compatibility

Support fields:

- title
- subtitle
- description
- icon
- x, y
- shape
- dependencies (alias: parents)

---

## Phases

1. Bootstrap mod + screen
2. Datapack loader
3. Advancement integration
4. Graph rendering
5. Details panel
6. Polish
7. Import tool

---

## Acceptance Criteria

- Datapack adds quests
- UI opens via keybind/item
- Nodes render + connect
- Completion matches advancements
- Details panel works

---

## Notes

- No zoom in v1
- No rewards
- No networking
- Layout defined only in graph config
