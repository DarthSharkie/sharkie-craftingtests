package me.sharkie.minecraft.sharkiecraftingtest;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.SidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.RecipeInputProvider;
import net.minecraft.recipe.RecipeMatcher;
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
