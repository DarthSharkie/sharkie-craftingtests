package me.sharkie.minecraft.sharkiecraftingtest.mixin;

import me.sharkie.minecraft.sharkiecraftingtest.DualSmelterRecipe;
import net.minecraft.client.recipebook.ClientRecipeBook;
import net.minecraft.client.recipebook.RecipeBookGroup;
import net.minecraft.recipe.RecipeEntry;
import net.minecraft.recipe.book.RecipeBook;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Debug;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Debug(export = true)
@Mixin(ClientRecipeBook.class)
public abstract class DualSmelterClientRecipeBookMixin extends RecipeBook {
    @Shadow
    @Final
    private static Logger LOGGER;

    @Inject(method = "getGroupForRecipe", at = @At("HEAD"), cancellable = true)
    private static void onGetGroupForRecipe(RecipeEntry<?> recipe, CallbackInfoReturnable<RecipeBookGroup> cir) {
        if (recipe.value() instanceof DualSmelterRecipe dualSmelterRecipe) {
            LOGGER.info("Found a DSR: {}", recipe.id());
            cir.setReturnValue(RecipeBookGroup.FURNACE_MISC);
        }
    }
}
