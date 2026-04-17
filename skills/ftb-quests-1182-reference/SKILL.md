---
name: ftb-quests-1182-reference
description: Use the project-local FTB Quests 1.18 source cache and quest SNBT examples for exact-version quest graph, chapter, task, reward, dependency, and SNBT behavior. Use when creating, parsing, validating, or reviewing quest data or Java logic intended to resemble or interoperate with FTB Quests on Minecraft 1.18.x.
---

# FTB Quests 1.18.2 Reference

Use this skill when the task touches quest files, quest graph modeling, SNBT, rewards, task definitions, dependencies,
chapter layout, or compatibility expectations inspired by FTB Quests.

## Workflow

1. Prefer 1.18 examples first:
    - `examples/FTB-Quests-1.18-main`
    - `examples/FTB-Library-1.18-main`
    - `examples/ATM-8-main/ftbquests/quests`
2. Use later-version quest packs only to compare data shape when no 1.18 example exists. Label that as a cross-version
   inference.
3. Search for the concept before opening files: `dependency`, `quest_links`, `tasks`, `rewards`, `chapter_groups`, `x`,
   `y`, `id`, `title`, or the reward/task type.
4. If implementing Java inspired by FTB Quests, inspect the 1.18 source in `examples/FTB-Quests-1.18-main` before
   designing classes or method names.
5. Keep loaded context narrow: open one chapter/reward table and one source file at a time.

## Fast Lookups

```powershell
rg --line-number "dependencies|dependency|tasks|rewards" examples/ATM-8-main/ftbquests/quests
rg --line-number "class .*Quest|class .*Chapter|interface .*Task" examples/FTB-Quests-1.18-main
```

For repeated data/source lookups, run:

```powershell
powershell -ExecutionPolicy Bypass -File skills/ftb-quests-1182-reference/scripts/search-quests.ps1 dependency
```

Use `-MaxResults 40` or `-Roots examples/ATM-8-main/ftbquests/quests/chapters` when a term is common.

## Guardrails

- Do not assume FTB Quests data is stable across Minecraft versions.
- Do not treat ATM/AQM quest packs as production source; they are examples for data shape.
- Do not invent SNBT fields without finding a matching example or source reader.
- Preserve IDs and dependency references carefully; graph behavior is more important than file aesthetics.
- When creating Cake Quests data formats, state whether a field is compatible with FTB Quests, inspired by it, or Cake
  Quests-specific.

## Reference Map

Read `references/quest-reference-map.md` when you need path hints for source and SNBT examples.
