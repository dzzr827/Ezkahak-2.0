package dev.ezkahak.client.module;

import dev.ezkahak.client.setting.BooleanSetting;
import dev.ezkahak.client.setting.ModeSetting;
import dev.ezkahak.client.setting.NumberSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.item.Items;
import org.lwjgl.glfw.GLFW;
import java.util.IdentityHashMap;
import java.util.Map;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ModuleManager {
    private static final List<ClientModule> MODULES = new ArrayList<>();
    private static final Map<ClientModule, Boolean> KEY_WAS_DOWN = new IdentityHashMap<>();

    private ModuleManager() { }

    public static void initialize() {
        if (!MODULES.isEmpty()) return;

        MODULES.add(new ClientModule("Click GUI", "Render", "Open the EzkaHak settings interface.") { });
        MODULES.add(new ClientModule("HUD", "HUD", "Show the compact client status overlay.") {
            public final BooleanSetting showFps = addSetting(new BooleanSetting("FPS", true));
            public final BooleanSetting showCoordinates = addSetting(new BooleanSetting("Coordinates", true));
            public final BooleanSetting showBrand = addSetting(new BooleanSetting("Client name", true));
            public final BooleanSetting showPing = addSetting(new BooleanSetting("Ping", true));
            public final BooleanSetting showArmor = addSetting(new BooleanSetting("Armor status", true));
            public final ModeSetting theme = addSetting(new ModeSetting("Accent", "Cyan", "Cyan", "Blue", "Purple"));
        });
        MODULES.add(new ClientModule("Array List", "HUD", "Show enabled modules on screen.") {
            public final NumberSetting scale = addSetting(new NumberSetting("Text scale", 1.0, 0.5, 2.0, 0.1));
        });
        MODULES.add(new ClientModule("Fullbright", "Render", "Client-side brightness convenience toggle.") {
            public final NumberSetting brightness = addSetting(new NumberSetting("Brightness", 1.0, 0.0, 1.0, 0.1));
        });
        MODULES.add(new ClientModule("Sprint", "Movement", "Sprint helper placeholder; gameplay logic can be added later.") {
            public final BooleanSetting onlyForward = addSetting(new BooleanSetting("Forward only", true));
        });
        MODULES.add(new ClientModule("Notifications", "Misc", "Show compact notifications when modules change.") {
            public final ModeSetting style = addSetting(new ModeSetting("Style", "Compact", "Compact", "Wide"));
        });
        MODULES.add(new ClientModule("Visual Effects", "Render", "Animated cyan accents and subtle client-side visual polish.") {
            public final BooleanSetting animatedAccent = addSetting(new BooleanSetting("Animated accent", true));
            public final BooleanSetting panelGlow = addSetting(new BooleanSetting("Panel glow", true));
        });
        MODULES.add(new ClientModule("Crystal Aura", "Combat", "Basic crystal combat automation: attacks nearby crystals and places crystals over obsidian/bedrock near a target.") {
            public final NumberSetting range = addSetting(new NumberSetting("Range", 4.0, 1.0, 6.0, 0.1));
            public final NumberSetting placeDelay = addSetting(new NumberSetting("Place delay", 2.0, 0.0, 20.0, 1.0));
            public final BooleanSetting rotate = addSetting(new BooleanSetting("Rotate", true));
            public final BooleanSetting pauseOnEat = addSetting(new BooleanSetting("Pause while eating", true));
            public final ModeSetting targetMode = addSetting(new ModeSetting("Target mode", "Nearest", "Nearest", "Lowest health"));
            private int actionTicks;

            @Override public void onClientTick() {
                MinecraftClient mc = MinecraftClient.getInstance();
                if (mc.player == null || mc.world == null || mc.interactionManager == null) return;
                if (pauseOnEat.get() && mc.player.isUsingItem()) return;
                if (actionTicks++ < placeDelay.get().intValue()) return;
                actionTicks = 0;
                double r = range.get();
                var player = mc.player;
                var world = mc.world;
                var target = world.getPlayers().stream()
                    .filter(p -> p != player && p.isAlive() && !p.isSpectator() && player.distanceTo(p) <= r + 2.0)
                    .min((a, b) -> targetMode.get().equals("Lowest health")
                        ? Float.compare(a.getHealth(), b.getHealth())
                        : Double.compare(player.distanceTo(a), player.distanceTo(b)))
                    .orElse(null);

                // Detonate a crystal close to the target first.
                if (target != null) {
                    var crystals = world.getEntitiesByClass(net.minecraft.entity.decoration.EndCrystalEntity.class,
                        player.getBoundingBox().expand(r), c -> c.isAlive() && c.squaredDistanceTo(target) < 16.0);
                    if (!crystals.isEmpty()) {
                        mc.interactionManager.attackEntity(player, crystals.get(0));
                        player.swingHand(net.minecraft.util.Hand.MAIN_HAND);
                        return;
                    }
                }
                if (target == null) return;
                int crystalSlot = -1;
                for (int i = 0; i < 9; i++) if (player.getInventory().getStack(i).isOf(net.minecraft.item.Items.END_CRYSTAL)) { crystalSlot = i; break; }
                if (crystalSlot < 0) return;
                var origin = target.getBlockPos();
                int radius = (int)Math.ceil(r) + 1;
                for (int x = -radius; x <= radius; x++) for (int y = -2; y <= 1; y++) for (int z = -radius; z <= radius; z++) {
                    var base = origin.add(x, y, z);
                    if (player.squaredDistanceTo(base.toCenterPos()) > r * r) continue;
                    if (!world.getBlockState(base).isOf(net.minecraft.block.Blocks.OBSIDIAN)
                        && !world.getBlockState(base).isOf(net.minecraft.block.Blocks.BEDROCK)) continue;
                    if (!world.getBlockState(base.up()).isAir() || !world.getBlockState(base.up(2)).isAir()) continue;
                    if (target.squaredDistanceTo(base.toCenterPos()) > 16.0) continue;
                    int previous = player.getInventory().selectedSlot;
                    player.getInventory().selectedSlot = crystalSlot;
                    if (mc.getNetworkHandler() != null) mc.getNetworkHandler().sendPacket(
                        new net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket(crystalSlot));
                    var hit = new net.minecraft.util.hit.BlockHitResult(base.toCenterPos().add(0, 0.5, 0),
                        net.minecraft.util.math.Direction.UP, base, false);
                    mc.interactionManager.interactBlock(player, net.minecraft.util.Hand.MAIN_HAND, hit);
                    player.swingHand(net.minecraft.util.Hand.MAIN_HAND);
                    player.getInventory().selectedSlot = previous;
                    if (mc.getNetworkHandler() != null) mc.getNetworkHandler().sendPacket(
                        new net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket(previous));
                    return;
                }
            }
        });
        MODULES.add(new AutoFarmModule());

        MODULES.add(new ClientModule("AutoTotem", "Combat", "Automatically moves a Totem of Undying into the offhand when needed.") {
            public final NumberSetting healthThreshold = addSetting(new NumberSetting("Health threshold", 8.0, 1.0, 20.0, 1.0));
            public final BooleanSetting onlyLowHealth = addSetting(new BooleanSetting("Only at low health", false));
            public final BooleanSetting preferHotbar = addSetting(new BooleanSetting("Prefer hotbar", true));
            public final BooleanSetting pauseInClientMenu = addSetting(new BooleanSetting("Pause in EzkaHak menu", true));

            @Override public void onClientTick() {
                MinecraftClient mc = MinecraftClient.getInstance();
                if (mc.player == null || mc.world == null || mc.interactionManager == null) return;
                if (pauseInClientMenu.get() && mc.currentScreen instanceof dev.ezkahak.client.EzkaHakClickGui) return;
                var player = mc.player;
                var inventory = player.getInventory();
                if (inventory.getStack(40).isOf(Items.TOTEM_OF_UNDYING)) return;
                if (onlyLowHealth.get() && player.getHealth() + player.getAbsorptionAmount() > healthThreshold.get()) return;

                // Use the player's normal inventory handler directly. This never opens
                // the inventory screen and avoids accidentally clicking slots in a chest.
                // While another container is active, wait until the player handler is active.
                var handler = player.playerScreenHandler;
                if (player.currentScreenHandler != handler) return;

                int hotbarSlotId = -1;
                int inventorySlotId = -1;
                for (int slotId = 0; slotId < handler.slots.size(); slotId++) {
                    var slot = handler.slots.get(slotId);
                    if (slot.inventory != inventory || slot.getIndex() == 40 || !slot.getStack().isOf(Items.TOTEM_OF_UNDYING)) continue;
                    int inventoryIndex = slot.getIndex();
                    if (inventoryIndex >= 0 && inventoryIndex <= 8) {
                        hotbarSlotId = slotId;
                        break;
                    }
                    if (inventorySlotId < 0) inventorySlotId = slotId;
                }
                int sourceSlotId = preferHotbar.get() && hotbarSlotId >= 0 ? hotbarSlotId
                        : (inventorySlotId >= 0 ? inventorySlotId : hotbarSlotId);
                if (sourceSlotId < 0) return;
                // SWAP button 40 exchanges the source slot with the offhand. The
                // normal handler works while the player is in-world; no GUI is opened.
                mc.interactionManager.clickSlot(handler.syncId, sourceSlotId, 40, SlotActionType.SWAP, player);
            }
        });

        MODULES.add(new ClientModule("Velocity", "Combat", "Reduces incoming player velocity from server velocity updates; percentages mean reduction amount.") {
            public final BooleanSetting horizontal = addSetting(new BooleanSetting("Horizontal", true));
            public final BooleanSetting vertical = addSetting(new BooleanSetting("Vertical", true));
            public final NumberSetting horizontalPercent = addSetting(new NumberSetting("Horizontal percent", 100.0, 0.0, 100.0, 5.0));
            public final NumberSetting verticalPercent = addSetting(new NumberSetting("Vertical percent", 100.0, 0.0, 100.0, 5.0));
        });
    }

    public static List<ClientModule> all() { return Collections.unmodifiableList(MODULES); }

    public static ClientModule find(String name) {
        return MODULES.stream().filter(module -> module.getName().equalsIgnoreCase(name)).findFirst().orElse(null);
    }

    public static void tick(MinecraftClient client) {
        long window = client.getWindow().getHandle();
        boolean allowHotkeys = client.currentScreen == null && client.player != null;
        dev.ezkahak.client.NotificationManager.tick();
        for (ClientModule module : MODULES) {
            int key = module.getKeyCode();
            boolean down = allowHotkeys && key > 0 && GLFW.glfwGetKey(window, key) == GLFW.GLFW_PRESS;
            boolean wasDown = KEY_WAS_DOWN.getOrDefault(module, false);
            if (down && !wasDown) module.toggle();
            KEY_WAS_DOWN.put(module, down);
            if (module.isEnabled()) module.onClientTick();
        }
    }
}
