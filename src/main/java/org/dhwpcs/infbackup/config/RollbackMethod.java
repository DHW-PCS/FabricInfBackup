package org.dhwpcs.infbackup.config;

import java.util.Set;

public enum RollbackMethod implements StringSerializable {
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

    static RollbackMethod deserialize(String datum) {
        return RollbackMethod.valueOf(datum);
    }

    static {
        StringSerializable.register(RollbackMethod.class, RollbackMethod::deserialize);
    }
}
