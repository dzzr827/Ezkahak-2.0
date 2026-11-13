package dev.ezkahak.client;

import dev.ezkahak.client.config.ConfigManager;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.io.IOException;
import java.util.List;

/** Dedicated profile manager: create, duplicate, rename, load, save, delete and reset profiles. */
public final class ConfigManagerScreen extends Screen {
    private static final int BG = 0xF20A0D14, PANEL = 0xF2141B28, BLUE = 0xFF22B8FF;
    private static final int TEXT = 0xFFEAF5FF, MUTED = 0xFF8CA3BA;
    private final Screen parent;
    private TextFieldWidget nameField;
    private String selectedProfile;
    private String status = "Choose a profile or enter a name";
    private List<String> profiles = List.of();
    private int listScroll;

    public ConfigManagerScreen(Screen parent) {
        super(Text.literal("EzkaHak Config Manager Pro"));
        this.parent = parent;
        this.selectedProfile = ConfigManager.activeProfile();
    }

    @Override protected void init() {
        int w = Math.min(620, width - 32);
        int x = (width - w) / 2;
        nameField = new TextFieldWidget(textRenderer, x + 24, 64, Math.min(250, w - 48), 20, Text.literal("Profile name"));
        nameField.setPlaceholder(Text.literal("profile_name"));
        nameField.setMaxLength(32);
        addDrawableChild(nameField);
        refreshProfiles();
    }

    private void refreshProfiles() {
        try { profiles = ConfigManager.profiles(); }
        catch (IOException e) { profiles = List.of(); status = "Could not read profiles"; }
        if (selectedProfile == null || !profiles.contains(selectedProfile)) selectedProfile = ConfigManager.activeProfile();
    }

    @Override public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        renderBackground(ctx, mouseX, mouseY, delta);
        int w = Math.min(620, width - 32), h = Math.min(400, height - 32);
        int x = (width - w) / 2, y = (height - h) / 2;
        ctx.fill(0, 0, width, height, 0xB8000308);
        ctx.fill(x, y, x + w, y + h, BG);
        ctx.fill(x, y, x + w, y + 3, BLUE);
        ctx.fill(x, y + 3, x + w, y + 44, PANEL);
        ctx.drawText(textRenderer, Text.literal("CONFIG MANAGER PRO"), x + 18, y + 15, BLUE, true);
        ctx.drawText(textRenderer, Text.literal("Active: " + ConfigManager.activeProfile()), x + w - 180, y + 15, MUTED, false);
        ctx.drawText(textRenderer, Text.literal("PROFILE NAME"), x + 24, y + 52, MUTED, false);
        ctx.drawText(textRenderer, Text.literal("PROFILES"), x + 24, y + 96, MUTED, false);

        int listX = x + 24, listY = y + 114, listW = Math.min(270, w - 48), listH = h - 174;
        ctx.fill(listX, listY, listX + listW, listY + listH, 0xFF0D1420);
        ctx.enableScissor(listX, listY, listX + listW, listY + listH);
        int rowY = listY + 6 - listScroll;
        for (String profile : profiles) {
            boolean active = profile.equals(ConfigManager.activeProfile());
            boolean selected = profile.equals(selectedProfile);
            if (selected) ctx.fill(listX + 4, rowY, listX + listW - 4, rowY + 24, 0xFF18334A);
            if (active) ctx.fill(listX + 4, rowY, listX + 7, rowY + 24, BLUE);
            ctx.drawText(textRenderer, Text.literal(profile), listX + 14, rowY + 8, selected ? TEXT : MUTED, selected);
            if (active) ctx.drawText(textRenderer, Text.literal("ACTIVE"), listX + listW - 54, rowY + 8, BLUE, true);
            rowY += 29;
        }
        ctx.disableScissor();

        int rightX = x + Math.min(320, w / 2 + 8);
        int rightW = x + w - 20 - rightX;
        ctx.drawText(textRenderer, Text.literal("PROFILE ACTIONS"), rightX, y + 96, MUTED, false);
        drawButton(ctx, rightX, y + 116, rightW, 22, "LOAD SELECTED", 0xFF164A6B);
        drawButton(ctx, rightX, y + 143, rightW, 22, "SAVE CURRENT", 0xFF164A6B);
        drawButton(ctx, rightX, y + 170, rightW, 22, "CREATE FROM CURRENT", 0xFF164A6B);
        drawButton(ctx, rightX, y + 197, rightW, 22, "DUPLICATE SELECTED", 0xFF164A6B);
        drawButton(ctx, rightX, y + 224, rightW, 22, "RENAME SELECTED", 0xFF164A6B);
        drawButton(ctx, rightX, y + 251, rightW, 22, "DELETE SELECTED", 0xFF4A2632);
        drawButton(ctx, rightX, y + 278, rightW, 22, "RESET CURRENT", 0xFF4A2632);

        int footerY = y + h - 28;
        drawButton(ctx, x + 20, footerY, 92, 18, "BACK", 0xFF26384A);
        String shownStatus = status;
        while (!shownStatus.isEmpty() && textRenderer.getWidth(shownStatus) > w - 132) shownStatus = shownStatus.substring(0, shownStatus.length() - 1);
        ctx.drawText(textRenderer, Text.literal(shownStatus), x + 124, footerY + 5, MUTED, false);
        super.render(ctx, mouseX, mouseY, delta);
    }

    private void drawButton(DrawContext ctx, int x, int y, int w, int h, String label, int color) {
        ctx.fill(x, y, x + w, y + h, color);
        ctx.drawCenteredTextWithShadow(textRenderer, Text.literal(label), x + w / 2, y + 7, TEXT);
    }

    private String enteredName() { return nameField == null ? "" : nameField.getText().trim(); }

    private void runAction(String action) {
        try {
            switch (action) {
                case "load" -> { if (selectedProfile == null) throw new IOException("Select a profile first"); ConfigManager.useProfile(selectedProfile, true); status = "Loaded " + selectedProfile; }
                case "save" -> { ConfigManager.save(); status = "Saved " + ConfigManager.activeProfile(); }
                case "create" -> { ConfigManager.createProfile(enteredName()); selectedProfile = ConfigManager.activeProfile(); status = "Created " + selectedProfile; }
                case "duplicate" -> { if (selectedProfile == null) throw new IOException("Select a profile first"); ConfigManager.duplicateProfile(selectedProfile, enteredName()); selectedProfile = ConfigManager.activeProfile(); status = "Duplicated to " + selectedProfile; }
                case "rename" -> { if (selectedProfile == null) throw new IOException("Select a profile first"); String old = selectedProfile; ConfigManager.renameProfile(old, enteredName()); selectedProfile = old.equals(ConfigManager.activeProfile()) ? ConfigManager.activeProfile() : enteredName(); status = "Renamed profile"; }
                case "delete" -> { if (selectedProfile == null) throw new IOException("Select a profile first"); String old = selectedProfile; ConfigManager.deleteProfile(old); selectedProfile = ConfigManager.activeProfile(); status = "Deleted " + old; }
                case "reset" -> { ConfigManager.resetCurrentProfile(); selectedProfile = ConfigManager.activeProfile(); status = "Current profile reset"; }
            }
            refreshProfiles();
        } catch (IOException e) { status = e.getMessage() == null ? "Profile operation failed" : e.getMessage(); }
    }

    @Override public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != GLFW.GLFW_MOUSE_BUTTON_LEFT) return super.mouseClicked(mouseX, mouseY, button);
        int w = Math.min(620, width - 32), h = Math.min(400, height - 32);
        int x = (width - w) / 2, y = (height - h) / 2;
        int listX = x + 24, listY = y + 114, listW = Math.min(270, w - 48), listH = h - 174;
        if (mouseX >= listX && mouseX <= listX + listW && mouseY >= listY && mouseY <= listY + listH) {
            int index = (int)((mouseY - (listY + 6) + listScroll) / 29);
            if (index >= 0 && index < profiles.size()) selectedProfile = profiles.get(index);
            return true;
        }
        int rightX = x + Math.min(320, w / 2 + 8), rightW = x + w - 20 - rightX;
        String[] actions = {"load", "save", "create", "duplicate", "rename", "delete", "reset"};
        for (int i = 0; i < actions.length; i++) {
            int by = y + 116 + i * 27;
            if (mouseX >= rightX && mouseX <= rightX + rightW && mouseY >= by && mouseY <= by + 22) {
                runAction(actions[i]);
                return true;
            }
        }
        int footerY = y + h - 28;
        if (mouseX >= x + 20 && mouseX <= x + 112 && mouseY >= footerY && mouseY <= footerY + 18) {
            if (client != null) client.setScreen(parent);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        int h = Math.min(400, height - 32), y = (height - h) / 2;
        int listY = y + 114, listH = h - 174;
        if (mouseY >= listY && mouseY <= listY + listH) {
            int max = Math.max(0, profiles.size() * 29 - listH + 12);
            listScroll = Math.max(0, Math.min(max, listScroll - (int)(verticalAmount * 29)));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) { if (client != null) client.setScreen(parent); return true; }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override public void close() { if (client != null) client.setScreen(parent); }
    @Override public boolean shouldPause() { return false; }
}
