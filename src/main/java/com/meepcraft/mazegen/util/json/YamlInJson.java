package com.meepcraft.mazegen.util.json;
import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import com.sk89q.worldedit.blocks.BaseBlock;
import org.bukkit.Location;

import java.lang.reflect.Type;
import java.security.InvalidParameterException;
import java.util.Map;

public class YamlInJson implements JsonSerializer, JsonDeserializer
{

    @Override
    public Object deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException
    {
        if(typeOfT.equals(Location.class))
        {
            Gson gson = new Gson();
            return Location.deserialize(gson.fromJson(json, new TypeToken<Map<String, Object>>(){}.getType()));
        }
        else if(typeOfT.equals(BaseBlock.class))
        {
            JsonObject obj = json.getAsJsonObject();
            return new BaseBlock(obj.get("id").getAsShort(),obj.get("data").getAsShort());
        }
        else
        {
            throw new InvalidParameterException("Can't deserialize: " + typeOfT.getTypeName());
        }
    }

    @Override
    public JsonElement serialize(Object src, Type typeOfSrc, JsonSerializationContext context)
    {
        if(src instanceof Location)
        {
            JsonObject out = new JsonObject();
            for(Map.Entry<String, Object> pair: ((Location)(src)).serialize().entrySet())
            {
                out.add(pair.getKey(),context.serialize(pair.getValue()));
            }
            return out;
        }
        else if(src instanceof BaseBlock)
        {
            JsonObject out = new JsonObject();
            BaseBlock block = (BaseBlock) src;
            out.add("id", new JsonPrimitive(block.getId()));
            out.add("data", new JsonPrimitive(block.getData()));
            return out;
        }
        else
        {
            throw new InvalidParameterException();
        }
    }
}
