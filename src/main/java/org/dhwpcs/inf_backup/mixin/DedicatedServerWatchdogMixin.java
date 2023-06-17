package org.dhwpcs.inf_backup.mixin;

import net.minecraft.server.dedicated.DedicatedServerWatchdog;
import net.minecraft.server.dedicated.MinecraftDedicatedServer;
import org.dhwpcs.inf_backup.mixin.api.ServerExt;
import org.dhwpcs.inf_backup.mixin.api.WatchdogExt;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(DedicatedServerWatchdog.class)
public abstract class DedicatedServerWatchdogMixin implements WatchdogExt {
    @Shadow @Final private long maxTickTime;
    @Unique private boolean active = true;

    @Redirect(
            method = "run",
            at = @At(
                    value = "FIELD",
                    opcode = Opcodes.GETFIELD,
                    target = "Lnet/minecraft/server/dedicated/DedicatedServerWatchdog;maxTickTime:J",
                    ordinal = 0
            )
    )
    private long redirectMaxTickTime(DedicatedServerWatchdog instance) {
        return active ? maxTickTime : Long.MAX_VALUE;
    }

    @Inject(
            method = "<init>",
            at = @At("TAIL")
    )
    private void constructorEnd(MinecraftDedicatedServer server, CallbackInfo ci) {
        ((ServerExt)server).setWatchdog(this);
    }

    @Override
    public boolean isActive() {
        return active;
    }

    @Override
    public void setActive(boolean active) {
        this.active = active;
    }
}
