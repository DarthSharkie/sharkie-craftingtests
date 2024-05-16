package me.sharkie.minecraft.sharkiecraftingtest.client;

import com.mojang.blaze3d.systems.RenderSystem;
import me.sharkie.minecraft.sharkiecraftingtest.DualSmelterScreenHandler;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreens;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;

public class DualSmelterScreen extends HandledScreen<DualSmelterScreenHandler> {
    private static final Identifier TEXTURE = new Identifier("sharkie-craftingtest", "textures/gui/container/dual_smelter.png");
    private static final Identifier LIT_PROGRESS_TEXTURE = new Identifier("container/furnace/lit_progress");
    private static final Identifier BURN_PROGRESS_TEXTURE = new Identifier("container/furnace/burn_progress");

    public DualSmelterScreen(DualSmelterScreenHandler handler, PlayerInventory playerInventory, Text title) {
        super(handler, playerInventory, title);
    }

    public static void register() {
        HandledScreens.register(DualSmelterScreenHandler.SCREEN_HANDLER_TYPE, DualSmelterScreen::new);
    }

    @Override
    protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexProgram);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, TEXTURE);
        int x = (width - backgroundWidth) / 2;
        int y = (height - backgroundHeight) / 2;
        // Recommendation is to call the version with the following signature:
        // texture, x, y, width, height, u, v, regionWidth, regionHeight, textureWidth, textureHeight
        context.drawTexture(TEXTURE, x, y, 0, 0, backgroundWidth, backgroundHeight);

        if (this.handler.isBurning()) {
            int progress = MathHelper.ceil(this.handler.getBurnProgress() * 13.0f) + 1;
            int litWidth = 14;
            int litHeight = 14;
            int visibleHeight = litHeight - progress;
            context.drawGuiTexture(LIT_PROGRESS_TEXTURE, litWidth, litHeight, 0, visibleHeight, x + 46, y + 36 + visibleHeight, litWidth, progress);
        }
        int cookProgressHeight = 16;
        int cookProgressWidth = 24;
        int cookProgress = MathHelper.ceil(this.handler.getCookProgress() * 24.0f);
        context.drawGuiTexture(BURN_PROGRESS_TEXTURE, cookProgressWidth, cookProgressHeight, 0, 0, x + 79, y + 34, cookProgress, cookProgressHeight);
    }

    @Override
    protected void init() {
        super.init();
        // Center the title
        titleX = (backgroundWidth - textRenderer.getWidth(title)) / 2;
    }
}
