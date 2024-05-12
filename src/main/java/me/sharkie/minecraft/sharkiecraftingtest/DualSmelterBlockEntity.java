package me.sharkie.minecraft.sharkiecraftingtest;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

public class DualSmelterBlockEntity extends BlockEntity {
    private static BlockEntityType<DualSmelterBlockEntity> BLOCK_ENTITY_TYPE;

    public static void register(String modid, Block block) {
        BLOCK_ENTITY_TYPE = Registry.register(Registries.BLOCK_ENTITY_TYPE,
                                              new Identifier(modid, "dual_smelter_block_entity"),
                                              BlockEntityType.Builder.create(DualSmelterBlockEntity::new, block).build());
    }

    public DualSmelterBlockEntity(BlockPos pos, BlockState state) {
        super(BLOCK_ENTITY_TYPE, pos, state);
    }

}
