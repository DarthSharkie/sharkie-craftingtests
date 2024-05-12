package me.sharkie.minecraft.sharkiecraftingtest.client;

import net.fabricmc.api.ClientModInitializer;

public class SharkieCraftingTestClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        DualSmelterScreen.register();
    }
}
