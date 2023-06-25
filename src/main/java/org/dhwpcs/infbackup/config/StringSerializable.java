package org.dhwpcs.infbackup.config;

import java.util.HashMap;
import java.util.Map;

public interface StringSerializable {
    String serialize();

    Map<Class<? extends StringSerializable>, Deserializer> DESERIALIZERS = new HashMap<>();
    static<T extends StringSerializable> T deserialize(Class<T> clz, String datum) {
        return (T) DESERIALIZERS.get(clz).deserialize(datum);
    }

    static StringSerializable deserialize(String datum) {
        throw new UnsupportedOperationException();
    }

    static<T extends StringSerializable> boolean register(Class<T> clz, Deserializer deserializer) {
        return DESERIALIZERS.putIfAbsent(clz, deserializer) == null;
    }

    StringSerializable NIL = () -> "<INVALID>";

    @FunctionalInterface
    interface Deserializer {
        StringSerializable deserialize(String datum);
    }
}
