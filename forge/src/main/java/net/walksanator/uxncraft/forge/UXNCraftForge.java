package net.walksanator.uxncraft.forge;

import dev.architectury.platform.forge.EventBuses;
import net.minecraft.client.Minecraft;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.walksanator.uxncraft.Resources;
import net.walksanator.uxncraft.UXNCraft;
import net.walksanator.uxncraft.UXNCraftClient;

@Mod(UXNCraft.MOD_ID)
public class UXNCraftForge {
    public static int SHADER_NUM = 0;
    public UXNCraftForge() {
        // Submit our event bus to let architectury register our content on the right time
        EventBuses.registerModEventBus(UXNCraft.MOD_ID, FMLJavaModLoadingContext.get().getModEventBus());
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::onClientSetup);
        UXNCraft.init();
    }

    private void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(UXNCraftClient::initClient);
        event.enqueueWork(() -> {
            ResourceManager rm = Minecraft.getInstance().getResourceManager();
            SHADER_NUM = Resources.RES_KT.loadShader(rm,"screen");
        });
    }
}
