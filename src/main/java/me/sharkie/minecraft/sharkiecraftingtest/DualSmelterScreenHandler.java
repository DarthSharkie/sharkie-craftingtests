package me.sharkie.minecraft.sharkiecraftingtest;

import net.minecraft.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.resource.featuretoggle.FeatureFlags;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.Identifier;

import java.util.Map;

public class DualSmelterScreenHandler extends ScreenHandler {

    public static ScreenHandlerType<DualSmelterScreenHandler> SCREEN_HANDLER_TYPE;
    private static final TagKey<Item> INGOTS_TAGKEY = TagKey.of(RegistryKeys.ITEM, new Identifier("c", "ingots"));
    private static final Map<Item, Integer> FUEL_BURN_TIME_MAP = AbstractFurnaceBlockEntity.createFuelTimeMap();

    public static void register(String modid) {
        SCREEN_HANDLER_TYPE = Registry.register(Registries.SCREEN_HANDLER,
                                                new Identifier(modid, "dual_smelter_screen_handler"),
                                                new ScreenHandlerType<>(DualSmelterScreenHandler::new, FeatureFlags.VANILLA_FEATURES));
    }

    private final Inventory inventory;

    public DualSmelterScreenHandler(int syncId, PlayerInventory playerInventory) {
        this(syncId, playerInventory, SmeltingInventory.ofSize(4));
    }

    public DualSmelterScreenHandler(int syncId, PlayerInventory playerInventory, SmeltingInventory inventory) {
        super(SCREEN_HANDLER_TYPE, syncId);
        checkSize(inventory, 4);
        this.inventory = inventory;
        // Allow logic on opening inventory
        inventory.onOpen(playerInventory.player);

        // Compute Slots shown on this screen, including crafting slots, player inventory, and player hotbar
        // Crafting
        this.addSlot(new Slot(inventory, 0, 36, 17));   // 16x16 input 1
        this.addSlot(new Slot(inventory, 1, 56, 17));   // 16x16 input 2
        this.addSlot(new Slot(inventory, 2, 46, 53));   // 16x16 fuel slot
        this.addSlot(new Slot(inventory, 3, 116, 35));  // 24x24 output slot, offset by (4,4) to center it

        //The player inventory
        for (int m = 0; m < 3; m++) {
            for (int l = 0; l < 9; l++) {
                // Player inventory has hotbar first, then the "bag"
                this.addSlot(new Slot(playerInventory, l + m * 9 + 9, 8 + l * 18, 84 + m * 18));
            }
        }
        //The player Hotbar
        for (int i = 0; i < 9; i++) {
            this.addSlot(new Slot(playerInventory, i, 8 + i * 18, 142));
        }
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int slotId) {
        // This handles Shift + Player Inventory (inserting into this screen's inventory)
        Slot slot = this.slots.get(slotId);
        if (!slot.hasStack()) {
            return ItemStack.EMPTY;
        }

        ItemStack stack = slot.getStack();
        ItemStack originalStack = stack.copy();
        if (slotId == 3) {
            // Source slot is this UI's output, try to return to player hotbar, then player inventory
            if (!this.insertItem(stack, this.inventory.size(), this.slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (slotId < this.inventory.size()) {
            // Source slot is specific to this UI, try to return to player inventory, then player hotbar
            if (!this.insertItem(stack, this.inventory.size(), this.slots.size(), false)) {
                return ItemStack.EMPTY;
            }
        } else if (stack.isIn(INGOTS_TAGKEY) && slotId > this.inventory.size()) {
            // Source stack is meltable, and source slot is player inventory/hotbar; try to move into melting slots
            if (!this.insertItem(stack, 0, 2, false)) {
                return ItemStack.EMPTY;
            }
        } else if (this.isFuel(stack) && slotId > this.inventory.size()) {
            // Source stack is fuel, and source slot is player inventory/hotbar; try to move into fuel slot
            if (!this.insertItem(stack, 2, 3, false)) {
                return ItemStack.EMPTY;
            }
        }

        if (stack.isEmpty()) {
            slot.setStack(ItemStack.EMPTY);
        } else {
            slot.markDirty();
        }
        if (originalStack.getCount() == stack.getCount()) {
            return ItemStack.EMPTY;
        }
        return originalStack;
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return this.inventory.canPlayerUse(player);
    }

    private boolean isFuel(ItemStack itemStack) {
        return FUEL_BURN_TIME_MAP.containsKey(itemStack.getItem());
    }
}
