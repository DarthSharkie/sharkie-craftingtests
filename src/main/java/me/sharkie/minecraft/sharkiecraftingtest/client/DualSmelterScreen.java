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

public class DualSmelterScreen extends HandledScreen<DualSmelterScreenHandler> {
    private static final Identifier TEXTURE = new Identifier("sharkie-craftingtest", "textures/gui/container/dual_smelter.png");

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
        context.drawTexture(TEXTURE, x, y, 0, 0, backgroundWidth, backgroundHeight);
    }

    @Override
    protected void init() {
        super.init();
        // Center the title
        titleX = (backgroundWidth - textRenderer.getWidth(title)) / 2;
    }
}