package me.sharkie.minecraft.sharkiecraftingtest;

import net.minecraft.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.Recipe;
import net.minecraft.recipe.RecipeEntry;
import net.minecraft.recipe.RecipeInputProvider;
import net.minecraft.recipe.RecipeMatcher;
import net.minecraft.recipe.book.RecipeBookCategory;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.resource.featuretoggle.FeatureFlags;
import net.minecraft.screen.AbstractRecipeScreenHandler;
import net.minecraft.screen.ArrayPropertyDelegate;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;

import java.util.Map;

public class DualSmelterScreenHandler extends AbstractRecipeScreenHandler<Inventory> {

    public static ScreenHandlerType<DualSmelterScreenHandler> SCREEN_HANDLER_TYPE;

    private static final TagKey<Item> INGOTS_TAGKEY = TagKey.of(RegistryKeys.ITEM, new Identifier("c", "ingots"));
    private static final Map<Item, Integer> FUEL_BURN_TIME_MAP = AbstractFurnaceBlockEntity.createFuelTimeMap();

    private static final int OUTPUT_SLOT_INDEX = 3;

    public static void register(String modid) {
        SCREEN_HANDLER_TYPE = Registry.register(Registries.SCREEN_HANDLER,
                                                new Identifier(modid, "dual_smelter_screen_handler"),
                                                new ScreenHandlerType<>(DualSmelterScreenHandler::new, FeatureFlags.VANILLA_FEATURES));
    }

    private final Inventory inventory;
    private final PropertyDelegate propertyDelegate;
    private final World world;

    /**
     * Client entry point; called by the server when the server wants the client to open this ScreenHandler.
     */
    public DualSmelterScreenHandler(int syncId, PlayerInventory playerInventory) {
        this(syncId, playerInventory, SmeltingInventory.ofSize(4), new ArrayPropertyDelegate(4));
    }

    /**
     * BlockEntity entry point.  The provided container inventory is known by the entity and synced to the server.
     */
    public DualSmelterScreenHandler(int syncId, PlayerInventory playerInventory, SmeltingInventory inventory, PropertyDelegate propertyDelegate) {
        super(SCREEN_HANDLER_TYPE, syncId);
        checkSize(inventory, 4);
        this.inventory = inventory;
        // Allow logic on opening inventory
        inventory.onOpen(playerInventory.player);

        // Configure the PropertyDelegate; this syncs state between logical client/server
        this.propertyDelegate = propertyDelegate;
        this.addProperties(this.propertyDelegate);

        // Get a reference to the World
        this.world = playerInventory.player.getWorld();

        // Compute Slots shown on this screen, including crafting slots, player inventory, and player hotbar
        // Crafting
        this.addSlot(new Slot(inventory, 0, 36, 17));   // 16x16 input 1
        this.addSlot(new Slot(inventory, 1, 56, 17));   // 16x16 input 2
        this.addSlot(new Slot(inventory, 2, 46, 53));   // 16x16 fuel slot
        this.addSlot(new OutputSlot(inventory, 3, 116, 35));  // 24x24 output slot, offset by (4,4) to center it

        //The player inventory
        for (int m = 0; m < 3; m++) {
            for (int l = 0; l < 9; l++) {
                // Player inventory has hotbar first, then the "bag"
                this.addSlot(new Slot(playerInventory, l + m * 9 + 9, 8 + l * 18, 84 + m * 18));
            }
        }
        //The player Hotbar
        for (int i = 0; i < 9; i++) {
            this.addSlot(new Slot(playerInventory, i, 8 + i * 18, 142));
        }
    }

    /**
     * Returns burn progress clamped between [0.0, 1.0]
     */
    public float getBurnProgress() {
        int burnTime = propertyDelegate.get(DualSmelterBlockEntity.BURN_TIME_PROPERTY);
        int fuelTime = propertyDelegate.get(DualSmelterBlockEntity.FUEL_TIME_PROPERTY);
        if (burnTime == 0 || fuelTime == 0) {
            return 0.0f;
        }
        return Math.min(Math.max(0.0f, (float) burnTime / (float) fuelTime), 1.0f);
    }

    public boolean isBurning() {
        return propertyDelegate.get(DualSmelterBlockEntity.BURN_TIME_PROPERTY) > 0;
    }

    /**
     * Returns cook progress clamped between [0.0, 1.0]
     */
    public float getCookProgress() {
        int cookTime = propertyDelegate.get(DualSmelterBlockEntity.COOK_TIME_PROPERTY);
        int cookTimeTotal = propertyDelegate.get(DualSmelterBlockEntity.COOK_TIME_TOTAL_PROPERTY);
        if (cookTime == 0 || cookTimeTotal == 0) {
            return 0.0f;
        }
        return Math.min(Math.max(0.0f, (float) cookTime / (float) cookTimeTotal), 1.0f);
    }

    @Override
    public void onClosed(PlayerEntity player) {
        super.onClosed(player);
    }

    @Override
    public boolean canInsertIntoSlot(ItemStack stack, Slot slot) {
        return slot.getIndex() != OUTPUT_SLOT_INDEX && super.canInsertIntoSlot(stack, slot);
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int slotId) {
        // This handles Shift + Player Inventory (inserting into this screen's inventory)
        Slot slot = this.slots.get(slotId);
        if (!slot.hasStack()) {
            return ItemStack.EMPTY;
        }

        ItemStack stack = slot.getStack();
        ItemStack originalStack = stack.copy();
        if (slotId == 3) {
            // Source slot is this UI's output, try to return to player hotbar, then player inventory
            if (!this.insertItem(stack, this.inventory.size(), this.slots.size(), true)) {
                return ItemStack.EMPTY;
            }
            slot.onQuickTransfer(stack, originalStack);
        } else if (slotId < this.inventory.size()) {
            // Source slot is specific to this UI, try to return to player inventory, then player hotbar
            if (!this.insertItem(stack, this.inventory.size(), this.slots.size(), false)) {
                return ItemStack.EMPTY;
            }
        } else if (stack.isIn(INGOTS_TAGKEY) && slotId > this.inventory.size()) {
            // Source stack is meltable, and source slot is player inventory/hotbar; try to move into melting slots
            if (!this.insertItem(stack, 0, 2, false)) {
                return ItemStack.EMPTY;
            }
        } else if (this.isFuel(stack) && slotId > this.inventory.size()) {
            // Source stack is fuel, and source slot is player inventory/hotbar; try to move into fuel slot
            if (!this.insertItem(stack, 2, 3, false)) {
                return ItemStack.EMPTY;
            }
        }

        if (stack.isEmpty()) {
            slot.setStack(ItemStack.EMPTY);
        } else {
            slot.markDirty();
        }
        if (originalStack.getCount() == stack.getCount()) {
            return ItemStack.EMPTY;
        }
        slot.onTakeItem(player, stack);
        return originalStack;
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return this.inventory.canPlayerUse(player);
    }

    private boolean isFuel(ItemStack itemStack) {
        return FUEL_BURN_TIME_MAP.containsKey(itemStack.getItem());
    }

    @Override
    public void populateRecipeFinder(RecipeMatcher finder) {
        if (this.inventory instanceof RecipeInputProvider recipeInputProvider) {
            recipeInputProvider.provideRecipeInputs(finder);
        }
    }

    @Override
    public void clearCraftingSlots() {
        this.getSlot(0).setStackNoCallbacks(ItemStack.EMPTY);
        this.getSlot(1).setStackNoCallbacks(ItemStack.EMPTY);
        this.getSlot(3).setStackNoCallbacks(ItemStack.EMPTY);
    }

    @Override
    public boolean matches(RecipeEntry<? extends Recipe<Inventory>> recipe) {
        return recipe.value().matches(this.inventory, this.world);
    }

    @Override
    public int getCraftingResultSlotIndex() {
        return 3;
    }

    @Override
    public int getCraftingWidth() {
        return 2;
    }

    @Override
    public int getCraftingHeight() {
        return 1;
    }

    @Override
    public int getCraftingSlotCount() {
        return 2;
    }

    @Override
    public RecipeBookCategory getCategory() {
        return RecipeBookCategory.FURNACE;
    }

    @Override
    public boolean canInsertIntoSlot(int index) {
        return index != 2;
    }

    static class OutputSlot extends Slot {
        public OutputSlot(Inventory inventory, int index, int x, int y) {
            super(inventory, index, x, y);
        }

        @Override
        public boolean canInsert(ItemStack stack) {
            return false;
        }
    }
}