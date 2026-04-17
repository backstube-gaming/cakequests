---
name: mc-forge-1182-api-guard
description: Validate Java code for this exact Minecraft Forge 1.18.2 project against local decompiled-sources before using Minecraft, Forge, registry, event, networking, data generation, or resource APIs. Use when writing or reviewing Java under src/main/java, Forge metadata/resources, or generated-data code where Minecraft patch-version API drift could break correctness.
---

# MC/Forge 1.18.2 API Guard

Use this skill to keep generated code tied to the exact local target:
Minecraft 1.18.2, Forge 40.3.12, Architectury API 4.12.94, Java 17, official mappings.

## Workflow

1. Read `gradle.properties` only for the version constants relevant to the task.
2. Identify every Minecraft, Forge, or Architectury type/method the change will depend on.
3. Search `decompiled-sources/` before committing to an API shape. Prefer exact class files over memory from other
   Minecraft versions.
4. For patterns not present in `decompiled-sources/`, search `examples/` for the matching 1.18.x project before using
   newer-version examples.
5. When APIs differ between memory and local sources, local sources win.
6. Summarize which local files confirmed the API if the task is risky or user-facing.

## Fast Lookups

Use `rg` first. Keep searches narrow:

```powershell
rg "class RegistryObject|interface IEventBus|RegisterEvent|DeferredRegister" decompiled-sources examples
rg "enqueueWork|NetworkEvent.Context|SimpleChannel" decompiled-sources/net/minecraftforge/network
rg "GatherDataEvent|DataGenerator|ExistingFileHelper" decompiled-sources/net/minecraftforge
```

For repeated symbol lookups, run:

```powershell
powershell -ExecutionPolicy Bypass -File skills/mc-forge-1182-api-guard/scripts/search-version-api.ps1 DeferredRegister
```

Use `-MaxResults 40` or `-Roots decompiled-sources/net/minecraftforge/registries` to narrow noisy searches.

## Guardrails

- Do not assume newer Minecraft APIs exist in 1.18.2. Verify names and signatures locally.
- Treat `decompiled-sources/` as read-only reference material.
- Do not edit anything under `examples/`; use it only as an offline reference cache.
- Avoid broad source loading. Search first, then open only the matching file(s).
- For imports, verify package names against local source files.
- For lifecycle code, verify the event bus and side/client-only annotations in local Forge sources.
- For resources, verify pack format and metadata against existing `src/main/resources` and Forge 1.18.2 examples.

## Reference Map

Read `references/source-map.md` when you need path hints for common Minecraft/Forge APIs.
