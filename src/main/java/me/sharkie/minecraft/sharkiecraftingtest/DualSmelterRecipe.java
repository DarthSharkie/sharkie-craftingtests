package me.sharkie.minecraft.sharkiecraftingtest;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.Recipe;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.recipe.RecipeType;
import net.minecraft.recipe.book.CookingRecipeCategory;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.dynamic.Codecs;
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

    public static void register(String modid) {
        Registry.register(Registries.RECIPE_TYPE, new Identifier(modid, Type.ID), Type.INSTANCE);
        Registry.register(Registries.RECIPE_SERIALIZER, new Identifier(modid, Serializer.ID), Serializer.INSTANCE);
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
        return width == 2 && height == 1;
    }

    @Override
    public ItemStack getResult(DynamicRegistryManager registryManager) {
        return this.getOutput();
    }

    @Override
    public DefaultedList<Ingredient> getIngredients() {
        DefaultedList<Ingredient> defaultedList = DefaultedList.of();
        defaultedList.add(this.inputA);
        defaultedList.add(this.inputB);
        return defaultedList;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return Serializer.INSTANCE;
    }

    @Override
    public RecipeType<?> getType() {
        return Type.INSTANCE;
    }

    public static class Type implements RecipeType<DualSmelterRecipe> {
        private Type() {
        }

        public static final Type INSTANCE = new Type();
        public static final String ID = "dual_smelter";
    }

    public static class Serializer implements RecipeSerializer<DualSmelterRecipe> {

        private Serializer() {
        }

        public static final Serializer INSTANCE = new Serializer();
        public static final String ID = "dual_smelter";

        private static final Codec<DualSmelterRecipe> CODEC = createCodec();

        private static Codec<DualSmelterRecipe> createCodec() {
            MapCodec<Ingredient> ingredientACodec = Ingredient.DISALLOW_EMPTY_CODEC.fieldOf("ingredientA");
            MapCodec<Ingredient> ingredientBCodec = Ingredient.DISALLOW_EMPTY_CODEC.fieldOf("ingredientB");
            MapCodec<ItemStack> outputCodec = ItemStack.RECIPE_RESULT_CODEC.fieldOf("result");
            MapCodec<String> groupCodec = Codecs.createStrictOptionalFieldCodec(Codec.STRING, "group", CookingRecipeCategory.MISC.asString());
            MapCodec<Integer> cookingTimeCodec = Codec.INT.fieldOf("cookingtime");
            return RecordCodecBuilder.create(instance -> instance.group(ingredientACodec.forGetter(DualSmelterRecipe::getInputA),
                                                                        ingredientBCodec.forGetter(DualSmelterRecipe::getInputB),
                                                                        outputCodec.forGetter(DualSmelterRecipe::getOutput),
                                                                        groupCodec.forGetter(DualSmelterRecipe::getGroup),
                                                                        cookingTimeCodec.forGetter(DualSmelterRecipe::getCookingTime))
                                                                 .apply(instance, DualSmelterRecipe::new));
        }

        @Override
        public Codec<DualSmelterRecipe> codec() {
            return CODEC;
        }

        @Override
        public DualSmelterRecipe read(PacketByteBuf buf) {
            Ingredient ingredientA = Ingredient.fromPacket(buf);
            Ingredient ingredientB = Ingredient.fromPacket(buf);
            ItemStack output = buf.readItemStack();
            String group = buf.readString();
            int cookingTime = buf.readInt();
            return new DualSmelterRecipe(ingredientA, ingredientB, output, group, cookingTime);
        }

        @Override
        public void write(PacketByteBuf buf, DualSmelterRecipe recipe) {
            recipe.getInputA().write(buf);
            recipe.getInputB().write(buf);
            buf.writeItemStack(recipe.getOutput());
            buf.writeString(recipe.getGroup());
            buf.writeInt(recipe.getCookingTime());

        }
    }
}
