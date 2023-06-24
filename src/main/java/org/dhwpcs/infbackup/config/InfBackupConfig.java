package org.dhwpcs.infbackup.config;

import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;

import java.util.Map;

public class InfBackupConfig {

    public static final Multimap<String, String> CONFIG_ENTRIES = MultimapBuilder.hashKeys().hashSetValues().build();
    public static final Map<String, String[]> DESCRIPTION = Map.of(
            "rollback_method", lines(
                    "The method to rollback. There are two valid values:",
                    "First, SHUTDOWN, which means the save would be restored when the server shuts down.",
                    "Second, INSTANT, which means the save would be restored once the selected region is unloaded from the memory"
            )
    );

    static {
        CONFIG_ENTRIES.putAll("rollback_method", RollbackMethod.NAMES);
    }
    public RollbackMethod rollback_method;
    public InfBackupConfig() {}

    public static InfBackupConfig getDefault() {
        InfBackupConfig result = new InfBackupConfig();
        result.rollback_method = RollbackMethod.SHUTDOWN;
        return result;
    }

    private static String[] lines(String... strings) {
        return strings;
    }

    public boolean set(String key, String value) {
        switch (key) {
            case "rollback_method" -> {
                if(!RollbackMethod.NAMES.contains(value)) {
                    return false;
                }
                rollback_method = RollbackMethod.valueOf(value);
                return true;
            }
            default -> {
                return true;
            }
        }
    }

    public StringSerializable get(String key) {
        switch (key) {
            case "rollback_method" -> {
                return rollback_method;
            }

            default -> {
                return null;
            }
        }
    }

    public boolean has(String key) {
        return CONFIG_ENTRIES.containsKey(key);
    }
}
