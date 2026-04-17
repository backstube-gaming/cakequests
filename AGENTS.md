# Repository Guidelines

## Versions
- Target Java 17
- Minecraft Java 1.18.2
- Forge 40.3.12 for 1.18.2
- Architectury API v4.12.94 for 1.18.2
- UTF-8 encoding

## Project Structure & Module Organization

This is a Minecraft Forge 1.18.2 mod project built with Gradle. Main Java sources live in `src/main/java`; resources and Forge metadata live in `src/main/resources`, especially `META-INF/mods.toml` and `pack.mcmeta`. Generated data resources are expected under `src/generated/resources` and are included by `build.gradle`.

Project metadata is centralized in `gradle.properties`: `mod_id=cakequests`, `mod_group_id=net.backstube.cakequests`, Forge version, Minecraft version, and mappings. Keep Java package names, `@Mod(...)`, and resource namespaces aligned with `mod_id`. The `examples/` directory is an offline reference cache for FTB Quests, Architectury, and quest data examples; do not treat it as production source.

## Build, Test, and Development Commands

Use the Gradle wrapper from the repository root:

- `.\gradlew.bat build` builds and reobfuscates the mod jar.
- `.\gradlew.bat runClient` launches the Forge client using `run/`.
- `.\gradlew.bat runServer` launches a local dedicated server using `run/`.
- `.\gradlew.bat runData` runs data generation into `src/generated/resources`.
- `.\gradlew.bat genIntellijRuns` or `.\gradlew.bat genEclipseRuns` creates IDE run configs.
- `.\gradlew.bat --refresh-dependencies` refreshes Forge and Gradle dependency caches when setup breaks.

On Unix-like shells, replace `.\gradlew.bat` with `./gradlew`.

## Coding Style & Naming Conventions

Lowercase mod/resource IDs. Prefer Forge event bus patterns and registries over ad hoc initialization. Keep constants such as mod IDs in one place when adding real mod code.
Use Architectury API to structure the code, but the mod is only for Forge, no other mod loaders.

## Testing Guidelines

There are no committed unit tests yet. Add Java tests under `src/test/java` when logic can be tested outside Minecraft, and run them with `.\gradlew.bat test`.
Do not add tests for code that cannot be tested outside of Minecraft.

## Commands

Do not run build or git commands on your own.