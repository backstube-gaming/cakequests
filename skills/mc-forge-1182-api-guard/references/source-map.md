# Source Map

Use these paths as starting points. Search within them; do not load whole folders.

## Core Minecraft

- Blocks/items/entities: `decompiled-sources/net/minecraft/world/level`, `decompiled-sources/net/minecraft/world/item`,
  `decompiled-sources/net/minecraft/world/entity`
- Registries/resources: `decompiled-sources/net/minecraft/core`, `decompiled-sources/net/minecraft/resources`
- Components/text: `decompiled-sources/net/minecraft/network/chat`
- NBT/SNBT: `decompiled-sources/net/minecraft/nbt`
- Server/player/commands: `decompiled-sources/net/minecraft/server`
- Client-only code: `decompiled-sources/net/minecraft/client`

## Forge 40.3.12

- Mod loading and event bus: `decompiled-sources/net/minecraftforge/fml`,
  `decompiled-sources/net/minecraftforge/eventbus`
- Registries: `decompiled-sources/net/minecraftforge/registries`
- Common events: `decompiled-sources/net/minecraftforge/event`
- Networking: `decompiled-sources/net/minecraftforge/network`
- Data generation: `decompiled-sources/net/minecraftforge/common/data`, `decompiled-sources/net/minecraftforge/data`
- Capabilities: `decompiled-sources/net/minecraftforge/common/capabilities`

## Local Project

- Main Java sources: `src/main/java`
- Resources: `src/main/resources`
- Generated resources: `src/generated/resources`
- Version constants: `gradle.properties`
- Offline examples: `examples/`
