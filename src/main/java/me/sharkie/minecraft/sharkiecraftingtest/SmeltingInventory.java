package me.sharkie.minecraft.sharkiecraftingtest;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.SidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.RecipeEntry;
import net.minecraft.recipe.RecipeInputProvider;
import net.minecraft.recipe.RecipeMatcher;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.Direction;
import org.jetbrains.annotations.Nullable;

public class SmeltingInventory implements SidedInventory, RecipeInputProvider {

    // Should this simply be an array?
    private final DefaultedList<ItemStack> items;

    private SmeltingInventory(DefaultedList<ItemStack> items) {
        this.items = items;
    }

    public static SmeltingInventory ofSize(int size) {
        return new SmeltingInventory(DefaultedList.ofSize(size, ItemStack.EMPTY));
    }

    public boolean canFitOutput(DynamicRegistryManager registryManager, @Nullable RecipeEntry<?> recipeEntry) {
        if (!this.hasInputs() || recipeEntry == null) {
            // Missing input(s) or missing recipe
            return false;
        }
        ItemStack resultPreviewStack = recipeEntry.value().getResult(registryManager);
        if (resultPreviewStack.isEmpty()) {
            // Recipe doesn't craft anything???
            return false;
        }
        ItemStack outputStack = this.getOutputStack();
        if (outputStack.isEmpty()) {
            // No current output == craft away!
            return true;
        }
        if (!ItemStack.areItemsEqual(outputStack, resultPreviewStack)) {
            // If output already exists, must make more of the exact same item.
            return false;
        }
        if (outputStack.getCount() < this.getMaxCountPerStack() && outputStack.getCount() < outputStack.getMaxCount()) {
            // If the stack is less than both the inventory's max stack size _and_ the item's max stack size, then craft!
            return true;
        }
        // If the output stack is smaller than the output item's max stack size, then craft, else don't.
        return outputStack.getCount() < resultPreviewStack.getMaxCount();
    }

    public boolean craftRecipe(DynamicRegistryManager registryManager, @Nullable RecipeEntry<?> recipeEntry) {
        if (recipeEntry == null || !this.canFitOutput(registryManager, recipeEntry)) {
            // If no recipe, or the output situation changed, do not craft
            return false;
        }

        // Quick references
        ItemStack resultStack = recipeEntry.value().getResult(registryManager);
        ItemStack outputStack = this.getOutputStack();

        if (outputStack.isEmpty()) {
            // No output, so copy over
            this.setStack(3, resultStack.copy());
        } else if (outputStack.isOf(resultStack.getItem())) {
            // Adding to output, so increment appropriately
            outputStack.increment(resultStack.getCount());
        }
        this.getStack(0).decrement(((DualSmelterRecipe) recipeEntry.value()).getInputA().getCount());
        this.getStack(1).decrement(((DualSmelterRecipe) recipeEntry.value()).getInputB().getCount());
        return true;
    }

    /**
     * Retrieves the list of ItemStacks in this Inventory.  Always returns the same instance.
     *
     * @return List<ItemStack>
     */
    public DefaultedList<ItemStack> getItems() {
        return this.items;
    }

    @Override
    public int size() {
        return items.size();
    }

    @Override
    public boolean isEmpty() {
        return items.stream().allMatch(ItemStack::isEmpty);
    }

    @Override
    public ItemStack getStack(int slot) {
        return 0 <= slot && slot < items.size() ? items.get(slot) : ItemStack.EMPTY;
    }

    public boolean hasInputs() {
        return !getStack(0).isEmpty() && !getStack(1).isEmpty();
    }

    public ItemStack getFuelStack() {
        return getStack(2);
    }

    public ItemStack getOutputStack() {
        return getStack(3);
    }

    @Override
    public ItemStack removeStack(int slot, int amount) {
        ItemStack removed = Inventories.splitStack(getItems(), slot, amount);
        if (!removed.isEmpty()) {
            markDirty();
        }
        return removed;
    }

    @Override
    public ItemStack removeStack(int slot) {
        return Inventories.removeStack(getItems(), slot);
    }

    @Override
    public void setStack(int slot, ItemStack stack) {
        this.items.set(slot, stack);
        if (stack.getCount() > stack.getMaxCount()) {
            stack.setCount(stack.getMaxCount());
        }
    }

    @Override
    public void markDirty() {

    }

    @Override
    public boolean canPlayerUse(PlayerEntity player) {
        return true;
    }

    @Override
    public void clear() {
        this.items.clear();
    }

    @Override
    public int[] getAvailableSlots(Direction side) {
        // All slots are available for now.
        int[] slots = new int[this.items.size()];
        for (int i = 0; i < slots.length; i++) {
            slots[i] = i;
        }
        return slots;
    }

    @Override
    public boolean canInsert(int slot, ItemStack stack, @Nullable Direction dir) {
        return true;
    }

    @Override
    public boolean canExtract(int slot, ItemStack stack, Direction dir) {
        return true;
    }

    @Override
    public void provideRecipeInputs(RecipeMatcher finder) {
        for (ItemStack itemStack : this.items) {
            finder.addInput(itemStack);
        }
    }
}
