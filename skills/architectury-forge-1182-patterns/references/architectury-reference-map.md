# Architectury Reference Map

## Local Examples

- Architectury API 1.18.2 examples: `examples/architectury-api-1.18.2`
- Architectury Forge testmod resources: `examples/architectury-api-1.18.2/testmod-forge`
- FTB Quests 1.18 multi-module project: `examples/FTB-Quests-1.18-main`
- FTB Quests Forge module: `examples/FTB-Quests-1.18-main/forge`
- FTB Quests common module: `examples/FTB-Quests-1.18-main/common`

## Local Project

- Forge-only source root: `src/main/java`
- Forge metadata: `src/main/resources/META-INF/mods.toml`
- Pack metadata: `src/main/resources/pack.mcmeta`
- Version and mod constants: `gradle.properties`

## Useful Searches

- Registries:
  `rg "DeferredRegister|RegistrySupplier|RegistrySupplier" examples/architectury-api-1.18.2 examples/FTB-Quests-1.18-main`
- Events:
  `rg "EventFactory|EventResult|@SubscribeEvent|MOD_EVENT_BUS" examples/architectury-api-1.18.2 examples/FTB-Quests-1.18-main`
- Networking:
  `rg "NetworkManager|SimpleChannel|registerReceiver" examples/architectury-api-1.18.2 examples/FTB-Quests-1.18-main`
