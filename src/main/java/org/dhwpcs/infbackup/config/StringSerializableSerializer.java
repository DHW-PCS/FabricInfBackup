package org.dhwpcs.infbackup.config;

import com.google.gson.*;

import java.lang.reflect.Type;

public class StringSerializableSerializer implements JsonSerializer<StringSerializable>, JsonDeserializer<StringSerializable> {
    @Override
    public StringSerializable deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext ctx) throws JsonParseException {
        if(jsonElement.isJsonPrimitive()) {
            JsonPrimitive prim = jsonElement.getAsJsonPrimitive();
            if(prim.isString()) {
                String datum = prim.getAsString();
                if(type instanceof Class) {
                    Class<?> clz = (Class<?>) type;
                    if(StringSerializable.class.isAssignableFrom(clz)) {
                        return StringSerializable.deserialize(((Class<?>) type).asSubclass(StringSerializable.class), datum);
                    }
                }
                throw new JsonParseException("The type should be a subclass of StringSerializable.");
            }
        }
        throw new JsonParseException("The value should be a string.");
    }

    @Override
    public JsonElement serialize(StringSerializable ser, Type type, JsonSerializationContext ctx) {
        return new JsonPrimitive(ser.serialize());
    }
}
