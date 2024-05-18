package me.sharkie.minecraft.sharkiecraftingtest;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.item.Item;
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

import java.util.List;

/**
 * Represents a {@link Recipe} crafted in a {@link me.sharkie.minecraft.sharkiecraftingtest.client.DualSmelterScreen}.
 * <p>
 * Recipe structure is as follows:
 * <pre>{@code
 * {
 *   "type": "sharkie-craftingtest:dual_smelter",
 *   "group": "alloys",
 *   "inputs": [
 *     {
 *       "item": "<mod>:<item>",
 *       "count": <positive-int>?
 *     },
 *     {
 *       "item": "<mod>:<item>",
 *       "count": <positive-int>?
 *     }
 *   ],
 *   "result": {
 *     "item": "<mod>:<item>",
 *     "count": <positive-int>?
 *   },
 *   "smeltingTime": <positive-int>?,
 *   "experience": <positive-float>?
 * }
 * }</pre>
 */
public class DualSmelterRecipe implements Recipe<SmeltingInventory> {

    // Defined in game ticks
    private static final int DEFAULT_SMELTING_TIME = 250;
    private static final float DEFAULT_EXPERIENCE = 1.1F;

    public static void register(String modId) {
        Registry.register(Registries.RECIPE_TYPE, new Identifier(modId, Type.ID), Type.INSTANCE);
        Registry.register(Registries.RECIPE_SERIALIZER, new Identifier(modId, Serializer.ID), Serializer.INSTANCE);
    }

    private final List<ItemStack> inputs;
    private final ItemStack output;
    private final String group;
    private final int smeltingTime;
    private final float experience;

    public DualSmelterRecipe(List<ItemStack> inputs, ItemStack output, String group, int smeltingTime, float experience) {
        this.inputs = inputs;
        this.output = output;
        this.group = group;
        this.smeltingTime = smeltingTime;
        this.experience = experience;
    }

    DualSmelterRecipe(List<ItemStack> inputs, ItemStack output, String group, int smeltingTime) {
        this(inputs, output, group, smeltingTime, DEFAULT_EXPERIENCE);
    }

    DualSmelterRecipe(List<ItemStack> inputs, ItemStack output, String group) {
        this(inputs, output, group, DEFAULT_SMELTING_TIME, DEFAULT_EXPERIENCE);
    }

    public List<ItemStack> getInputs() {
        return this.inputs;
    }

    public ItemStack getInputA() {
        return this.inputs.get(0);
    }

    public ItemStack getInputB() {
        return this.inputs.get(1);
    }

    public ItemStack getOutput() {
        return output;
    }

    public String getGroup() {
        return group;
    }

    public int getSmeltingTime() {
        return smeltingTime;
    }

    public float getExperience() {
        return experience;
    }

    @Override
    public boolean matches(SmeltingInventory inventory, World world) {
        if (!inventory.hasInputs()) {
            // Don't even look until both inputs are provided
            return false;
        }
        // Consider checking fuels depending on recipe, could rely on specific fuels for enough heat?
        return this.getInputA().getCount() <= inventory.getStack(0).getCount()
               && ItemStack.areItemsEqual(this.getInputA(), inventory.getStack(0))
               && ItemStack.areItemsEqual(this.getInputB(), inventory.getStack(1));
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
        defaultedList.add(Ingredient.ofStacks(this.getInputA()));
        defaultedList.add(Ingredient.ofStacks(this.getInputB()));
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

        private static final Codec<ItemStack> ITEM_STACK_CODEC = createItemStackCodec();

        private static Codec<ItemStack> createItemStackCodec() {
            MapCodec<Item> item = Registries.ITEM.getCodec().fieldOf("item");
            MapCodec<Integer> count = Codecs.createStrictOptionalFieldCodec(Codecs.POSITIVE_INT, "count", 1);
            return RecordCodecBuilder.create(instance -> instance.group(item.forGetter(ItemStack::getItem), count.forGetter(ItemStack::getCount))
                                                                 .apply(instance, ItemStack::new));
        }

        private static final Codec<DualSmelterRecipe> CODEC = createCodec();

        private static Codec<DualSmelterRecipe> createCodec() {
            MapCodec<List<ItemStack>> inputsCodec = ITEM_STACK_CODEC.listOf().fieldOf("inputs");
            MapCodec<ItemStack> outputCodec = ItemStack.RECIPE_RESULT_CODEC.fieldOf("result");
            MapCodec<String> groupCodec = Codecs.createStrictOptionalFieldCodec(Codec.STRING, "group", CookingRecipeCategory.MISC.asString());
            MapCodec<Integer> smeltingTimeCodec = Codecs.createStrictOptionalFieldCodec(Codecs.POSITIVE_INT, "smeltingTime", DEFAULT_SMELTING_TIME);
            MapCodec<Float> experienceCodec = Codecs.createStrictOptionalFieldCodec(Codecs.POSITIVE_FLOAT, "experience", DEFAULT_EXPERIENCE);
            return RecordCodecBuilder.create(instance -> instance.group(inputsCodec.forGetter(DualSmelterRecipe::getInputs),
                                                                        outputCodec.forGetter(DualSmelterRecipe::getOutput),
                                                                        groupCodec.forGetter(DualSmelterRecipe::getGroup),
                                                                        smeltingTimeCodec.forGetter(DualSmelterRecipe::getSmeltingTime),
                                                                        experienceCodec.forGetter(DualSmelterRecipe::getExperience))
                                                                 .apply(instance, DualSmelterRecipe::new));
        }

        @Override
        public Codec<DualSmelterRecipe> codec() {
            return CODEC;
        }

        @Override
        public DualSmelterRecipe read(PacketByteBuf buf) {
            // No need to read into a list
            ItemStack inputA = buf.readItemStack();
            ItemStack inputB = buf.readItemStack();
            ItemStack output = buf.readItemStack();
            String group = buf.readString();
            int smeltingTime = buf.readInt();
            float experience = buf.readFloat();
            return new DualSmelterRecipe(List.of(inputA, inputB), output, group, smeltingTime, experience);
        }

        @Override
        public void write(PacketByteBuf buf, DualSmelterRecipe recipe) {
            // No need to iterate through the list
            buf.writeItemStack(recipe.getInputA());
            buf.writeItemStack(recipe.getInputB());
            buf.writeItemStack(recipe.getOutput());
            buf.writeString(recipe.getGroup());
            buf.writeInt(recipe.getSmeltingTime());
            buf.writeFloat(recipe.getExperience());
        }
    }
}
