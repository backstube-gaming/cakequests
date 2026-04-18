package net.backstube.cakequests.forge.jei;

import mezz.jei.api.recipe.IFocus;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.ModList;

import java.util.Optional;

public final class CakeQuestsJeiHelper {
    private CakeQuestsJeiHelper() {
    }

    public static void showItemRecipes(String itemId) {
        if (!ModList.get().isLoaded("jei") || CakeQuestsJeiIntegration.runtime == null) {
            return;
        }
        ResourceLocation location;
        try {
            location = new ResourceLocation(itemId);
        } catch (RuntimeException ex) {
            return;
        }
        Optional<Item> item = Registry.ITEM.getOptional(location);
        if (item.isEmpty()) {
            return;
        }
        CakeQuestsJeiIntegration.runtime.getRecipesGui().show(
                CakeQuestsJeiIntegration.runtime.getRecipeManager().createFocus(IFocus.Mode.OUTPUT, new ItemStack(item.get()))
        );
    }
}
