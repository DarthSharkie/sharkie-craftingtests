package me.sharkie.minecraft.sharkiecraftingtest;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.HorizontalFacingBlock;
import net.minecraft.datafixer.fix.ChunkPalettedStorageFix;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

public class DualSmelterBlock extends Block {
    private static final DirectionProperty FACING = HorizontalFacingBlock.FACING;
    private static final BooleanProperty LIT = Properties.LIT;

    // Doesn't have an accessor, yet
    private static DualSmelterBlock BLOCK;

    public DualSmelterBlock(Settings settings) {
        super(settings);
        setDefaultState(getDefaultState().with(FACING, Direction.NORTH).with(LIT, false));
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

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FACING).add(LIT);
        super.appendProperties(builder);
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (!world.isClient()) {
            player.sendMessage(Text.literal("USed!"), false);
        }
        return ActionResult.SUCCESS;
    }

    @Override
    public BlockState rotate(BlockState state, BlockRotation rotation) {
        return state.with(FACING, rotation.rotate(state.get(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, BlockMirror mirror) {
        return state.rotate(mirror.getRotation(state.get(FACING)));
    }
}
