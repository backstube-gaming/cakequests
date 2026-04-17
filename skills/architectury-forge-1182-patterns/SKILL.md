---
name: architectury-forge-1182-patterns
description: Check Architectury API 4.12.94 usage in this Forge-only Minecraft 1.18.2 mod before adding registries, events, networking, platform abstractions, or shared code patterns. Use when generated code may accidentally mix Fabric/newer Architectury APIs or ignore this repository's Forge-only constraint.
---

# Architectury Forge 1.18.2 Patterns

Use this skill to apply Architectury API only where it fits this Forge-only project. The mod targets Forge 40.3.12 and
Minecraft 1.18.2; do not add loader targets or multi-loader structure unless the user asks.

## Workflow

1. Read `gradle.properties` for `minecraft_version`, `forge_version`, and Architectury dependency versions if needed.
2. Search `examples/architectury-api-1.18.2` for exact API usage before using remembered Architectury APIs.
3. Search `examples/FTB-Quests-1.18-main` only when looking for real 1.18 multi-module patterns.
4. Keep the implementation Forge-only in `src/main/java` and Forge resources unless the user explicitly asks for another
   loader.
5. Verify any Architectury API package and method against examples or dependency sources before importing it.

## Fast Lookups

```powershell
rg --line-number "DeferredRegister|RegistrySupplier|Event|NetworkManager" examples/architectury-api-1.18.2 examples/FTB-Quests-1.18-main
rg --line-number "architectury" build.gradle gradle.properties examples/architectury-api-1.18.2
```

For repeated Architectury lookups, run:

```powershell
powershell -ExecutionPolicy Bypass -File skills/architectury-forge-1182-patterns/scripts/search-architectury.ps1 RegistrySupplier
```

Use `-MaxResults 40` or `-Roots examples/architectury-api-1.18.2/common` for narrower searches.

## Guardrails

- Use Architectury as a helper API, not as a reason to create Fabric/Common/Forge source sets in this project.
- Do not use Architectury APIs from newer Minecraft versions without confirming they exist in the local 1.18.2 examples.
- Prefer established Forge registration/event patterns when Architectury would add complexity.
- Keep `mod_id=cakequests` and package alignment in mind when adding real mod classes.
- When a source example is multi-loader, translate only the relevant Forge-side or common pattern.

## Reference Map

Read `references/architectury-reference-map.md` when you need path hints for local Architectury and Forge examples.
