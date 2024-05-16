package me.sharkie.minecraft.sharkiecraftingtest;

import net.minecraft.Bootstrap;
import net.minecraft.SharedConstants;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.recipe.Ingredient;
import net.minecraft.world.World;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;

import static org.mockito.Mockito.mock;

class DualSmelterRecipeTest {

    @BeforeAll
    public static void setup() {
        SharedConstants.createGameVersion();
        Bootstrap.initialize();
    }

    @org.junit.jupiter.api.Test
    void matches() {
        SmeltingInventory inventory = SmeltingInventory.ofSize(4);
        inventory.setStack(0, new ItemStack(Items.COPPER_INGOT));
        inventory.setStack(1, new ItemStack(Items.GOLD_INGOT));

        DualSmelterRecipe recipe = new DualSmelterRecipe(Ingredient.ofItems(Items.COPPER_INGOT),
                                                         Ingredient.ofItems(Items.GOLD_INGOT),
                                                         new ItemStack(Items.NETHERITE_INGOT),
                                                         "misc",
                                                         200);

        // Act
        boolean result = recipe.matches(inventory, mock(World.class));

        Assertions.assertTrue(result);

    }

    @org.junit.jupiter.api.Test
    void matches_fails_with_inverted_inputs() {
        SmeltingInventory inventory = SmeltingInventory.ofSize(4);
        inventory.setStack(0, new ItemStack(Items.COPPER_INGOT));
        inventory.setStack(1, new ItemStack(Items.GOLD_INGOT));

        DualSmelterRecipe recipe = new DualSmelterRecipe(Ingredient.ofItems(Items.GOLD_INGOT),
                                                         Ingredient.ofItems(Items.COPPER_INGOT),
                                                         new ItemStack(Items.NETHERITE_INGOT),
                                                         "misc",
                                                         200);

        // Act
        boolean result = recipe.matches(inventory, mock(World.class));

        Assertions.assertFalse(result);

    }
}