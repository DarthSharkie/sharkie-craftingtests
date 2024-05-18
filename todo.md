## TODO List:

1. Multi-count inputs (see above)
    - Ingredient codec does not allow for stack counts.
    - ItemStack (for results) does allow for stack counts.
    - Therefore, will need to modify DualSmelterRecipe to use ItemStack inputs.
        - This should be acceptable because these recipes don't need tag capability in inputs.
2. Drop XP orbs for crafting
3. Figure out recipe unlocking
4. New RecipeBookGroup and tab? More mixins this way...