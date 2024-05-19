package me.sharkie.minecraft.sharkiecraftingtest;

import me.sharkie.MinecraftSetupExtension;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.world.World;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;

import static org.mockito.Mockito.mock;

@ExtendWith(MinecraftSetupExtension.class)
class DualSmelterRecipeTest {

    @org.junit.jupiter.api.Test
    void matches() {
        SmeltingInventory inventory = SmeltingInventory.ofSize(4);
        inventory.setStack(0, new ItemStack(Items.COPPER_INGOT, 3));
        inventory.setStack(1, new ItemStack(Items.GOLD_INGOT));

        DualSmelterRecipe recipe = new DualSmelterRecipe(List.of(new ItemStack(Items.COPPER_INGOT, 3), new ItemStack(Items.GOLD_INGOT)),
                                                         new ItemStack(SharkieCraftingTest.ROSE_GOLD_INGOT, 4),
                                                         "alloys");

        // Act
        boolean result = recipe.matches(inventory, mock(World.class));

        Assertions.assertTrue(result);
    }

    @org.junit.jupiter.api.Test
    void matches_fails_with_inverted_inputs() {
        SmeltingInventory inventory = SmeltingInventory.ofSize(4);
        inventory.setStack(0, new ItemStack(Items.GOLD_INGOT));
        inventory.setStack(1, new ItemStack(Items.COPPER_INGOT, 3));

        DualSmelterRecipe recipe = new DualSmelterRecipe(List.of(new ItemStack(Items.COPPER_INGOT, 3), new ItemStack(Items.GOLD_INGOT)),
                                                         new ItemStack(SharkieCraftingTest.ROSE_GOLD_INGOT, 4),
                                                         "misc",
                                                         200);

        // Act
        boolean result = recipe.matches(inventory, mock(World.class));

        Assertions.assertFalse(result);
    }

    @org.junit.jupiter.api.Test
    void matches_fails_with_insufficient_inventory_count() {
        SmeltingInventory inventory = SmeltingInventory.ofSize(4);
        inventory.setStack(0, new ItemStack(Items.COPPER_INGOT, 2));
        inventory.setStack(1, new ItemStack(Items.GOLD_INGOT));

        DualSmelterRecipe recipe = new DualSmelterRecipe(List.of(new ItemStack(Items.COPPER_INGOT, 3), new ItemStack(Items.GOLD_INGOT)),
                                                         new ItemStack(SharkieCraftingTest.ROSE_GOLD_INGOT, 4),
                                                         "misc",
                                                         200);

        // Act
        boolean result = recipe.matches(inventory, mock(World.class));

        Assertions.assertFalse(result);
    }
}