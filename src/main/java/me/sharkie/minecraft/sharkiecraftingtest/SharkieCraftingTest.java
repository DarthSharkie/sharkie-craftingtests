package me.sharkie.minecraft.sharkiecraftingtest;

import net.fabricmc.api.ModInitializer;

public class SharkieCraftingTest implements ModInitializer {
    public static final String MODID = "sharkie-craftingtest";

    @Override
    public void onInitialize() {
        DualSmelterBlock.register(MODID);
        DualSmelterBlockEntity.register(MODID, DualSmelterBlock.BLOCK);
    }
}
