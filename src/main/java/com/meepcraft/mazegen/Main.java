package com.meepcraft.mazegen;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.meepcraft.mazegen.commands.CommandMaze;
import com.meepcraft.mazegen.listeners.ListenerPlayerInteract;
import com.meepcraft.mazegen.util.*;
import com.meepcraft.mazegen.util.json.MapJson;
import com.meepcraft.mazegen.util.json.YamlInJson;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldguard.bukkit.WGBukkit;
import com.sk89q.worldguard.protection.flags.Flag;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.flags.registry.FlagConflictException;
import com.sk89q.worldguard.protection.flags.registry.FlagRegistry;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.*;

public class Main extends JavaPlugin
{
    public static final String PREFIX = ChatColor.DARK_RED + "[MazeGen] "+ChatColor.DARK_AQUA;

    public HashMap<String, MazeData> mazes;
    public HashMap<UUID, MazeData> mazesInProgress;

    public HashMap<String, Integer> tasks;

    public static String DATA_PATH = "data.json";
    public static StateFlag MAZE_GEN_FLAG;
    GsonBuilder builder;
    @Override
    public void onEnable()
    {
        // Initialize Fields
        builder = new GsonBuilder();
        builder.registerTypeAdapter(Location.class, new YamlInJson());
        builder.registerTypeAdapter(BaseBlock.class, new YamlInJson());
        builder.registerTypeAdapter(new TypeToken<HashMap<MazePos, MazeCell>>(){}.getType(), new MapJson());
        mazesInProgress = new HashMap<>();
        tasks = new HashMap<>();
        loadData();
        if(mazes==null)
        {
            getLogger().info(PREFIX+"Data file was invalid or empty.");
            mazes = new HashMap<>();
        }
        scheduleTasks(true);
        getCommand("maze").setExecutor(new CommandMaze(this));
        getServer().getPluginManager().registerEvents(new ListenerPlayerInteract(this), this);
    }

    @Override
    public void onLoad()
    {
        FlagRegistry registry = WGBukkit.getPlugin().getFlagRegistry();
        // The plugin will not load if this flag is taken.
        String flagName = "maze-gen";
        StateFlag flag = new StateFlag(flagName, true);
        try
        {
            registry.register(flag);
            MAZE_GEN_FLAG = flag;
        }
        catch(IllegalStateException e)
        {
            Flag<?> existing = registry.get(flagName);
            if (existing instanceof StateFlag) {
                MAZE_GEN_FLAG = (StateFlag) existing;
            } else {
                throw new FlagConflictException("Flag "+flagName+" already taken, we cannot load!");
            }
        }
    }

    public void loadData()
    {
        Gson gson = builder.create();
        File dataFile = new File(getDataFolder(), DATA_PATH);
        try
        {
            saveResource(DATA_PATH, false);
            FileReader reader = new FileReader(dataFile);
            mazes = gson.fromJson(reader, new TypeToken<HashMap<String, MazeData>>(){}.getType());
            reader.close();
        }
        catch(Exception e)
        {
            getLogger().info("Failed to load data, error follows.");
            e.printStackTrace();
        }
    }

    public void saveData()
    {
        File dataFile = new File(getDataFolder(), DATA_PATH);
        Gson gson = builder.create();
        if (!dataFile.exists()) {
            dataFile.getParentFile().mkdirs();
        }
        try
        {
            saveResource(DATA_PATH, true);
            FileWriter writer = new FileWriter(dataFile);
            gson.toJson(mazes, writer);
            writer.close();
        }
        catch(Exception e)
        {
            getLogger().info("Failed to save data, error follows.");
            e.printStackTrace();
        }
    }

    public void scheduleTasks(boolean regen)
    {
        for(String name:mazes.keySet())
        {
            makeGenTimer(name, regen);
        }
    }

    public void makeGenTimer(String name, boolean regen)
    {
        cancelTask(name);
        if(mazes.containsKey(name))
        {
            MazeData data = mazes.get(name);
            if(data.getInterval()==0) return;
            MazeTask task = new MazeTask(this, data, getServer().getConsoleSender(), name);
            long ticks = (long)(data.getInterval()*60*20);
            long delay = regen?0:ticks;
            task.runTaskTimer(this, delay, ticks);
            tasks.put(name, task.getTaskId());
        }
    }

    public void cancelTask(String name)
    {
        if(tasks.containsKey(name))
        {
            getServer().getScheduler().cancelTask(tasks.get(name));
            tasks.remove(name);
        }
    }
}
