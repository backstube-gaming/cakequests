package net.backstube.cakequests.forge;

import com.mojang.blaze3d.platform.InputConstants;
import net.backstube.cakequests.CakeQuests;
import net.backstube.cakequests.client.ClientQuestGraphStore;
import net.backstube.cakequests.client.QuestGraphScreen;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.client.ClientRegistry;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import org.lwjgl.glfw.GLFW;

public class CakeQuestsForgeClient {
    private static final String QUEST_BOOK_NAME = "CakeQuests";
    private static final ResourceLocation QUEST_BOOK_PROPERTY = CakeQuests.id("cakequests_book");
    private static KeyMapping openKey;

    public static void registerModBus(IEventBus modBus) {
        modBus.addListener(CakeQuestsForgeClient::clientSetup);
    }

    private static void clientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            openKey = new KeyMapping("key.cakequests.open", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_K, "key.categories.cakequests");
            ClientRegistry.registerKeyBinding(openKey);
            ItemProperties.register(Items.BOOK, QUEST_BOOK_PROPERTY, (stack, level, entity, seed) -> isNamedQuestBook(stack) ? 1.0F : 0.0F);
        });
    }

    public static void openScreen() {
        Minecraft.getInstance().setScreen(new QuestGraphScreen());
    }

    @SubscribeEvent
    public void keyInput(InputEvent.KeyInputEvent event) {
        if (openKey != null) {
            while (openKey.consumeClick()) {
                openScreen();
            }
        }
    }

    private static boolean isNamedQuestBook(ItemStack stack) {
        return stack.is(Items.BOOK) && stack.hasCustomHoverName() && QUEST_BOOK_NAME.equals(stack.getHoverName().getString());
    }

    @SubscribeEvent
    public void clientLoggedOut(ClientPlayerNetworkEvent.LoggedOutEvent event) {
        ClientQuestGraphStore.clear();
    }

    @SubscribeEvent
    public void clickInput(InputEvent.ClickInputEvent event) {
        if (!event.isUseItem() || Minecraft.getInstance().player == null) {
            return;
        }
        ItemStack stack = Minecraft.getInstance().player.getItemInHand(event.getHand());
        if (isNamedQuestBook(stack)) {
            openScreen();
            event.setSwingHand(false);
            event.setCanceled(true);
        }
    }
}
