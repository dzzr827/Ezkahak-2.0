package dev.ezkahak.client;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

public final class HudEditorScreen extends Screen {
    private boolean dragging;
    private int dragX, dragY;
    public HudEditorScreen() { super(Text.literal("EzkaHak HUD Editor")); }
    @Override public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        renderBackground(ctx, mouseX, mouseY, delta);
        ctx.fill(0, 0, width, height, 0x66030810);
        int bx = HudLayout.x, by = HudLayout.y;
        ctx.fill(bx - 5, by - 5, bx + 180, by + 60, 0xDD0A101A);
        ctx.fill(bx - 5, by - 5, bx - 2, by + 60, 0xFF22B8FF);
        ctx.drawText(textRenderer, Text.literal("EZKAHAK 2.0  •  HUD PREVIEW"), bx, by, 0xFF22B8FF, true);
        ctx.drawText(textRenderer, Text.literal("FPS: 240   PING: 42 ms"), bx, by + 14, 0xFFEAF5FF, false);
        ctx.drawText(textRenderer, Text.literal("XYZ: 120 64 -350"), bx, by + 27, 0xFFEAF5FF, false);
        ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("Drag the preview to move HUD • Esc to close"), width / 2, height - 30, 0xFFEAF5FF);
        super.render(ctx, mouseX, mouseY, delta);
    }
    @Override public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && mouseX >= HudLayout.x - 8 && mouseX <= HudLayout.x + 190 && mouseY >= HudLayout.y - 8 && mouseY <= HudLayout.y + 66) {
            dragging = true; dragX = (int) mouseX - HudLayout.x; dragY = (int) mouseY - HudLayout.y; return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
    @Override public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (dragging && button == 0) { HudLayout.x = Math.max(4, Math.min(width - 190, (int) mouseX - dragX)); HudLayout.y = Math.max(4, Math.min(height - 70, (int) mouseY - dragY)); return true; }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }
    @Override public boolean mouseReleased(double mouseX, double mouseY, int button) { dragging = false; return super.mouseReleased(mouseX, mouseY, button); }
    @Override public void close() { try { dev.ezkahak.client.config.ConfigManager.save(); } catch (java.io.IOException ignored) { } super.close(); }
    @Override public boolean shouldPause() { return false; }
}
