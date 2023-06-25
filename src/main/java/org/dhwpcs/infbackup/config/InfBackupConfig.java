package org.dhwpcs.infbackup.config;

import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import com.google.gson.annotations.Expose;
import org.dhwpcs.infbackup.FabricEntrypoint;
import org.dhwpcs.infbackup.util.Util;

import java.util.*;
import java.util.function.Function;

public class InfBackupConfig {

    public static final Set<String> ENTRIES = new HashSet<>();
    public static final Map<String, Function<FabricEntrypoint, Iterable<String>>> SUGGESTIONS = new HashMap<>();
    public static final Multimap<String, Filter> FILTERS = MultimapBuilder.hashKeys().hashSetValues().build();
    public static final Map<String, Accessor> ACCESSORS = new HashMap<>();
    public static final Map<String, Iterable<String>> DESCRIPTION = new HashMap<>();

    public RollbackMethod rollback_method;

    @Expose(serialize = false, deserialize = false)
    public transient FabricEntrypoint entrypoint;

    public InfBackupConfig() {
    }


    public boolean set(String key, String value) {
        if (!has(key)) {
            return false;
        }
        Accessor accessor = ACCESSORS.get(key);
        StringSerializable oldValue = accessor.get(this);
        for (Filter filter : FILTERS.get(key)) {
            if (!filter.interceptSet(entrypoint, oldValue, value)) {
                return false;
            }
        }
        accessor.set(this, value);
        return true;
    }

    public StringSerializable get(String key) {
        if (!has(key)) {
            return StringSerializable.NIL;
        }
        return ACCESSORS.get(key).get(this);
    }

    public static boolean has(String key) {
        return ENTRIES.contains(key);
    }

    public static Iterable<String> getSuggestions(String key, FabricEntrypoint entrypoint) {
        return SUGGESTIONS.get(key).apply(entrypoint);
    }

    public static Iterable<String> getDescription(String key) {
        return DESCRIPTION.get(key);
    }

    public static InfBackupConfig getDefault() {
        InfBackupConfig result = new InfBackupConfig();
        result.rollback_method = RollbackMethod.SHUTDOWN;
        return result;
    }

    private static <T extends StringSerializable> void register(String id, Function<FabricEntrypoint, Iterable<String>> suggestedValue, Iterable<String> description, Accessor accessor, Filter... filters) {
        ENTRIES.add(id);
        SUGGESTIONS.put(id, suggestedValue);
        DESCRIPTION.put(id, description);
        FILTERS.putAll(id, Util.asIterable(filters));
        ACCESSORS.put(id, accessor);
    }


    static {
        register("rollback_method", unused -> RollbackMethod.NAMES, Util.asIterable(
                "The method to rollback. There are two valid values:",
                "First, SHUTDOWN, which means the save would be restored when the server shuts down.",
                "Second, INSTANT, which means the save would be restored once the selected region is unloaded from the memory"
        ), Accessor.ROLLBACK_METHOD, Filter.ROLLBACK_METHOD);
    }


    interface Filter {
        boolean interceptSet(FabricEntrypoint entrypoint, StringSerializable old, String latest);

        Filter ROLLBACK_METHOD = (entrypoint, old, latest) -> RollbackMethod.NAMES.contains(latest);
    }

    interface Accessor {
        StringSerializable get(InfBackupConfig config);

        void set(InfBackupConfig config, String value);

        Accessor ROLLBACK_METHOD = new Accessor() {

            @Override
            public StringSerializable get(InfBackupConfig config) {
                return config.rollback_method;
            }

            @Override
            public void set(InfBackupConfig config, String value) {
                config.rollback_method = RollbackMethod.deserialize(value);
            }
        };
    }
}
