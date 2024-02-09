package net.walksanator.uxncraft.fabric;

import net.fabricmc.api.ClientModInitializer;

public class UXNCraftClientFabric implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        Shaders.INSTANCE.init();
    }
}
