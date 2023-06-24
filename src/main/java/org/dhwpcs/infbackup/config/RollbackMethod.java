package org.dhwpcs.infbackup.config;

import java.util.Set;

public enum RollbackMethod implements StringSerializable{
    SHUTDOWN,
    INSTANT;

    public static final Set<String> NAMES = Set.of(
            SHUTDOWN.name(),
            INSTANT.name()
    );

    @Override
    public String serialize() {
        return name();
    }

    public static RollbackMethod deserialize(String datum) {
        return RollbackMethod.NAMES.contains(datum) ? RollbackMethod.valueOf(datum) : null;
    }
}
