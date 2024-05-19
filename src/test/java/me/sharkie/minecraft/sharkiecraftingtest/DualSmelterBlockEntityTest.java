package me.sharkie.minecraft.sharkiecraftingtest;

import com.mojang.datafixers.util.Pair;
import me.sharkie.MinecraftSetupExtension;
import net.minecraft.block.BlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.recipe.RecipeEntry;
import net.minecraft.recipe.RecipeManager;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MinecraftSetupExtension.class)
class DualSmelterBlockEntityTest {

    public static final Identifier RECIPE_ID = new Identifier("sharkie-craftingtest:rose_gold_ingot");
    public static final DualSmelterRecipe RECIPE = new DualSmelterRecipe(List.of(new ItemStack(Items.COPPER_INGOT, 3),
                                                                                 new ItemStack(Items.GOLD_INGOT)),
                                                                         new ItemStack(SharkieCraftingTest.ROSE_GOLD_INGOT, 4),
                                                                         "alloys",
                                                                         250);

    @Test
    void tick_idleWithSufficientInventoryAndFuelAndOutputSpaceForRecipe() throws IOException {
        try (World world = mock(World.class)) {
            // Given mock parameters
            BlockPos mockPos = mock(BlockPos.class);
            BlockState mockState = mock(BlockState.class);
            when(mockState.with(DualSmelterBlock.LIT, true)).thenReturn(mockState);
            RecipeManager mockRecipeManager = mock(RecipeManager.class);
            when(world.getRecipeManager()).thenReturn(mockRecipeManager);

            // Given a SmeltingInventory
            SmeltingInventory inventory = SmeltingInventory.ofSize(4);
            inventory.setStack(0, new ItemStack(Items.COPPER_INGOT, 3));
            inventory.setStack(1, new ItemStack(Items.GOLD_INGOT, 1));
            inventory.setStack(2, new ItemStack(Items.COAL, 1));

            // Given the BlockEntity
            DualSmelterBlockEntity entity = new DualSmelterBlockEntity(mockPos, mockState);
            entity.setInventory(inventory);

            // Given the recipe
            Optional<Pair<Identifier, RecipeEntry<DualSmelterRecipe>>> pair = Optional.of(Pair.of(RECIPE_ID, new RecipeEntry<>(RECIPE_ID, RECIPE)));
            when(mockRecipeManager.getFirstMatch(DualSmelterRecipe.Type.INSTANCE, inventory, world, null)).thenReturn(pair);

            // When the game ticks
            entity.tick(world, mockPos, mockState);

            // Then the world updates
            verify(world).setBlockState(mockPos, mockState, DualSmelterBlock.NOTIFY_ALL);
            verify(world).markDirty(mockPos);
            assertEquals(1600, entity.getBurnTime());
            assertEquals(0, entity.getCookTime());
            assertEquals(250, entity.getCookTimeTotal());
            assertTrue(entity.isBurning());
            assertEquals(0, inventory.getStack(2).getCount(), "Fuel stack should be empty");
        }
    }

    @Test
    void tick_activeRecipeWithSufficientInventoryAndFuelAndOutputSpace() throws IOException {
        try (World world = mock(World.class)) {
            // Given mock parameters
            BlockPos mockPos = mock(BlockPos.class);
            BlockState mockState = mock(BlockState.class);
            RecipeManager mockRecipeManager = mock(RecipeManager.class);

            // Given a SmeltingInventory
            SmeltingInventory inventory = SmeltingInventory.ofSize(4);
            inventory.setStack(0, new ItemStack(Items.COPPER_INGOT, 3));
            inventory.setStack(1, new ItemStack(Items.GOLD_INGOT, 1));
            inventory.setStack(2, new ItemStack(Items.COAL, 1));

            // Given the BlockEntity
            DualSmelterBlockEntity entity = new DualSmelterBlockEntity(mockPos, mockState);
            entity.setInventory(inventory);
            entity.setCurrentRecipe(new RecipeEntry<>(RECIPE_ID, RECIPE));
            // Value not important, just needs to be greater than 1 so the recipe doesn't finish on this tick
            entity.setCookTimeTotal(250);
            entity.setBurnTime(1600);

            // When the game ticks
            entity.tick(world, mockPos, mockState);

            // Then the world updates
            verify(world).markDirty(mockPos);
            assertEquals(1599, entity.getBurnTime());
            assertEquals(1, entity.getCookTime());
            assertTrue(entity.isBurning());
        }
    }

    //@Test
    void tick_finishingRecipeWithSufficientInventoryAndFuelAndOutputSpace() throws IOException {
        try (World world = mock(World.class)) {
            // Given mock parameters
            BlockPos mockPos = mock(BlockPos.class);
            BlockState mockState = mock(BlockState.class);
            RecipeManager mockRecipeManager = mock(RecipeManager.class);

            // Given a SmeltingInventory
            SmeltingInventory inventory = SmeltingInventory.ofSize(4);
            inventory.setStack(0, new ItemStack(Items.COPPER_INGOT, 3));
            inventory.setStack(1, new ItemStack(Items.GOLD_INGOT, 1));
            inventory.setStack(2, new ItemStack(Items.COAL, 1));

            // Given the BlockEntity
            DualSmelterBlockEntity entity = new DualSmelterBlockEntity(mockPos, mockState);
            entity.setInventory(inventory);
            entity.setCurrentRecipe(new RecipeEntry<>(RECIPE_ID, RECIPE));
            // Value not important, just needs to be greater than 1 so the recipe doesn't finish on this tick
            entity.setCookTimeTotal(250);
            entity.setBurnTime(1600);
            entity.setCookTime(250);
            entity.setCookTimeTotal(250);

            // When the game ticks
            entity.tick(world, mockPos, mockState);

            // Then the world updates
            verify(world).markDirty(mockPos);
            assertEquals(1599, entity.getBurnTime());
            assertEquals(0, entity.getCookTime());
            assertNull(entity.getCurrentRecipe());
            assertEquals(new RecipeEntry<DualSmelterRecipe>(RECIPE_ID, RECIPE), entity.getLastRecipe());

            // Then the inventory updates
            assertEquals(0, inventory.getStack(0).getCount());
            assertEquals(0, inventory.getStack(1).getCount());
            assertEquals(4, inventory.getStack(3).getCount());
        }
    }
}