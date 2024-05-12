package me.sharkie.minecraft.sharkiecraftingtest;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class DualSmelterBlockEntity extends BlockEntity {
    static BlockEntityType<DualSmelterBlockEntity> BLOCK_ENTITY_TYPE;

    public static void register(String modid, Block block) {
        BLOCK_ENTITY_TYPE = Registry.register(Registries.BLOCK_ENTITY_TYPE,
                                              new Identifier(modid, "dual_smelter_block_entity"),
                                              BlockEntityType.Builder.create(DualSmelterBlockEntity::new, block).build());
    }

    // Data fields
    private static final String USES_KEY = "uses";
    private int uses = 0;

    private static final String TICKS_KEY = "ticks";
    private int ticks = 0;

    public DualSmelterBlockEntity(BlockPos pos, BlockState state) {
        super(BLOCK_ENTITY_TYPE, pos, state);
    }

    @Override
    protected void writeNbt(NbtCompound nbt) {
        nbt.putInt(USES_KEY, this.uses);
        super.writeNbt(nbt);
    }

    @Override
    public void readNbt(NbtCompound nbt) {
        // Read in reverse order
        super.readNbt(nbt);
        this.uses = nbt.getInt(USES_KEY);
    }

    @Nullable
    @Override
    public Packet<ClientPlayPacketListener> toUpdatePacket() {
        // Allow sending data to client, if necessary
        return BlockEntityUpdateS2CPacket.create(this);
    }

    @Override
    public NbtCompound toInitialChunkDataNbt() {
        // Allow sending data to client, if necessary
        return createNbt();
    }

    // Runs logic on game tick
    public static void tick(World world, BlockPos blockPos, BlockState blockState, DualSmelterBlockEntity dualSmelterBlockEntity) {
        dualSmelterBlockEntity.ticks++;
        markDirty(world, blockPos, blockState);
    }

    public void incrementUses() {
        this.uses++;
    }

    public int getUses() {
        return this.uses;
    }

    public int getTicks() {
        return this.ticks;
    }
}
