package me.sharkie.minecraft.sharkiecraftingtest;

import com.mojang.datafixers.util.Pair;
import me.sharkie.MinecraftSetupExtension;
import net.minecraft.block.BlockState;
import net.minecraft.entity.ExperienceOrbEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.recipe.RecipeEntry;
import net.minecraft.recipe.RecipeManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MinecraftSetupExtension.class)
class DualSmelterBlockEntityTest {

    public static final Identifier RECIPE_ID = new Identifier("sharkie-craftingtest:rose_gold_ingot");
    public static final DualSmelterRecipe RECIPE = new DualSmelterRecipe(List.of(new ItemStack(Items.COPPER_INGOT, 3),
                                                                                 new ItemStack(Items.GOLD_INGOT)),
                                                                         new ItemStack(SharkieCraftingTest.ROSE_GOLD_INGOT, 4),
                                                                         "alloys",
                                                                         250,
                                                                         1.0F);

    @Test
    void tick_startsNewRecipe() throws IOException {
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
            entity.setBurnTime(0);

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
    void tick_cannotStartNewRecipe() throws IOException {
        try (World world = mock(World.class)) {
            // Given mock parameters
            BlockPos mockPos = mock(BlockPos.class);
            BlockState mockState = mock(BlockState.class);
            RecipeManager mockRecipeManager = mock(RecipeManager.class);
            when(world.getRecipeManager()).thenReturn(mockRecipeManager);

            // Given a SmeltingInventory
            SmeltingInventory inventory = SmeltingInventory.ofSize(4);
            inventory.setStack(0, new ItemStack(Items.COPPER_INGOT, 2));
            inventory.setStack(1, new ItemStack(Items.GOLD_INGOT, 1));
            inventory.setStack(2, new ItemStack(Items.COAL, 1));

            // Given the BlockEntity
            DualSmelterBlockEntity entity = new DualSmelterBlockEntity(mockPos, mockState);
            entity.setInventory(inventory);

            // Given the recipe
            when(mockRecipeManager.getFirstMatch(DualSmelterRecipe.Type.INSTANCE, inventory, world, null)).thenReturn(Optional.empty());

            // When the game ticks
            entity.tick(world, mockPos, mockState);

            // Then the world updates
            verify(world).markDirty(mockPos);
            assertEquals(0, entity.getBurnTime());
            assertEquals(0, entity.getCookTime());
            assertEquals(0, entity.getCookTimeTotal());
            assertFalse(entity.isBurning());
            assertEquals(2, inventory.getStack(0).getCount(), "Should be unchanged");
            assertEquals(1, inventory.getStack(1).getCount(), "Should be unchanged");
            assertEquals(1, inventory.getStack(2).getCount(), "Should be unchanged");
        }
    }

    @Test
    void tick_continuesActiveRecipe() throws IOException {
        try (World world = mock(World.class)) {
            // Given mock parameters
            BlockPos mockPos = mock(BlockPos.class);
            BlockState mockState = mock(BlockState.class);

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
            entity.setCookTime(100);
            entity.setCookTimeTotal(250);
            entity.setBurnTime(1501);

            // When the game ticks
            entity.tick(world, mockPos, mockState);

            // Then the world updates
            verify(world).markDirty(mockPos);
            assertEquals(1500, entity.getBurnTime());
            assertEquals(101, entity.getCookTime());
            assertTrue(entity.isBurning());
        }
    }

    @Test
    void tick_cannotContinueActiveRecipe() throws IOException {
        try (World world = mock(World.class)) {
            // Given mock parameters
            BlockPos mockPos = mock(BlockPos.class);
            BlockState mockState = mock(BlockState.class);

            // Given a SmeltingInventory that doesn't match the recipe
            SmeltingInventory inventory = SmeltingInventory.ofSize(4);
            inventory.setStack(0, new ItemStack(Items.COPPER_INGOT, 2));
            inventory.setStack(1, new ItemStack(Items.GOLD_INGOT, 1));
            inventory.setStack(2, new ItemStack(Items.COAL, 1));

            // Given the BlockEntity
            DualSmelterBlockEntity entity = new DualSmelterBlockEntity(mockPos, mockState);
            entity.setInventory(inventory);
            entity.setCurrentRecipe(new RecipeEntry<>(RECIPE_ID, RECIPE));
            // Value not important, just needs to be greater than 1 so the recipe doesn't finish on this tick
            entity.setCookTime(100);
            entity.setCookTimeTotal(250);
            entity.setBurnTime(1300);

            // When the game ticks
            entity.tick(world, mockPos, mockState);

            // Then the world updates
            verify(world).markDirty(mockPos);
            assertEquals(1299, entity.getBurnTime());
            assertEquals(0, entity.getCookTime(), "Cook time should be reset to zero");
            assertEquals(250, entity.getCookTimeTotal(), "Cook time total should be unchanged");
            assertTrue(entity.isBurning());
        }
    }

    @Test
    void tick_activeRecipeIsOutOfFuel() throws IOException {
        try (World world = mock(World.class)) {
            // Given mock parameters
            BlockPos mockPos = mock(BlockPos.class);
            BlockState mockState = mock(BlockState.class);

            // Given a SmeltingInventory that matches the recipe, but has no fuel left
            SmeltingInventory inventory = SmeltingInventory.ofSize(4);
            inventory.setStack(0, new ItemStack(Items.COPPER_INGOT, 3));
            inventory.setStack(1, new ItemStack(Items.GOLD_INGOT, 1));
            inventory.setStack(2, new ItemStack(Items.COAL, 0));

            // Given the BlockEntity
            DualSmelterBlockEntity entity = new DualSmelterBlockEntity(mockPos, mockState);
            entity.setInventory(inventory);
            entity.setCurrentRecipe(new RecipeEntry<>(RECIPE_ID, RECIPE));
            // Value not important, just needs to be greater than 1 so the recipe doesn't finish on this tick
            entity.setCookTime(100);
            entity.setCookTimeTotal(250);
            entity.setBurnTime(0);

            // When the game ticks
            entity.tick(world, mockPos, mockState);

            // Then the world updates
            verify(world).markDirty(mockPos);
            assertEquals(0, entity.getBurnTime(), "No fuel to reset burn time");
            assertEquals(99, entity.getCookTime(), "Cook time should be reset to zero");
            assertEquals(250, entity.getCookTimeTotal(), "Cook time total should be unchanged");
            assertFalse(entity.isBurning(), "No fuel to burn");
        }
    }

    @Test
    void tick_cannotOutputFinishedRecipe() throws IOException {
        try (ServerWorld world = mock(ServerWorld.class)) {
            // Given mock parameters
            BlockPos mockPos = mock(BlockPos.class);
            BlockState mockState = mock(BlockState.class);

            // Given a SmeltingInventory
            SmeltingInventory inventory = SmeltingInventory.ofSize(4);
            inventory.setStack(0, new ItemStack(Items.COPPER_INGOT, 3));
            inventory.setStack(1, new ItemStack(Items.GOLD_INGOT, 1));
            inventory.setStack(2, new ItemStack(Items.COAL, 1));
            inventory.setStack(3, new ItemStack(Items.IRON_INGOT, 4));

            // Given the BlockEntity
            DualSmelterBlockEntity entity = new DualSmelterBlockEntity(mockPos, mockState);
            entity.setInventory(inventory);
            entity.setCurrentRecipe(new RecipeEntry<>(RECIPE_ID, RECIPE));
            entity.setBurnTime(738);
            entity.setCookTime(250);
            entity.setCookTimeTotal(250);

            // Given a PlayerEntity (spawning experience orbs requires a ServerPlayerEntity)
            ServerPlayerEntity mockServerPlayerEntity = mock(ServerPlayerEntity.class);
            when(mockServerPlayerEntity.getServerWorld()).thenReturn(world);
            when(mockServerPlayerEntity.getPos()).thenReturn(Vec3d.ZERO);
            entity.setPlayer(mockServerPlayerEntity);

            // When the game ticks
            entity.tick(world, mockPos, mockState);

            // Then the world updates
            verify(world).markDirty(mockPos);
            assertEquals(737, entity.getBurnTime());
            assertEquals(250, entity.getCookTime(), "Cook time should not be reset to zero");
            assertEquals(new RecipeEntry<>(RECIPE_ID, RECIPE), entity.getCurrentRecipe());
            assertNull(entity.getLastRecipe());

            // Then the inventory updates
            assertEquals(3, inventory.getStack(0).getCount(), "Inventory unchanged");
            assertEquals(1, inventory.getStack(1).getCount(), "Inventory unchanged");
            assertEquals(4, inventory.getStack(3).getCount(), "Stack cannot combine.");
        }
    }

    @Test
    void tick_finishesRecipe() throws IOException {
        try (ServerWorld world = mock(ServerWorld.class);
             MockedStatic<ExperienceOrbEntity> staticExperienceOrbEntity = mockStatic(ExperienceOrbEntity.class)) {
            // Given mock parameters
            BlockPos mockPos = mock(BlockPos.class);
            BlockState mockState = mock(BlockState.class);

            // Given a SmeltingInventory
            SmeltingInventory inventory = SmeltingInventory.ofSize(4);
            inventory.setStack(0, new ItemStack(Items.COPPER_INGOT, 3));
            inventory.setStack(1, new ItemStack(Items.GOLD_INGOT, 1));
            inventory.setStack(2, new ItemStack(Items.COAL, 1));

            // Given the BlockEntity
            DualSmelterBlockEntity entity = new DualSmelterBlockEntity(mockPos, mockState);
            entity.setInventory(inventory);
            entity.setCurrentRecipe(new RecipeEntry<>(RECIPE_ID, RECIPE));
            entity.setBurnTime(1600);
            entity.setCookTime(250);
            entity.setCookTimeTotal(250);

            // Given a PlayerEntity (spawning experience orbs requires a ServerPlayerEntity)
            ServerPlayerEntity mockServerPlayerEntity = mock(ServerPlayerEntity.class);
            when(mockServerPlayerEntity.getServerWorld()).thenReturn(world);
            when(mockServerPlayerEntity.getPos()).thenReturn(Vec3d.ZERO);
            entity.setPlayer(mockServerPlayerEntity);

            // When the game ticks
            entity.tick(world, mockPos, mockState);

            // Then the world updates
            verify(world).markDirty(mockPos);
            assertEquals(1599, entity.getBurnTime());
            assertEquals(0, entity.getCookTime());
            assertNull(entity.getCurrentRecipe());
            // The last recipe is set for XP purposes, and then cleared
            assertNull(entity.getLastRecipe());

            // Then the inventory updates
            assertEquals(0, inventory.getStack(0).getCount());
            assertEquals(0, inventory.getStack(1).getCount());
            assertEquals(4, inventory.getStack(3).getCount());

            // Then the orb is spawned
            staticExperienceOrbEntity.verify(() -> ExperienceOrbEntity.spawn(eq(world), eq(Vec3d.ZERO), anyInt()));
        }
    }
}