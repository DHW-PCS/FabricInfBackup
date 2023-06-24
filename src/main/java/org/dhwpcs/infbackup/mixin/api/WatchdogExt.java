package org.dhwpcs.infbackup.mixin.api;

public interface WatchdogExt {
    void setActive(boolean active);
    boolean isActive();
}
