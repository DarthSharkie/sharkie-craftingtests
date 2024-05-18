package me.sharkie.minecraft.sharkiecraftingtest;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class SharkieCraftingTest implements ModInitializer {
    public static final String MODID = "sharkie-craftingtest";

    public static final Item ROSE_GOLD_INGOT = Registry.register(Registries.ITEM,
                                                                 new Identifier(MODID, "rose_gold_ingot"),
                                                                 new Item(new Item.Settings()));

    @Override
    public void onInitialize() {
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.INGREDIENTS).register(group -> group.addAfter(Items.GOLD_INGOT, ROSE_GOLD_INGOT));

        DualSmelterBlock.register(MODID);
        DualSmelterBlockEntity.register(MODID, DualSmelterBlock.BLOCK);
        DualSmelterScreenHandler.register(MODID);
        DualSmelterRecipe.register(MODID);
    }
}
