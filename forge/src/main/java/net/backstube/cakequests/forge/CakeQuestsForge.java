package net.backstube.cakequests.forge;

import dev.architectury.platform.forge.EventBuses;
import net.backstube.cakequests.CakeQuests;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.IExtensionPoint;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.network.NetworkConstants;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

@Mod(CakeQuests.MOD_ID)
public class CakeQuestsForge {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, CakeQuests.MOD_ID);
    public static final RegistryObject<Item> QUEST_BOOK = ITEMS.register("quest_book", QuestBookItem::new);

    public CakeQuestsForge() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        EventBuses.registerModEventBus(CakeQuests.MOD_ID, modBus);
        ITEMS.register(modBus);
        modBus.addListener(CakeQuestsForgeNetwork::setup);
        ModLoadingContext.get().registerExtensionPoint(
                IExtensionPoint.DisplayTest.class,
                () -> new IExtensionPoint.DisplayTest(() -> NetworkConstants.IGNORESERVERONLY, (remote, isServer) -> true)
        );
        DistExecutor.unsafeRunWhenOn(net.minecraftforge.api.distmarker.Dist.CLIENT, () -> () -> CakeQuestsForgeClient.registerModBus(modBus));

        CakeQuests.init();
        MinecraftForge.EVENT_BUS.register(new CakeQuestsForgeEvents());
        DistExecutor.unsafeRunWhenOn(net.minecraftforge.api.distmarker.Dist.CLIENT, () -> () -> MinecraftForge.EVENT_BUS.register(new CakeQuestsForgeClient()));
    }

    public static CreativeModeTab itemGroup() {
        return CreativeModeTab.TAB_MISC;
    }
}
