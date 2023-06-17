package org.dhwpcs.inf_backup.mixin.api;

public interface ServerExt {
    WatchdogExt getWatchdog();
    void setWatchdog(WatchdogExt watchdog);
}
