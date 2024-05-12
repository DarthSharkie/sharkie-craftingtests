package me.sharkie.minecraft.sharkiecraftingtest;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.inventory.Inventories;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

public class DualSmelterBlockEntity extends BlockEntity {
    private static final Logger LOGGER = LogManager.getLogger();

    static BlockEntityType<DualSmelterBlockEntity> BLOCK_ENTITY_TYPE;

    public static void register(String modid, Block block) {
        BLOCK_ENTITY_TYPE = Registry.register(Registries.BLOCK_ENTITY_TYPE,
                                              new Identifier(modid, "dual_smelter_block_entity"),
                                              BlockEntityType.Builder.create(DualSmelterBlockEntity::new, block).build());
    }

    // Data fields

    // Inventory: two inputs, one fuel, one output
    private static final String INVENTORY_KEY = "inventory";
    private final SmeltingInventory inventory = SmeltingInventory.ofSize(4);

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
        nbt.putInt(TICKS_KEY, this.ticks);
        Inventories.writeNbt(nbt, this.inventory.getItems());
        super.writeNbt(nbt);
    }

    @Override
    public void readNbt(NbtCompound nbt) {
        // Read in reverse order
        super.readNbt(nbt);
        this.uses = nbt.getInt(USES_KEY);
        this.ticks = nbt.getInt(TICKS_KEY);
        Inventories.readNbt(nbt, this.inventory.getItems());
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
        markDirty();
    }

    public int getUses() {
        return this.uses;
    }

    public int getTicks() {
        return this.ticks;
    }

    public boolean insertItemStack(ItemStack itemStack) {
        if (this.inventory.getStack(0).isEmpty()) {
            LOGGER.info("Putting {} in slot 0", itemStack);
            this.inventory.setStack(0, itemStack);
            return true;
        } else if (this.inventory.getStack(1).isEmpty()) {
            LOGGER.info("Putting {} in slot 1", itemStack);
            this.inventory.setStack(1, itemStack);
            return true;
        } else if (this.inventory.getStack(2).isEmpty()) {
            LOGGER.info("Putting {} in slot 2", itemStack);
            this.inventory.setStack(2, itemStack);
            return true;
        } else {
            LOGGER.info("No slot available for {}!", itemStack);
            return false;
        }
    }

    public boolean hasOutput() {
        return !this.inventory.getStack(3).isEmpty();
    }

    public ItemStack takeOutput() {
        return this.inventory.removeStack(3);
    }
}
