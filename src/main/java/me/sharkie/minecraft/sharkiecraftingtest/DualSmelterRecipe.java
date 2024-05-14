package me.sharkie.minecraft.sharkiecraftingtest;

import net.minecraft.item.ItemStack;
import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.Recipe;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.recipe.RecipeType;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;

/**
 * Represents a {@link Recipe} crafted in a {@link me.sharkie.minecraft.sharkiecraftingtest.client.DualSmelterScreen}.
 * <p>
 * Recipe structure is as follows:
 * <pre>{@code
 * {
 *   "type": "sharkie-craftingtest:dual_smelter",
 *   "group": [group],
 *   "ingredientA": {
 *     "item": "<mod>:<item>"
 *   },
 *   "ingredientB": {
 *     "item": "<mod>:<item>"
 *   },
 *   "result": "<mod>:<item>",
 *   "cookingtime": [cooking time]
 * }
 * }</pre>
 */
public class DualSmelterRecipe implements Recipe<SmeltingInventory> {

    public static class Type implements RecipeType<DualSmelterRecipe> {
        private Type() {
        }

        public static final Type INSTANCE = new Type();
        public static final String ID = "dual_smelter";
    }

    public static void register(String modid) {
        Registry.register(Registries.RECIPE_TYPE, new Identifier(modid, Type.ID), Type.INSTANCE);
    }

    private final Identifier id;
    private final Ingredient inputA;
    private final Ingredient inputB;
    private final ItemStack output;
    private final String group;
    private final int cookingTime;

    public DualSmelterRecipe(Ingredient inputA, Ingredient inputB, ItemStack output, String group, int cookingTime) {
        this.id = new Identifier("sharkie-craftingtest", "rose_gold_recipe");
        this.inputA = inputA;
        this.inputB = inputB;
        this.output = output;
        this.group = group;
        this.cookingTime = cookingTime;
    }

    public Ingredient getInputA() {
        return inputA;
    }

    public Ingredient getInputB() {
        return inputB;
    }

    public ItemStack getOutput() {
        return output;
    }

    public Identifier getId() {
        return id;
    }

    public String getGroup() {
        return group;
    }

    public int getCookingTime() {
        return cookingTime;
    }

    @Override
    public boolean matches(SmeltingInventory inventory, World world) {
        if (!inventory.hasInputs()) {
            // Don't even look until both inputs are provided
            return false;
        }
        // Consider checking fuels depending on recipe, could rely on specific fuels for enough heat?
        return inputA.test(inventory.getStack(0)) && inputB.test(inventory.getStack(1));
    }

    @Override
    public ItemStack craft(SmeltingInventory inventory, DynamicRegistryManager registryManager) {
        return this.getOutput().copy();
    }

    @Override
    public boolean fits(int width, int height) {
        return true;
    }

    @Override
    public ItemStack getResult(DynamicRegistryManager registryManager) {
        return this.getOutput();
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return null;
    }

    @Override
    public RecipeType<?> getType() {
        return Type.INSTANCE;
    }
}
