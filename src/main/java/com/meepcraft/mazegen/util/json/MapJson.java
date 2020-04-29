package com.meepcraft.mazegen.util.json;

import com.google.gson.*;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

public class MapJson implements JsonDeserializer
{
    @Override
    public Object deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException
    {
        HashMap<String, Object> out = new HashMap<>();
        JsonObject obj = json.getAsJsonObject();
        for(Map.Entry<String, JsonElement> pair: obj.entrySet())
        {
            out.put(pair.getKey(), context.deserialize(pair.getValue(), Object.class));
        }
        return out;
    }
}
