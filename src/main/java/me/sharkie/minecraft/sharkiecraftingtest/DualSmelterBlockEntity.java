package me.sharkie.minecraft.sharkiecraftingtest;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.ExperienceOrbEntity;
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
import net.minecraft.recipe.RecipeUnlocker;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public class DualSmelterBlockEntity extends BlockEntity implements NamedScreenHandlerFactory, RecipeUnlocker {
    private static final Logger LOGGER = LogManager.getLogger();
    public static final Map<Item, Integer> FUEL_BURN_TIME_MAP = AbstractFurnaceBlockEntity.createFuelTimeMap();

    static BlockEntityType<DualSmelterBlockEntity> BLOCK_ENTITY_TYPE;
    @Nullable
    private PlayerEntity player;

    public static void register(String modId, Block block) {
        BLOCK_ENTITY_TYPE = Registry.register(Registries.BLOCK_ENTITY_TYPE,
                                              new Identifier(modId, "dual_smelter_block_entity"),
                                              BlockEntityType.Builder.create(DualSmelterBlockEntity::new, block).build());
    }

    // Data fields

    // Inventory: two inputs, one fuel, one output
    private final SmeltingInventory inventory = SmeltingInventory.ofSize(4);

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

    private RecipeEntry<DualSmelterRecipe> currentRecipe;

    @Nullable
    private RecipeEntry<?> lastRecipe;

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

    // Storage and rehydration on save/load
    @Override
    protected void writeNbt(NbtCompound nbt) {
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
        dualSmelterBlockEntity.tick(world, blockPos, blockState);
    }

    private void tick(World world, BlockPos blockPos, BlockState blockState) {
        final boolean isBurningThisTick = this.isBurning();

        // Figure out if there's a legit recipe with these ingredients
        if (this.currentRecipe == null) {
            RecipeEntry<DualSmelterRecipe> proposedRecipe = this.matchGetter.getFirstMatch(this.inventory, world).orElse(null);
            if (proposedRecipe != null && this.inventory.canFitOutput(proposedRecipe.value().getOutput())) {
                this.currentRecipe = proposedRecipe;
            }
        }

        if (this.currentRecipe != null) {
            // If inputs are missing, reset progress
            if (!this.currentRecipe.value().matches(this.inventory, world)) {
                this.currentRecipe = null;
                this.cookTime = 0;
            }

            // Ready for action?
            if (currentRecipe != null && this.currentRecipe.value().matches(this.inventory, world)) {
                // Check for done
                if (this.cookTime >= this.cookTimeTotal) {
                    boolean outputFits = this.inventory.canFitOutput(this.currentRecipe.value().getOutput());
                    if (outputFits) {
                        this.inventory.craftRecipe(currentRecipe.value());
                        this.onCraftedRecipe(this.currentRecipe);

                        // Reset for next tick
                        this.currentRecipe = null;
                        this.cookTime = 0;
                    }
                } else {
                    if (isBurningThisTick) {
                        this.cookTime++;
                    } else {
                        // Out of fuel, start more if there's a recipe that needs fuel
                        ItemStack fuelStack = this.inventory.getFuelStack();
                        if (!fuelStack.isEmpty()) {
                            this.fuelTime = this.getFuelTime(fuelStack);
                            this.burnTime = this.getFuelTime(fuelStack);
                            fuelStack.decrement(1);
                            if (fuelStack.isEmpty()) {
                                Item fuelRecipeRemainder = fuelStack.getItem().getRecipeRemainder();
                                ItemStack remainderStack = fuelRecipeRemainder == null ? ItemStack.EMPTY : new ItemStack(fuelRecipeRemainder);
                                this.inventory.setStack(2, remainderStack);
                            }
                        } else {
                            // Future plan: change to scrap metal recipe and output???
                            this.cookTime = Math.min(Math.max(0, this.cookTime - 1), this.cookTimeTotal);
                        }
                    }
                }
            }
        }

        // Now process fuel
        if (isBurningThisTick) {
            this.burnTime--;
        }

        if (isBurningThisTick != this.isBurning()) {
            blockState = blockState.with(DualSmelterBlock.LIT, this.isBurning());
            world.setBlockState(blockPos, blockState, Block.NOTIFY_ALL);
        }
        markDirty(world, blockPos, blockState);
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
        this.player = playerInventory.player;
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

    @Override
    public void setLastRecipe(@Nullable RecipeEntry<?> recipe) {
        this.lastRecipe = recipe;
    }

    @Nullable
    @Override
    public RecipeEntry<?> getLastRecipe() {
        return this.lastRecipe;
    }

    private void onCraftedRecipe(RecipeEntry<DualSmelterRecipe> recipeEntry) {
        setLastRecipe(recipeEntry);
        DualSmelterRecipe dualSmelterRecipe = recipeEntry.value();
        unlockLastRecipe(this.player, dualSmelterRecipe.getInputs());
        dropExperience(dualSmelterRecipe);
    }

    private void dropExperience(DualSmelterRecipe dualSmelterRecipe) {
        // Amortize fractional points
        int intExp = MathHelper.floor(dualSmelterRecipe.getExperience());
        float fracExp = MathHelper.fractionalPart(dualSmelterRecipe.getExperience());
        if (fracExp > 0.0F && Math.random() < fracExp) {
            intExp++;
        }
        assert this.player != null;
        // Drop XP wherever the player is; no need to come back to the entity.
        ExperienceOrbEntity.spawn(((ServerPlayerEntity) this.player).getServerWorld(), this.player.getPos(), intExp);
    }
}
