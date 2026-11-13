package dev.ezkahak.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import java.util.ArrayDeque;
import java.util.Deque;

/** Small client-side toast queue; entries expire using client ticks, not wall-clock timers. */
public final class NotificationManager {
    private record Toast(String text, int ticks) {}
    private static final Deque<Toast> TOASTS = new ArrayDeque<>();
    private NotificationManager() {}
    public static void push(String text) {
        synchronized (TOASTS) { if (TOASTS.size() >= 5) TOASTS.removeFirst(); TOASTS.addLast(new Toast(text, 70)); }
    }
    public static void tick() {
        synchronized (TOASTS) {
            if (TOASTS.isEmpty()) return;
            Toast first = TOASTS.removeFirst();
            if (first.ticks() > 1) TOASTS.addFirst(new Toast(first.text(), first.ticks() - 1));
        }
    }
    public static void render(MinecraftClient client, DrawContext ctx) {
        if (client.player == null || client.options.hudHidden) return;
        var module = dev.ezkahak.client.module.ModuleManager.find("Notifications");
        if (module == null || !module.isEnabled()) return;
        synchronized (TOASTS) {
            int i = 0;
            for (Toast toast : TOASTS) {
                int w = client.textRenderer.getWidth(toast.text()) + 18, x = client.getWindow().getScaledWidth() - w - 10, y = 12 + i * 22;
                ctx.fill(x, y, x + w, y + 18, 0xDD101A28);
                ctx.fill(x, y, x + 2, y + 18, 0xFF22B8FF);
                ctx.drawText(client.textRenderer, Text.literal(toast.text()), x + 8, y + 5, 0xFFEAF5FF, true);
                i++;
            }
        }
    }
}
