package net.walksanator.uxncraft.fabric;

import net.fabricmc.api.ModInitializer;
import net.walksanator.uxncraft.UXNCraft;

public class UXNCraftFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        UXNCraft.init();
    }
}
