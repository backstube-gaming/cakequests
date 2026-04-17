original prompt:

give me a plan for a coding ai agent, on what would be needed to build a FTB-quest like mod with a much more limited feature set: no teams, no sync - or rather only use minecraft advancements which are already synced to the client. The mod should take a json/snbt from a datapack to add new advancements to specific actions, and a config that describes the nodes for the quest graph. Quest graph: tab-style (like ftb-quests) for different mods. Each mod has a simple tree of steps (nodes may split, and link together again, just like in FTB-quests) Zoom is not required, but it should be scrollable horizontal and vertical. Each node can be clicked, which opens a short card explaining the quest step. The structure of the config should facilitate easy conversion from existing FTB-Quest configs. Only actions that can be added as minecraft advancements will be availble as quest-fulfilf criteria. Node should support using different icons, changing the color (in the config) and having colored, italic and bold text in the node details descriptions. Categories should be able to have a colored title + tab handle as well. The Ui should look almost exactly like FTB-QUests. Minecraft version 1.18.2 java, using forge.

## Use Architectury API v4.12.94 for 1.18.2

## use https://docs.minecraftforge.net/en/1.18.x as the docs
