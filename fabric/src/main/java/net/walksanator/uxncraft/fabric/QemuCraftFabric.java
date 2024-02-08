package net.walksanator.uxncraft.fabric;

import net.walksanator.uxncraft.QemuCraft;
import net.fabricmc.api.ModInitializer;

public class QemuCraftFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        QemuCraft.init();
    }
}
