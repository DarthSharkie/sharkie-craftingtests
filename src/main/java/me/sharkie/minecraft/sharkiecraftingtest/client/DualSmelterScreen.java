package me.sharkie.minecraft.sharkiecraftingtest.client;

import com.mojang.blaze3d.systems.RenderSystem;
import me.sharkie.minecraft.sharkiecraftingtest.DualSmelterScreenHandler;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreens;
import net.minecraft.client.gui.screen.recipebook.RecipeBookProvider;
import net.minecraft.client.gui.screen.recipebook.RecipeBookWidget;
import net.minecraft.client.gui.widget.TexturedButtonWidget;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;

public class DualSmelterScreen extends HandledScreen<DualSmelterScreenHandler> implements RecipeBookProvider {
    private static final Identifier TEXTURE = new Identifier("sharkie-craftingtest", "textures/gui/container/dual_smelter.png");
    private static final Identifier LIT_PROGRESS_TEXTURE = new Identifier("container/furnace/lit_progress");
    private static final Identifier BURN_PROGRESS_TEXTURE = new Identifier("container/furnace/burn_progress");

    private final RecipeBookWidget recipeBook;
    private boolean narrow;

    public DualSmelterScreen(DualSmelterScreenHandler handler, PlayerInventory playerInventory, Text title) {
        super(handler, playerInventory, title);
        // Built-in screens pass in a new instance, so mimic that by creating an instance here.
        this.recipeBook = new DualSmelterRecipeBookScreen();
    }

    public static void register() {
        HandledScreens.register(DualSmelterScreenHandler.SCREEN_HANDLER_TYPE, DualSmelterScreen::new);
    }

    @Override
    protected void init() {
        super.init();
        this.narrow = this.width < 379;
        this.recipeBook.initialize(this.width, this.height, this.client, this.narrow, this.handler);
        this.x = this.recipeBook.findLeftEdge(this.width, this.backgroundWidth);
        this.addDrawableChild(new TexturedButtonWidget(this.x + 20, this.height / 2 - 49, 20, 18, RecipeBookWidget.BUTTON_TEXTURES, button -> {
            this.recipeBook.toggleOpen();
            this.x = this.recipeBook.findLeftEdge(this.width, this.backgroundWidth);
            button.setPosition(this.x + 20, this.height / 2 - 49);
        }));
        // Center the title
        titleX = (backgroundWidth - textRenderer.getWidth(title)) / 2;
    }

    @Override
    protected void handledScreenTick() {
        super.handledScreenTick();
        this.recipeBook.update();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        if (this.recipeBook.isOpen() && this.narrow) {
            this.renderBackground(context, mouseX, mouseY, delta);
            this.recipeBook.render(context, mouseX, mouseY, delta);
        } else {
            super.render(context, mouseX, mouseY, delta);
            this.recipeBook.render(context, mouseX, mouseY, delta);
            this.recipeBook.drawGhostSlots(context, this.x, this.y, true, delta);
        }
        this.drawMouseoverTooltip(context, mouseX, mouseY);
        this.recipeBook.drawTooltip(context, this.x, this.y, mouseX, mouseY);
    }

    @Override
    protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexProgram);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, TEXTURE);
        int _x = this.x;
        int _y = this.y;
        // Recommendation is to call the version with the following signature:
        // texture, x, y, width, height, u, v, regionWidth, regionHeight, textureWidth, textureHeight
        context.drawTexture(TEXTURE, _x, _y, 0, 0, backgroundWidth, backgroundHeight);

        if (this.handler.isBurning()) {
            int progress = MathHelper.ceil(this.handler.getBurnProgress() * 13.0f) + 1;
            int litWidth = 14;
            int litHeight = 14;
            int visibleHeight = litHeight - progress;
            context.drawGuiTexture(LIT_PROGRESS_TEXTURE, litWidth, litHeight, 0, visibleHeight, _x + 46, _y + 36 + visibleHeight, litWidth, progress);
        }
        int cookProgressHeight = 16;
        int cookProgressWidth = 24;
        int cookProgress = MathHelper.ceil(this.handler.getCookProgress() * 24.0f);
        context.drawGuiTexture(BURN_PROGRESS_TEXTURE,
                               cookProgressWidth,
                               cookProgressHeight,
                               0,
                               0,
                               _x + 79,
                               _y + 34,
                               cookProgress,
                               cookProgressHeight);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.recipeBook.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        if (this.narrow && this.recipeBook.isOpen()) {
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    protected void onMouseClick(Slot slot, int slotId, int button, SlotActionType actionType) {
        super.onMouseClick(slot, slotId, button, actionType);
        this.recipeBook.slotClicked(slot);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.recipeBook.keyPressed(keyCode, scanCode, modifiers)) {
            return false;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    protected boolean isClickOutsideBounds(double mouseX, double mouseY, int left, int top, int button) {
        boolean bl = mouseX < (double) left || mouseY < (double) top || mouseX >= (double) (left + this.backgroundWidth) || mouseY >= (double) (top + this.backgroundHeight);
        return this.recipeBook.isClickOutsideBounds(mouseX, mouseY, this.x, this.y, this.backgroundWidth, this.backgroundHeight, button) && bl;
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (this.recipeBook.charTyped(chr, modifiers)) {
            return true;
        }
        return super.charTyped(chr, modifiers);
    }

    @Override
    public void refreshRecipeBook() {
        this.getRecipeBookWidget().refresh();
    }

    @Override
    public RecipeBookWidget getRecipeBookWidget() {
        return this.recipeBook;
    }
}
