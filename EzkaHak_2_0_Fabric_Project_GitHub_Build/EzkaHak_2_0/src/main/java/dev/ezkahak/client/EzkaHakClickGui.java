package dev.ezkahak.client;

import dev.ezkahak.client.config.ConfigManager;
import dev.ezkahak.client.ConfigManagerScreen;
import dev.ezkahak.client.module.ClientModule;
import dev.ezkahak.client.module.ModuleManager;
import dev.ezkahak.client.setting.BooleanSetting;
import dev.ezkahak.client.setting.ModeSetting;
import dev.ezkahak.client.setting.NumberSetting;
import dev.ezkahak.client.setting.Setting;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

public final class EzkaHakClickGui extends Screen {
    private static final int BG = 0xF20A0D14, PANEL = 0xF2141B28, PANEL_HI = 0xFF1C2B40;
    private static final int BLUE = 0xFF22B8FF, BLUE_DARK = 0xFF11618F, TEXT = 0xFFEAF5FF, MUTED = 0xFF8CA3BA;
    private static final String[] CATEGORIES = {"Combat", "Render", "Movement", "Player", "HUD", "Misc"};
    private String category = "Combat";
    private TextFieldWidget search;
    private ClientModule selected;
    private String status = "Ctrl+F search • right-click a module to bind";
    private int settingStartY;
    private int moduleScroll;
    private boolean awaitingKeybind;
    private int filterMode;
    private long openedAt = System.currentTimeMillis();

    public EzkaHakClickGui() {
        super(Text.literal("EzkaHak 2.0"));
        selected = ModuleManager.all().stream().filter(m -> m.getCategory().equals(category)).findFirst().orElse(null);
    }

    @Override protected void init() {
        int w = Math.min(760, width - 32);
        int x = (width - w) / 2;
        int searchWidth = Math.min(170, Math.max(105, width / 5));
        search = new TextFieldWidget(textRenderer, x + w - 350, (height - Math.min(430, height - 32)) / 2 + 14,
                searchWidth, 20, Text.literal("Search"));
        search.setPlaceholder(Text.literal("Search modules..."));
        search.setMaxLength(48);
        addDrawableChild(search);
    }

    @Override public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        renderBackground(ctx, mouseX, mouseY, delta);
        int w = Math.min(760, width - 32), h = Math.min(430, height - 32);
        int x = (width - w) / 2, y = (height - h) / 2;
        int elapsed = (int)Math.min(150, System.currentTimeMillis() - openedAt);
        int overlayAlpha = Math.max(0, Math.min(0x99, elapsed * 0x99 / 150));
        ctx.fill(0, 0, width, height, (overlayAlpha << 24) | 0x00030810);
        var visualEffects = ModuleManager.find("Visual Effects");
        boolean glow = visualEffects != null && visualEffects.isEnabled() && visualEffects.getSettings().stream().filter(setting -> setting instanceof BooleanSetting).map(setting -> (BooleanSetting) setting).filter(setting -> setting.getName().equals("Panel glow")).findFirst().map(BooleanSetting::get).orElse(true);
        if (glow) ctx.fill(x - 2, y - 2, x + w + 2, y + h + 2, 0xAA22B8FF);
        ctx.fill(x, y, x + w, y + h, BG);
        ctx.fill(x, y, x + w, y + 3, BLUE);
        ctx.fill(x, y + 3, x + w, y + 46, PANEL);
        ctx.fill(x, y + 46, x + 154, y + h, 0xF20D1420);
        ctx.fill(x + 154, y + 46, x + w - 190, y + h, 0xF20A101A);
        ctx.fill(x + w - 190, y + 46, x + w, y + h, 0xF2141B28);

        ctx.drawText(textRenderer, Text.literal("EZKAHAK"), x + 18, y + 13, BLUE, true);
        ctx.drawText(textRenderer, Text.literal("2.0  /  CYBER CLIENT"), x + 91, y + 14, MUTED, false);
        ctx.drawText(textRenderer, Text.literal("RIGHT SHIFT  •  MENU"), x + w - 164, y + 15, MUTED, false);

        int cy = y + 66;
        ctx.drawText(textRenderer, Text.literal("CATEGORIES"), x + 14, cy, MUTED, false);
        cy += 16;
        for (String cat : CATEGORIES) {
            boolean active = cat.equals(category);
            int bx = x + 8, by = cy;
            if (active) {
                ctx.fill(bx, by, bx + 138, by + 27, PANEL_HI);
                ctx.fill(bx, by, bx + 3, by + 27, BLUE);
            }
            ctx.drawText(textRenderer, Text.literal((active ? "◆  " : "◇  ") + cat), bx + 10, by + 9, active ? TEXT : MUTED, active);
            cy += 32;
        }

        int contentX = x + 170, contentY = y + 62;
        String filter = search == null ? "" : search.getText().toLowerCase(Locale.ROOT);
        List<ClientModule> visibleModules = filteredModules(filter);
        int cardW = w - 170 - 204;
        ctx.drawText(textRenderer, Text.literal((filter.isBlank() ? category : "SEARCH RESULTS").toUpperCase(Locale.ROOT)), contentX, contentY, TEXT, true);
        ctx.drawText(textRenderer, Text.literal(visibleModules.size() + " modules  •  click to select, switch to toggle"), contentX, contentY + 14, MUTED, false);
        ctx.drawText(textRenderer, Text.literal("FILTER: " + (filterMode == 0 ? "ALL" : filterMode == 1 ? "ENABLED" : "DISABLED")), contentX + cardW - 100, contentY + 14, BLUE, true);
        int listTop = contentY + 34;
        int listBottom = y + h - 40;
        int maxScroll = Math.max(0, visibleModules.size() * 51 - (listBottom - listTop));
        moduleScroll = Math.max(0, Math.min(moduleScroll, maxScroll));
        ctx.enableScissor(contentX, listTop, contentX + cardW, listBottom);
        int cardY = contentY + 36 - moduleScroll;
        for (ClientModule module : visibleModules) {
            boolean hover = mouseX >= contentX && mouseX <= contentX + cardW && mouseY >= cardY && mouseY <= cardY + 44;
            ctx.fill(contentX, cardY, contentX + cardW, cardY + 44,
                    module == selected ? 0xFF172A3C : hover ? 0xFF131F2E : PANEL);
            if (module == selected) ctx.fill(contentX, cardY, contentX + 2, cardY + 44, BLUE);
            ctx.drawText(textRenderer, Text.literal(module.getName()), contentX + 10, cardY + 7, TEXT, false);
            ctx.drawText(textRenderer, Text.literal(module.isEnabled() ? "ENABLED" : "DISABLED"),
                    contentX + 10, cardY + 23, module.isEnabled() ? BLUE : MUTED, false);
            String bindLabel = keyLabel(module.getKeyCode());
            ctx.drawText(textRenderer, Text.literal("KEY: " + bindLabel.toUpperCase(Locale.ROOT)), contentX + 10, cardY + 34, MUTED, false);
            int descriptionWidth = Math.max(70, cardW - 92);
            String shortDescription = module.getDescription();
            while (!shortDescription.isEmpty() && textRenderer.getWidth(shortDescription) > descriptionWidth)
                shortDescription = shortDescription.substring(0, shortDescription.length() - 1);
            if (!shortDescription.equals(module.getDescription()) && shortDescription.length() > 3)
                shortDescription = shortDescription.substring(0, shortDescription.length() - 3) + "...";
            int tx = contentX + cardW - 34, ty = cardY + 15;
            ctx.fill(tx, ty, tx + 24, ty + 13, module.isEnabled() ? BLUE_DARK : 0xFF303B49);
            ctx.fill(module.isEnabled() ? tx + 13 : tx + 2, ty + 2, module.isEnabled() ? tx + 22 : tx + 11, ty + 11,
                    module.isEnabled() ? BLUE : 0xFF9AA9B8);
            cardY += 51;
        }
        ctx.disableScissor();

        int rx = x + w - 178;
        ctx.drawText(textRenderer, Text.literal("MODULE SETTINGS"), rx, contentY, MUTED, false);
        ctx.fill(rx, contentY + 20, x + w - 12, contentY + 21, 0xFF26384A);
        settingStartY = contentY + 54;
        if (selected != null) {
            ctx.drawText(textRenderer, Text.literal(selected.getName()), rx, contentY + 32, TEXT, true);
            String bind = keyLabel(selected.getKeyCode());
            ctx.drawText(textRenderer, Text.literal((awaitingKeybind ? "PRESS A KEY..." : "BIND: " + bind.toUpperCase(Locale.ROOT))), rx, contentY + 43, awaitingKeybind ? BLUE : MUTED, true);
            drawSingleLine(ctx, selected.getDescription(), rx, contentY + 57, 158, MUTED);
            int sy = settingStartY + 38;
            for (Setting<?> setting : selected.getSettings()) {
                ctx.drawText(textRenderer, Text.literal(setting.getName()), rx, sy, MUTED, false);
                String value = settingValue(setting);
                ctx.drawText(textRenderer, Text.literal(value), rx, sy + 12, TEXT, true);
                if (setting instanceof NumberSetting) {
                    ctx.drawText(textRenderer, Text.literal("−                         +"), rx, sy + 24, BLUE, true);
                    sy += 44;
                } else {
                    sy += 31;
                }
                if (sy > y + h - 48) break;
            }
            if (selected.getSettings().isEmpty()) {
                ctx.drawText(textRenderer, Text.literal("No settings"), rx, settingStartY + 2, MUTED, false);
            }
        } else {
            ctx.drawText(textRenderer, Text.literal("Choose a module"), rx, contentY + 34, MUTED, false);
        }

        int by = y + h - 30;
        drawButton(ctx, x + 170, by, 74, 18, "SAVE", BLUE_DARK);
        drawButton(ctx, x + 250, by, 74, 18, "LOAD", BLUE_DARK);
        drawButton(ctx, x + 330, by, 78, 18, "CONFIGS", 0xFF164A6B);
        ctx.drawText(textRenderer, Text.literal(ConfigManager.activeProfile() + " • " + status), x + 416, by + 5, MUTED, false);
        super.render(ctx, mouseX, mouseY, delta);
    }

    private List<ClientModule> filteredModules(String filter) {
        return ModuleManager.all().stream()
                .filter(m -> filter.isBlank() ? m.getCategory().equals(category) : matchesQuery(m, filter))
                .filter(m -> filterMode == 0 || (filterMode == 1 && m.isEnabled()) || (filterMode == 2 && !m.isEnabled()))
                .toList();
    }

    /** Supports plain words and scoped searches such as cat:render, name:velocity, setting:range, key:rshift. */
    private boolean matchesQuery(ClientModule module, String rawQuery) {
        String[] terms = rawQuery.toLowerCase(Locale.ROOT).trim().split("\\s+");
        for (String term : terms) {
            if (term.isBlank()) continue;
            boolean match;
            if (term.startsWith("cat:") || term.startsWith("category:")) {
                String value = term.substring(term.indexOf(':') + 1);
                match = module.getCategory().toLowerCase(Locale.ROOT).contains(value);
            } else if (term.startsWith("name:")) {
                match = module.getName().toLowerCase(Locale.ROOT).contains(term.substring(5));
            } else if (term.startsWith("desc:")) {
                match = module.getDescription().toLowerCase(Locale.ROOT).contains(term.substring(5));
            } else if (term.startsWith("setting:") || term.startsWith("set:")) {
                String value = term.substring(term.indexOf(':') + 1);
                match = module.getSettings().stream().anyMatch(setting -> setting.getName().toLowerCase(Locale.ROOT).contains(value));
            } else if (term.startsWith("key:")) {
                String value = term.substring(4);
                String keyName = keyLabel(module.getKeyCode()).toLowerCase(Locale.ROOT).replace("_", "").replace("-", "");
                String normalizedValue = value.replace("_", "").replace("-", "");
                match = keyName.contains(normalizedValue) || (module.getKeyCode() < 0 && (value.equals("none") || value.equals("unbound")));
            } else {
                String value = term.startsWith("@") ? term.substring(1) : term;
                match = module.getName().toLowerCase(Locale.ROOT).contains(value)
                        || module.getCategory().toLowerCase(Locale.ROOT).contains(value)
                        || module.getDescription().toLowerCase(Locale.ROOT).contains(value)
                        || module.getSettings().stream().anyMatch(setting -> setting.getName().toLowerCase(Locale.ROOT).contains(value))
                        || keyLabel(module.getKeyCode()).toLowerCase(Locale.ROOT).contains(value)
                        || keyLabel(module.getKeyCode()).toLowerCase(Locale.ROOT).replace("_", "").contains(value.replace("_", ""));
            }
            if (!match) return false;
        }
        return true;
    }

    private String keyLabel(int keyCode) {
        if (keyCode < 0) return "NONE";
        String name = GLFW.glfwGetKeyName(keyCode, 0);
        if (name != null) return name;
        return switch (keyCode) {
            case GLFW.GLFW_KEY_RIGHT_SHIFT -> "RIGHT_SHIFT";
            case GLFW.GLFW_KEY_LEFT_SHIFT -> "LEFT_SHIFT";
            case GLFW.GLFW_KEY_RIGHT_CONTROL -> "RIGHT_CTRL";
            case GLFW.GLFW_KEY_LEFT_CONTROL -> "LEFT_CTRL";
            case GLFW.GLFW_KEY_RIGHT_ALT -> "RIGHT_ALT";
            case GLFW.GLFW_KEY_LEFT_ALT -> "LEFT_ALT";
            case GLFW.GLFW_KEY_SPACE -> "SPACE";
            case GLFW.GLFW_KEY_TAB -> "TAB";
            default -> "KEY_" + keyCode;
        };
    }

    private String settingValue(Setting<?> setting) {
        if (setting instanceof BooleanSetting b) return b.get() ? "ON" : "OFF";
        if (setting instanceof NumberSetting n) return String.format(Locale.ROOT, "%.1f", n.get());
        if (setting instanceof ModeSetting m) return m.get();
        return String.valueOf(setting.get());
    }

    private void drawButton(DrawContext ctx, int x, int y, int w, int h, String label, int color) {
        ctx.fill(x, y, x + w, y + h, color);
        ctx.drawCenteredTextWithShadow(textRenderer, Text.literal(label), x + w / 2, y + 5, TEXT);
    }

    private void drawSingleLine(DrawContext ctx, String value, int x, int y, int maxWidth, int color) {
        String line = value;
        while (!line.isEmpty() && textRenderer.getWidth(line) > maxWidth) line = line.substring(0, line.length() - 1);
        if (!line.equals(value) && line.length() > 3) line = line.substring(0, line.length() - 3) + "...";
        ctx.drawText(textRenderer, Text.literal(line), x, y, color, false);
    }

    private void drawWrapped(DrawContext ctx, String value, int x, int y, int maxWidth, int color) {
        StringBuilder line = new StringBuilder();
        for (String word : value.split(" ")) {
            String candidate = line.isEmpty() ? word : line + " " + word;
            if (textRenderer.getWidth(candidate) > maxWidth && !line.isEmpty()) {
                ctx.drawText(textRenderer, Text.literal(line.toString()), x, y, color, false);
                y += 10;
                line = new StringBuilder(word);
            } else line = new StringBuilder(candidate);
        }
        if (!line.isEmpty()) ctx.drawText(textRenderer, Text.literal(line.toString()), x, y, color, false);
    }

    @Override public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != GLFW.GLFW_MOUSE_BUTTON_LEFT && button != GLFW.GLFW_MOUSE_BUTTON_RIGHT) return super.mouseClicked(mouseX, mouseY, button);
        int w = Math.min(760, width - 32), h = Math.min(430, height - 32);
        int x = (width - w) / 2, y = (height - h) / 2;
        int cy = y + 82;
        for (String cat : CATEGORIES) {
            if (mouseX >= x + 8 && mouseX <= x + 146 && mouseY >= cy && mouseY <= cy + 27) {
                category = cat;
                moduleScroll = 0;
                selected = ModuleManager.all().stream().filter(m -> m.getCategory().equals(cat)).findFirst().orElse(null);
                return true;
            }
            cy += 32;
        }
        int by = y + h - 30;
        if (mouseY >= by && mouseY <= by + 18) {
            if (mouseX >= x + 170 && mouseX <= x + 244) {
                try { ConfigManager.save(); status = "Config saved"; }
                catch (IOException e) { status = "Save failed"; }
                return true;
            }
            if (mouseX >= x + 250 && mouseX <= x + 324) {
                try { ConfigManager.load(); status = "Config loaded"; }
                catch (IOException e) { status = "Load failed"; }
                return true;
            }
            if (mouseX >= x + 330 && mouseX <= x + 408) {
                if (client != null) client.setScreen(new ConfigManagerScreen(this));
                return true;
            }
        }
        int contentX = x + 170, contentY = y + 62, cardY = contentY + 36 - moduleScroll, cardW = w - 170 - 204;
        if (mouseX >= contentX + cardW - 105 && mouseX <= contentX + cardW && mouseY >= contentY + 10 && mouseY <= contentY + 28) { filterMode = (filterMode + 1) % 3; moduleScroll = 0; return true; }
        String filter = search == null ? "" : search.getText().toLowerCase(Locale.ROOT);
        int listTop = contentY + 34, listBottom = y + h - 40;
        for (ClientModule module : filteredModules(filter)) {
            if (mouseX >= contentX && mouseX <= contentX + cardW && mouseY >= listTop && mouseY <= listBottom
                    && mouseY >= cardY && mouseY <= cardY + 44) {
                selected = module;
                if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
                    awaitingKeybind = true;
                    status = "Press a key for " + module.getName();
                } else if (mouseX >= contentX + cardW - 40) {
                    module.toggle();
                    try { ConfigManager.save(); } catch (IOException e) { status = "Toggle changed; save failed"; }
                }
                return true;
            }
            cardY += 51;
        }
        if (selected != null && mouseX >= x + w - 182 && mouseX <= x + w - 8) {
            int sy = settingStartY + 38;
            for (Setting<?> setting : selected.getSettings()) {
                if (setting instanceof NumberSetting number) {
                    if (mouseY >= sy + 20 && mouseY <= sy + 34) {
                        int midpoint = x + w - 95;
                        if (mouseX < midpoint) number.decrease(); else number.increase();
                        try { ConfigManager.save(); } catch (IOException e) { status = "Setting changed; save failed"; }
                        return true;
                    }
                    sy += 44;
                } else {
                    if (mouseY >= sy && mouseY <= sy + 24) {
                        if (setting instanceof BooleanSetting bool) bool.toggle();
                        else if (setting instanceof ModeSetting mode) mode.cycle();
                        try { ConfigManager.save(); } catch (IOException e) { status = "Setting changed; save failed"; }
                        return true;
                    }
                    sy += 31;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (awaitingKeybind && selected != null) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE || keyCode == GLFW.GLFW_KEY_DELETE || keyCode == GLFW.GLFW_KEY_BACKSPACE) {
                selected.setKeyCode(-1);
                status = "Keybind cleared";
            } else if (keyCode == GLFW.GLFW_KEY_RIGHT_SHIFT || keyCode == GLFW.GLFW_KEY_F6) {
                status = "Reserved key: menu/HUD editor";
                awaitingKeybind = false;
                return true;
            } else {
                for (ClientModule module : ModuleManager.all()) {
                    if (module != selected && module.getKeyCode() == keyCode) module.setKeyCode(-1);
                }
                selected.setKeyCode(keyCode);
                status = "Keybind saved: " + keyLabel(keyCode);
            }
            awaitingKeybind = false;
            try { ConfigManager.save(); } catch (IOException ignored) { status = "Key set; config save failed"; }
            return true;
        }
        if ((modifiers & GLFW.GLFW_MOD_CONTROL) != 0 && keyCode == GLFW.GLFW_KEY_F) {
            if (search != null) { search.setFocused(true); search.setCursorToEnd(false); }
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) { close(); return true; }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        int w = Math.min(760, width - 32), h = Math.min(430, height - 32);
        int x = (width - w) / 2, y = (height - h) / 2;
        int contentX = x + 170, contentY = y + 62, cardW = w - 170 - 204;
        if (mouseX >= contentX && mouseX <= contentX + cardW && mouseY >= contentY + 30 && mouseY <= y + h - 40) {
            String filter = search == null ? "" : search.getText().toLowerCase(Locale.ROOT);
            int visibleHeight = y + h - 40 - (contentY + 34);
            int maxScroll = Math.max(0, filteredModules(filter).size() * 51 - visibleHeight);
            moduleScroll = Math.max(0, Math.min(maxScroll, moduleScroll - (int)(verticalAmount * 34)));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }
    @Override public void close() {
        try { ConfigManager.save(); } catch (IOException ignored) { }
        super.close();
    }

    @Override public boolean shouldPause() { return false; }
}
