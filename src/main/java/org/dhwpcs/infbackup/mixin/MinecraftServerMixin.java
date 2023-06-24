package org.dhwpcs.infbackup.mixin;

import net.minecraft.server.MinecraftServer;
import org.dhwpcs.infbackup.mixin.api.ServerExt;
import org.dhwpcs.infbackup.mixin.api.WatchdogExt;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(MinecraftServer.class)
public abstract class MinecraftServerMixin implements ServerExt {
    @Unique private WatchdogExt watchdog;

    @Override
    public WatchdogExt getWatchdog() {
        return watchdog;
    }

    @Override
    public void setWatchdog(WatchdogExt watchdog) {
        this.watchdog = watchdog;
    }
}
