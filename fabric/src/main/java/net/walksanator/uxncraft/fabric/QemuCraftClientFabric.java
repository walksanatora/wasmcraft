package net.walksanator.uxncraft.fabric;

import net.fabricmc.api.ClientModInitializer;
import net.walksanator.uxncraft.QemuCraftClient;

public class QemuCraftClientFabric implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        Shaders.INSTANCE.init();
    }
}
