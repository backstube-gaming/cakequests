package net.backstube.cakequests.forge.jei;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.runtime.IJeiRuntime;
import net.backstube.cakequests.CakeQuests;
import net.minecraft.resources.ResourceLocation;

@JeiPlugin
public class CakeQuestsJeiIntegration implements IModPlugin {
    private static final ResourceLocation UID = CakeQuests.id("jei");
    static IJeiRuntime runtime;

    @Override
    public void onRuntimeAvailable(IJeiRuntime runtime) {
        CakeQuestsJeiIntegration.runtime = runtime;
    }

    @Override
    public ResourceLocation getPluginUid() {
        return UID;
    }
}
