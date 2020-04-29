package com.meepcraft.mazegen.types;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.Serializers;
import com.meepcraft.mazegen.Main;
import com.sk89q.worldedit.blocks.BaseBlock;
import org.bukkit.Location;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.configuration.serialization.ConfigurationSerialization;

import java.util.HashMap;
import java.util.Map;

import static com.meepcraft.mazegen.listeners.ListenerPlayerInteract.locString;

public class MazeData
{
    private Location firstCorner;
    private Location secondCorner;
    private Location top;

    private double interval=0;
    private BaseBlock wallBlock;
    private HashMap<MazePos, MazeCell> openings= new HashMap<>();;

    public MazeData() {}

    public MazeData(Map<String, Object> map)
    {
        firstCorner= (Location) map.getOrDefault("firstCorner", null);
        secondCorner= (Location) map.getOrDefault("secondCorner", null);
        top= (Location) map.getOrDefault("top", null);
        interval = (double) map.getOrDefault("interval", 0);
        wallBlock = (BaseBlock) map.getOrDefault("wallBlock", null);
        openings = (HashMap<MazePos, MazeCell>) map.getOrDefault("openings", null);
    }



    public MazeData(Location firstCorner, Location secondCorner, Location top, double interval, BaseBlock wallBlock, HashMap<MazePos, MazeCell> openings)
    {
        this.firstCorner = firstCorner;
        this.secondCorner = secondCorner;
        this.top = top;
        this.interval = interval;
        this.wallBlock = wallBlock;
        this.openings = openings;
    }

    public Location getTop()
    {
        return top;
    }

    public void setTop(Location top)
    {
        this.top = top;
    }

    public Location getFirstCorner()
    {
        return firstCorner;
    }

    public void setFirstCorner(Location firstCorner)
    {
        this.firstCorner = firstCorner;
    }

    public Location getSecondCorner()
    {
        return secondCorner;
    }

    public void setSecondCorner(Location secondCorner)
    {
        this.secondCorner = secondCorner;
    }

    public BaseBlock getWallBlock()
    {
        return wallBlock;
    }

    public void setWallBlock(BaseBlock wallBlock)
    {
        this.wallBlock = wallBlock;
    }

    public double getInterval()
    {
        return interval;
    }

    public void setInterval(double interval)
    {
        this.interval = interval;
    }

    public HashMap<MazePos, MazeCell> getOpenings()
    {
        return openings;
    }

    @Override
    public String toString()
    {
        return String.join("\n"+ Main.PREFIX,"World: "+firstCorner.getWorld().getName(),
                "Maze Floor: "+locString(firstCorner)+"-"+locString(secondCorner),
                "Walls: Y level: "+top.getBlockY()+", Block: "+wallBlock.toString(),
                "Interval: "+interval+" minutes",
                "Openings: "+openings.toString());
    }
}
