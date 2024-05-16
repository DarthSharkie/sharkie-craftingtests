package me.sharkie.minecraft.sharkiecraftingtest.client;

import me.sharkie.minecraft.sharkiecraftingtest.DualSmelterBlockEntity;
import me.sharkie.minecraft.sharkiecraftingtest.DualSmelterRecipe;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.screen.ButtonTextures;
import net.minecraft.client.gui.screen.recipebook.RecipeBookWidget;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.RecipeEntry;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Set;

@Environment(EnvType.CLIENT)
public class DualSmelterRecipeBookScreen extends RecipeBookWidget {
    private static final ButtonTextures TEXTURES = new ButtonTextures(new Identifier("recipe_book/furnace_filter_enabled"),
                                                                      new Identifier("recipe_book/furnace_filter_disabled"),
                                                                      new Identifier("recipe_book/furnace_filter_enabled_highlighted"),
                                                                      new Identifier("recipe_book/furnace_filter_disabled_highlighted"));
    private static final Text TOGGLE_ALLOY_RECIPES_TEXT = Text.translatable("gui.recipebook.toggleRecipes.dualSmelter");

    @Nullable
    private Ingredient fuels;

    @Override
    protected void setBookButtonTexture() {
        this.toggleCraftableButton.setTextures(TEXTURES);
    }

    @Override
    protected Text getToggleCraftableButtonText() {
        return TOGGLE_ALLOY_RECIPES_TEXT;
    }

    @Override
    public void slotClicked(@Nullable Slot slot) {
        super.slotClicked(slot);
        if (slot != null && slot.id < this.craftingScreenHandler.getCraftingSlotCount()) {
            this.ghostSlots.reset();
        }
    }

    @Override
    public void showGhostRecipe(RecipeEntry<?> recipe, List<Slot> slots) {
        if (!(recipe.value() instanceof DualSmelterRecipe dualSmelterRecipe)) {
            return;
        }
        assert this.client.world != null;
        ItemStack result = dualSmelterRecipe.getResult(this.client.world.getRegistryManager());
        this.ghostSlots.setRecipe(recipe);

        // Show result
        this.ghostSlots.addSlot(Ingredient.ofStacks(result), slots.get(3).x, slots.get(3).y);
        // Show fuel, if needed
        Slot fuelSlot = slots.get(2);
        if (fuelSlot.getStack().isEmpty()) {
            if (this.fuels == null) {
                this.fuels = Ingredient.ofStacks(this.getAllowedFuels()
                                                     .stream()
                                                     .filter(item -> item.isEnabled(this.client.world.getEnabledFeatures()))
                                                     .map(ItemStack::new));
            }
            this.ghostSlots.addSlot(this.fuels, fuelSlot.x, fuelSlot.y);
        }

        // Show Ingredients
        this.ghostSlots.addSlot(dualSmelterRecipe.getInputA(), slots.get(0).x, slots.get(0).y);
        this.ghostSlots.addSlot(dualSmelterRecipe.getInputB(), slots.get(1).x, slots.get(1).y);
    }

    private Set<Item> getAllowedFuels() {
        return DualSmelterBlockEntity.FUEL_BURN_TIME_MAP.keySet();
    }
}
