package dev.ezkahak.client.mixin;

import dev.ezkahak.client.module.ClientModule;
import dev.ezkahak.client.module.ModuleManager;
import dev.ezkahak.client.setting.BooleanSetting;
import dev.ezkahak.client.setting.NumberSetting;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayNetworkHandler.class)
public abstract class VelocityMixin {
    @Inject(method = "onEntityVelocityUpdate", at = @At("TAIL"))
    private void ezkahak$reduceVelocity(EntityVelocityUpdateS2CPacket packet, CallbackInfo ci) {
        var client = net.minecraft.client.MinecraftClient.getInstance();
        if (client.player == null || packet.getEntityId() != client.player.getId()) return;
        ClientModule module = ModuleManager.find("Velocity");
        if (module == null || !module.isEnabled()) return;
        boolean horizontal = true, vertical = true;
        double h = 0, v = 0;
        for (var setting : module.getSettings()) {
            if (setting instanceof BooleanSetting b && b.getName().equals("Horizontal")) horizontal = b.get();
            if (setting instanceof BooleanSetting b && b.getName().equals("Vertical")) vertical = b.get();
            if (setting instanceof NumberSetting n && n.getName().equals("Horizontal percent")) h = n.get() / 100.0;
            if (setting instanceof NumberSetting n && n.getName().equals("Vertical percent")) v = n.get() / 100.0;
        }
        var velocity = client.player.getVelocity();
        client.player.setVelocity(velocity.x * (horizontal ? 1.0 - h : 1.0),
                                  velocity.y * (vertical ? 1.0 - v : 1.0),
                                  velocity.z * (horizontal ? 1.0 - h : 1.0));
    }
}
