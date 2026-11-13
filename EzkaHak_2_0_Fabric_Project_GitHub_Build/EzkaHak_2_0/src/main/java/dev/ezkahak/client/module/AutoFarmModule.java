package dev.ezkahak.client.module;

import dev.ezkahak.client.setting.BooleanSetting;
import dev.ezkahak.client.setting.ModeSetting;
import dev.ezkahak.client.setting.NumberSetting;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

/**
 * Experimental client-side AutoFarm. Server commands, block reach, and container
 * actions remain server-authoritative; this module cannot bypass server limits.
 */
public final class AutoFarmModule extends ClientModule {
    public final ModeSetting target = addSetting(new ModeSetting("Target block", "Sand",
            "Sand", "Red Sand", "Gold Ore", "Deepslate Gold", "Iron Ore", "Deepslate Iron",
            "Diamond Ore", "Deepslate Diamond", "Coal Ore", "All Ores", "Gravel", "Stone", "Netherrack"));
    public final NumberSetting scanRange = addSetting(new NumberSetting("Scan range", 4.0, 2.0, 5.0, 1.0));
    public final NumberSetting mineDelay = addSetting(new NumberSetting("Mine delay (ticks)", 4.0, 1.0, 20.0, 1.0));
    public final BooleanSetting returnWhenFull = addSetting(new BooleanSetting("Return when full", true));
    public final BooleanSetting depositToChest = addSetting(new BooleanSetting("Deposit to chest", true));

    private enum State { MINING, RETURNING_HOME, LOOKING_FOR_CHEST, DEPOSITING, RETURNING_TO_MINE }
    private State state = State.MINING;
    private BlockPos currentTarget;
    private int delayTicks;
    private int stateTicks;
    private int depositTicks;
    private boolean depositedSomething;

    public AutoFarmModule() {
        super("AutoFarm", "World", "Mines selected nearby blocks at /home 1; when full, travels to /home 2, deposits matching drops into a nearby chest, then returns to /home 1.");
    }

    @Override protected void onEnable() {
        state = State.MINING;
        currentTarget = null;
        delayTicks = stateTicks = depositTicks = 0;
        depositedSomething = false;
    }

    @Override protected void onDisable() {
        currentTarget = null;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.interactionManager != null && mc.player != null) {
            mc.interactionManager.cancelBlockBreaking();
        }
    }

    @Override public void onClientTick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null || mc.interactionManager == null || mc.getNetworkHandler() == null) return;
        if (mc.currentScreen instanceof dev.ezkahak.client.EzkaHakClickGui) return;

        switch (state) {
            case MINING -> tickMining(mc);
            case RETURNING_HOME -> tickReturningHome(mc);
            case LOOKING_FOR_CHEST -> tickLookingForChest(mc);
            case DEPOSITING -> tickDepositing(mc);
            case RETURNING_TO_MINE -> tickReturningToMine(mc);
        }
    }

    private void tickMining(MinecraftClient mc) {
        if (returnWhenFull.get() && isInventoryFull(mc)) {
            mc.getNetworkHandler().sendChatCommand("home 2");
            state = State.RETURNING_HOME;
            stateTicks = 0;
            currentTarget = null;
            mc.interactionManager.cancelBlockBreaking();
            return;
        }
        if (mc.player.isUsingItem()) return;
        if (++delayTicks < mineDelay.get().intValue()) {
            if (currentTarget != null) mc.interactionManager.updateBlockBreakingProgress(currentTarget, Direction.UP);
            return;
        }
        delayTicks = 0;
        BlockPos next = findTarget(mc);
        if (next == null) {
            currentTarget = null;
            mc.interactionManager.cancelBlockBreaking();
            return;
        }
        if (!next.equals(currentTarget)) {
            if (currentTarget != null) mc.interactionManager.cancelBlockBreaking();
            currentTarget = next;
            mc.interactionManager.attackBlock(currentTarget, Direction.UP);
        } else {
            mc.interactionManager.updateBlockBreakingProgress(currentTarget, Direction.UP);
        }
        mc.player.swingHand(Hand.MAIN_HAND);
    }

    private BlockPos findTarget(MinecraftClient mc) {
        BlockPos origin = mc.player.getBlockPos();
        int r = scanRange.get().intValue();
        BlockPos best = null;
        double bestDistance = Double.MAX_VALUE;
        for (int x = -r; x <= r; x++) for (int y = -r; y <= r; y++) for (int z = -r; z <= r; z++) {
            BlockPos pos = origin.add(x, y, z);
            double distance = mc.player.squaredDistanceTo(pos.toCenterPos());
            if (distance > r * r || distance >= bestDistance) continue;
            BlockState blockState = mc.world.getBlockState(pos);
            if (!matches(blockState.getBlock())) continue;
            if (blockState.isAir() || !blockState.getFluidState().isEmpty()) continue;
            best = pos;
            bestDistance = distance;
        }
        return best;
    }

    private boolean matches(Block block) {
        String mode = target.get();
        if (mode.equals("Sand")) return block == Blocks.SAND;
        if (mode.equals("Red Sand")) return block == Blocks.RED_SAND;
        if (mode.equals("Gold Ore")) return block == Blocks.GOLD_ORE || block == Blocks.NETHER_GOLD_ORE;
        if (mode.equals("Deepslate Gold")) return block == Blocks.DEEPSLATE_GOLD_ORE;
        if (mode.equals("Iron Ore")) return block == Blocks.IRON_ORE;
        if (mode.equals("Deepslate Iron")) return block == Blocks.DEEPSLATE_IRON_ORE;
        if (mode.equals("Diamond Ore")) return block == Blocks.DIAMOND_ORE;
        if (mode.equals("Deepslate Diamond")) return block == Blocks.DEEPSLATE_DIAMOND_ORE;
        if (mode.equals("Coal Ore")) return block == Blocks.COAL_ORE;
        if (mode.equals("Gravel")) return block == Blocks.GRAVEL;
        if (mode.equals("Stone")) return block == Blocks.STONE;
        if (mode.equals("Netherrack")) return block == Blocks.NETHERRACK;
        if (mode.equals("All Ores")) return block == Blocks.COAL_ORE || block == Blocks.DEEPSLATE_COAL_ORE
                || block == Blocks.IRON_ORE || block == Blocks.DEEPSLATE_IRON_ORE
                || block == Blocks.GOLD_ORE || block == Blocks.DEEPSLATE_GOLD_ORE || block == Blocks.NETHER_GOLD_ORE
                || block == Blocks.COPPER_ORE || block == Blocks.DEEPSLATE_COPPER_ORE
                || block == Blocks.REDSTONE_ORE || block == Blocks.DEEPSLATE_REDSTONE_ORE
                || block == Blocks.LAPIS_ORE || block == Blocks.DEEPSLATE_LAPIS_ORE
                || block == Blocks.DIAMOND_ORE || block == Blocks.DEEPSLATE_DIAMOND_ORE
                || block == Blocks.EMERALD_ORE || block == Blocks.DEEPSLATE_EMERALD_ORE
                || block == Blocks.NETHER_QUARTZ_ORE || block == Blocks.ANCIENT_DEBRIS;
        return false;
    }

    private boolean isInventoryFull(MinecraftClient mc) {
        for (int i = 0; i < 36; i++) if (mc.player.getInventory().getStack(i).isEmpty()) return false;
        return true;
    }

    private void tickReturningHome(MinecraftClient mc) {
        // Give the server time to process /home and load the destination.
        if (++stateTicks >= 40) {
            state = depositToChest.get() ? State.LOOKING_FOR_CHEST : State.RETURNING_TO_MINE;
            stateTicks = 0;
        }
    }

    private void tickLookingForChest(MinecraftClient mc) {
        if (++stateTicks > 600) {
            // Stay at the storage home rather than returning to mine with a full inventory.
            setEnabled(false);
            return;
        }
        if (mc.player.currentScreenHandler != mc.player.playerScreenHandler) {
            state = State.DEPOSITING;
            depositTicks = 0;
            depositedSomething = false;
            return;
        }
        BlockPos chest = findNearbyChest(mc);
        if (chest == null) return;
        BlockHitResult hit = new BlockHitResult(chest.toCenterPos(), Direction.UP, chest, false);
        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, hit);
        mc.player.swingHand(Hand.MAIN_HAND);
    }

    private BlockPos findNearbyChest(MinecraftClient mc) {
        BlockPos origin = mc.player.getBlockPos();
        for (int r = 1; r <= 5; r++) {
            for (int x = -r; x <= r; x++) for (int y = -2; y <= 2; y++) for (int z = -r; z <= r; z++) {
                BlockPos pos = origin.add(x, y, z);
                var block = mc.world.getBlockState(pos).getBlock();
                if (block == Blocks.CHEST || block == Blocks.TRAPPED_CHEST || block == Blocks.BARREL) return pos;
            }
        }
        return null;
    }

    private void tickDepositing(MinecraftClient mc) {
        ScreenHandler handler = mc.player.currentScreenHandler;
        if (handler == mc.player.playerScreenHandler) {
            state = State.RETURNING_TO_MINE;
            stateTicks = 0;
            return;
        }
        if (depositTicks++ < 8) return;
        depositTicks = 0;
        boolean movedAnything = false;
        // Quick-move only the selected farm drops, keeping tools and survival items.
        for (int i = handler.slots.size() - 1; i >= 0; i--) {
            var slot = handler.slots.get(i);
            if (slot.inventory == mc.player.getInventory() && isFarmDrop(slot.getStack())) {
                mc.interactionManager.clickSlot(handler.syncId, i, 0, SlotActionType.QUICK_MOVE, mc.player);
                movedAnything = true;
                depositedSomething = true;
                break;
            }
        }
        if (!movedAnything) {
            mc.player.closeHandledScreen();
            if (depositedSomething) {
                state = State.RETURNING_TO_MINE;
                stateTicks = 0;
            } else {
                // No matching drops could be stored; do not loop back with a full inventory.
                setEnabled(false);
            }
        }
    }

    private boolean isFarmDrop(ItemStack stack) {
        if (stack.isEmpty()) return false;
        String mode = target.get();
        if (mode.equals("Sand")) return stack.isOf(Items.SAND);
        if (mode.equals("Red Sand")) return stack.isOf(Items.RED_SAND);
        if (mode.equals("Gold Ore")) return stack.isOf(Items.GOLD_ORE) || stack.isOf(Items.NETHER_GOLD_ORE) || stack.isOf(Items.RAW_GOLD);
        if (mode.equals("Deepslate Gold")) return stack.isOf(Items.DEEPSLATE_GOLD_ORE) || stack.isOf(Items.RAW_GOLD);
        if (mode.equals("Iron Ore")) return stack.isOf(Items.IRON_ORE) || stack.isOf(Items.RAW_IRON);
        if (mode.equals("Deepslate Iron")) return stack.isOf(Items.DEEPSLATE_IRON_ORE) || stack.isOf(Items.RAW_IRON);
        if (mode.equals("Diamond Ore")) return stack.isOf(Items.DIAMOND_ORE) || stack.isOf(Items.DIAMOND);
        if (mode.equals("Deepslate Diamond")) return stack.isOf(Items.DEEPSLATE_DIAMOND_ORE) || stack.isOf(Items.DIAMOND);
        if (mode.equals("Coal Ore")) return stack.isOf(Items.COAL_ORE) || stack.isOf(Items.COAL);
        if (mode.equals("Gravel")) return stack.isOf(Items.GRAVEL) || stack.isOf(Items.FLINT);
        if (mode.equals("Stone")) return stack.isOf(Items.STONE) || stack.isOf(Items.COBBLESTONE);
        if (mode.equals("Netherrack")) return stack.isOf(Items.NETHERRACK);
        return stack.isOf(Items.COAL) || stack.isOf(Items.RAW_IRON) || stack.isOf(Items.RAW_GOLD)
                || stack.isOf(Items.COPPER_INGOT) || stack.isOf(Items.REDSTONE) || stack.isOf(Items.LAPIS_LAZULI)
                || stack.isOf(Items.DIAMOND) || stack.isOf(Items.EMERALD) || stack.isOf(Items.QUARTZ)
                || stack.isOf(Items.ANCIENT_DEBRIS) || stack.isOf(Items.IRON_ORE) || stack.isOf(Items.GOLD_ORE);
    }

    private void tickReturningToMine(MinecraftClient mc) {
        if (stateTicks++ == 0) mc.getNetworkHandler().sendChatCommand("home 1");
        if (stateTicks >= 40) {
            state = State.MINING;
            stateTicks = 0;
            currentTarget = null;
        }
    }
}
