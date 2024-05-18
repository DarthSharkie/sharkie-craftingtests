package me.sharkie.minecraft.sharkiecraftingtest;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.recipe.RecipeEntry;
import net.minecraft.recipe.RecipeManager;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public class DualSmelterBlockEntity extends BlockEntity implements NamedScreenHandlerFactory {
    private static final Logger LOGGER = LogManager.getLogger();
    public static final Map<Item, Integer> FUEL_BURN_TIME_MAP = AbstractFurnaceBlockEntity.createFuelTimeMap();

    static BlockEntityType<DualSmelterBlockEntity> BLOCK_ENTITY_TYPE;

    public static void register(String modId, Block block) {
        BLOCK_ENTITY_TYPE = Registry.register(Registries.BLOCK_ENTITY_TYPE,
                                              new Identifier(modId, "dual_smelter_block_entity"),
                                              BlockEntityType.Builder.create(DualSmelterBlockEntity::new, block).build());
    }

    // Data fields

    // Inventory: two inputs, one fuel, one output
    private final SmeltingInventory inventory = SmeltingInventory.ofSize(4);

    private static final String USES_KEY = "uses";
    private int uses = 0;

    private static final String TICKS_KEY = "ticks";
    private int ticks = 0;

    public static final int BURN_TIME_PROPERTY = 0;
    private static final String BURN_TIME_KEY = "burnTime";
    private int burnTime = 0;

    public static final int FUEL_TIME_PROPERTY = 1;
    private int fuelTime = 0;

    public static final int COOK_TIME_PROPERTY = 2;
    private static final String COOK_TIME_KEY = "cookTime";
    private int cookTime = 0;

    public static final int COOK_TIME_TOTAL_PROPERTY = 3;
    private static final String COOK_TIME_TOTAL_KEY = "cookTimeTotal";
    private int cookTimeTotal = 0;

    private final PropertyDelegate propertyDelegate = new PropertyDelegate() {
        @Override
        public int get(int index) {
            if (index == BURN_TIME_PROPERTY) {
                return DualSmelterBlockEntity.this.burnTime;
            }
            if (index == FUEL_TIME_PROPERTY) {
                return DualSmelterBlockEntity.this.fuelTime;
            }
            if (index == COOK_TIME_PROPERTY) {
                return DualSmelterBlockEntity.this.cookTime;
            }
            if (index == COOK_TIME_TOTAL_PROPERTY) {
                return DualSmelterBlockEntity.this.cookTimeTotal;
            }
            return 0;
        }

        @Override
        public void set(int index, int value) {
            if (index == BURN_TIME_PROPERTY) {
                DualSmelterBlockEntity.this.burnTime = value;
            }
            if (index == FUEL_TIME_PROPERTY) {
                DualSmelterBlockEntity.this.fuelTime = value;
            }
            if (index == COOK_TIME_PROPERTY) {
                DualSmelterBlockEntity.this.cookTime = value;
            }
            if (index == COOK_TIME_TOTAL_PROPERTY) {
                DualSmelterBlockEntity.this.cookTimeTotal = value;
            }
        }

        @Override
        public int size() {
            return 4;
        }
    };

    // Added fields for internal logic
    private final RecipeManager.MatchGetter<SmeltingInventory, DualSmelterRecipe> matchGetter;

    public DualSmelterBlockEntity(BlockPos pos, BlockState state) {
        super(BLOCK_ENTITY_TYPE, pos, state);
        this.matchGetter = RecipeManager.createCachedMatchGetter(DualSmelterRecipe.Type.INSTANCE);
    }

    @Override
    protected void writeNbt(NbtCompound nbt) {
        nbt.putInt(USES_KEY, this.uses);
        nbt.putInt(TICKS_KEY, this.ticks);
        nbt.putShort(BURN_TIME_KEY, (short) this.burnTime);
        nbt.putShort(COOK_TIME_KEY, (short) this.cookTime);
        nbt.putShort(COOK_TIME_TOTAL_KEY, (short) this.cookTimeTotal);
        Inventories.writeNbt(nbt, this.inventory.getItems());
        super.writeNbt(nbt);
    }

    @Override
    public void readNbt(NbtCompound nbt) {
        // Read in reverse order
        super.readNbt(nbt);
        this.uses = nbt.getInt(USES_KEY);
        this.ticks = nbt.getInt(TICKS_KEY);
        this.burnTime = nbt.getShort(BURN_TIME_KEY);
        this.cookTime = nbt.getShort(COOK_TIME_KEY);
        this.cookTimeTotal = nbt.getShort(COOK_TIME_TOTAL_KEY);
        Inventories.readNbt(nbt, this.inventory.getItems());

        // Derived property
        this.fuelTime = this.getFuelTime(this.inventory.getStack(2));
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
        final boolean wasBurning = dualSmelterBlockEntity.isBurning();
        boolean burningChanged = false;

        if (wasBurning) {
            // We're burning, need to tick that down
            dualSmelterBlockEntity.burnTime--;
            //LOGGER.info("New burn time: {}/{}", dualSmelterBlockEntity.burnTime, dualSmelterBlockEntity.fuelTime);
        }
        ItemStack fuelStack = dualSmelterBlockEntity.inventory.getFuelStack();
        boolean hasInput1 = !dualSmelterBlockEntity.inventory.getStack(0).isEmpty();
        boolean hasInput2 = !dualSmelterBlockEntity.inventory.getStack(1).isEmpty();
        boolean hasFuel = !fuelStack.isEmpty();

        if (dualSmelterBlockEntity.isBurning() || (hasInput1 && hasInput2 && hasFuel)) {
            // Figure out if there's a legit recipe with these ingredients
            RecipeEntry<DualSmelterRecipe> recipeEntry = dualSmelterBlockEntity.findRecipe();
            if (!dualSmelterBlockEntity.isBurning() && canAcceptRecipeOutput(world.getRegistryManager(),
                                                                             recipeEntry,
                                                                             dualSmelterBlockEntity.inventory)) {
                LOGGER.info("Start burning {} to produce {}", fuelStack, recipeEntry.id());
                // Start the burn!
                dualSmelterBlockEntity.fuelTime = dualSmelterBlockEntity.getFuelTime(fuelStack);
                dualSmelterBlockEntity.burnTime = dualSmelterBlockEntity.getFuelTime(fuelStack);
                dualSmelterBlockEntity.cookTime = 0;
                dualSmelterBlockEntity.cookTimeTotal = recipeEntry.value().getCookingTime();
                // Could be zero if the fuel wasn't found, so double check before using the fuel.
                if (dualSmelterBlockEntity.isBurning()) {
                    burningChanged = true;
                    Item fuel = fuelStack.getItem();
                    fuelStack.decrement(1);
                    if (fuelStack.isEmpty()) {
                        Item fuelRecipeRemainder = fuel.getRecipeRemainder();
                        ItemStack remainderStack = fuelRecipeRemainder == null ? ItemStack.EMPTY : new ItemStack(fuelRecipeRemainder);
                        dualSmelterBlockEntity.inventory.setStack(2, remainderStack);
                    }
                }
            }
            // Now, we could be burning, so check that
            if (dualSmelterBlockEntity.isBurning() && canAcceptRecipeOutput(world.getRegistryManager(),
                                                                            recipeEntry,
                                                                            dualSmelterBlockEntity.inventory)) {
                dualSmelterBlockEntity.cookTime++;
                //LOGGER.info("Cook time: {}/{}", dualSmelterBlockEntity.cookTime, dualSmelterBlockEntity.cookTimeTotal);
                // Might have finished
                if (dualSmelterBlockEntity.cookTime == dualSmelterBlockEntity.cookTimeTotal) {
                    dualSmelterBlockEntity.cookTime = 0;
                    dualSmelterBlockEntity.cookTimeTotal = recipeEntry.value().getCookingTime();
                    if (craftRecipe(world.getRegistryManager(), recipeEntry, dualSmelterBlockEntity.inventory)) {
                        // Set last recipe and drop experience?
                        LOGGER.info("Crafted a {}", recipeEntry.value().getResult(world.getRegistryManager()));
                    }
                    burningChanged = true;
                }
            } else {
                // Maybe burning, but nowhere to put the output, so reset cook time.
                dualSmelterBlockEntity.cookTime = 0;
            }
        } else if (!dualSmelterBlockEntity.isBurning() && dualSmelterBlockEntity.cookTime > 0) {
            // Not burning, but something's partially cooked.  Start "un-cooking it" to mimic the AbstractFurnace.
            dualSmelterBlockEntity.cookTime = Math.min(Math.max(0, dualSmelterBlockEntity.cookTime - 2), dualSmelterBlockEntity.cookTimeTotal);
        }
        if (wasBurning != dualSmelterBlockEntity.isBurning()) {
            burningChanged = true;
            blockState = blockState.with(DualSmelterBlock.LIT, dualSmelterBlockEntity.isBurning());
            world.setBlockState(blockPos, blockState, Block.NOTIFY_ALL);
        }
        if (burningChanged) {
            DualSmelterBlockEntity.markDirty(world, blockPos, blockState);
        }
    }

    private RecipeEntry<DualSmelterRecipe> findRecipe() {
        // TODO: Stub recipe, then appropriate lookups
        return this.matchGetter.getFirstMatch(this.inventory, world).orElse(null);
    }

    private static boolean canAcceptRecipeOutput(DynamicRegistryManager registryManager,
                                                 @Nullable RecipeEntry<?> recipeEntry,
                                                 SmeltingInventory smeltingInventory) {
        if (!smeltingInventory.hasInputs() || recipeEntry == null) {
            // Missing input(s) or missing recipe
            return false;
        }
        ItemStack resultPreviewStack = recipeEntry.value().getResult(registryManager);
        if (resultPreviewStack.isEmpty()) {
            // Recipe doesn't craft anything???
            return false;
        }
        ItemStack outputStack = smeltingInventory.getOutputStack();
        if (outputStack.isEmpty()) {
            // No current output == craft away!
            return true;
        }
        if (!ItemStack.areItemsEqual(outputStack, resultPreviewStack)) {
            // If output already exists, must make more of the exact same item.
            return false;
        }
        if (outputStack.getCount() < smeltingInventory.getMaxCountPerStack() && outputStack.getCount() < outputStack.getMaxCount()) {
            // If the stack is less than both the inventory's max stack size _and_ the item's max stack size, then craft!
            return true;
        }
        // If the output stack is smaller than the output item's max stack size, then craft, else don't.
        return outputStack.getCount() < resultPreviewStack.getMaxCount();
    }

    private static boolean craftRecipe(DynamicRegistryManager registryManager,
                                       @Nullable RecipeEntry<?> recipeEntry,
                                       SmeltingInventory smeltingInventory) {
        if (recipeEntry == null || !canAcceptRecipeOutput(registryManager, recipeEntry, smeltingInventory)) {
            // If no recipe, or the output situation changed, do not craft
            return false;
        }

        // Quick references
        ItemStack inputA = smeltingInventory.getStack(0);
        ItemStack inputB = smeltingInventory.getStack(1);
        ItemStack resultStack = recipeEntry.value().getResult(registryManager);
        ItemStack outputStack = smeltingInventory.getOutputStack();

        if (outputStack.isEmpty()) {
            // No output, so copy over
            smeltingInventory.setStack(3, resultStack.copy());
        } else if (outputStack.isOf(resultStack.getItem())) {
            // Adding to output, so increment appropriately
            outputStack.increment(resultStack.getCount());
        }
        // TODO: Short-term hack!!
        inputA.decrement(3);
        inputB.decrement(1);
        return true;
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

    public Inventory getInventory() {
        return this.inventory;
    }

    @Override
    public Text getDisplayName() {
        return Text.translatable(getCachedState().getBlock().getTranslationKey());
    }

    @Nullable
    @Override
    public ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player) {
        return new DualSmelterScreenHandler(syncId, playerInventory, this.inventory, propertyDelegate);
    }

    private int getFuelTime(ItemStack itemStack) {
        if (itemStack.isEmpty()) {
            return 0;
        }
        return FUEL_BURN_TIME_MAP.getOrDefault(itemStack.getItem(), 0);
    }

    private boolean isBurning() {
        return this.burnTime > 0;
    }
}
