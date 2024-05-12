package me.sharkie.minecraft.sharkiecraftingtest;

import net.minecraft.block.Block;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class DualSmelterBlock extends Block {

    // Doesn't have an accessor, yet
    private static DualSmelterBlock BLOCK;

    public DualSmelterBlock(Settings settings) {
        super(settings);
    }

    public static DualSmelterBlock register(String modid) {
        // Move to class fields if necessary.
        final String name = "dual_smelter_block";
        final Identifier identifier = new Identifier(modid, name);
        BLOCK = Registry.register(Registries.BLOCK, identifier, new DualSmelterBlock(Settings.create()));

        // Register the related BlockItem, too
        Registry.register(Registries.ITEM, identifier, new BlockItem(BLOCK, new Item.Settings()));
        return BLOCK;
    }
}
