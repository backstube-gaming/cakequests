package net.backstube.cakequests.data;

import com.google.gson.JsonObject;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public record QuestIcon(ResourceLocation item) {
    public static final QuestIcon DEFAULT = new QuestIcon(new ResourceLocation("minecraft", "book"));

    public static QuestIcon fromJson(JsonObject json) {
        if (json == null || !json.has("item")) {
            return DEFAULT;
        }
        try {
            return new QuestIcon(new ResourceLocation(json.get("item").getAsString()));
        } catch (RuntimeException ex) {
            return DEFAULT;
        }
    }

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("item", item.toString());
        return json;
    }

    public ItemStack stack() {
        Item itemValue = Registry.ITEM.getOptional(item).orElse(Items.BOOK);
        return new ItemStack(itemValue == null ? Items.BOOK : itemValue);
    }
}
