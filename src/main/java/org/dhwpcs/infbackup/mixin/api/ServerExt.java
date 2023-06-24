package org.dhwpcs.infbackup.mixin.api;

public interface ServerExt {
    WatchdogExt getWatchdog();
    void setWatchdog(WatchdogExt watchdog);
}
