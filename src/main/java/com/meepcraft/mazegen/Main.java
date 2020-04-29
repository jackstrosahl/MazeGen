package com.meepcraft.mazegen;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.meepcraft.mazegen.commands.CommandMaze;
import com.meepcraft.mazegen.listeners.ListenerPlayerInteract;
import com.meepcraft.mazegen.types.GeneratingMaze;
import com.meepcraft.mazegen.types.MazeData;
import com.meepcraft.mazegen.types.MazeTask;
import com.meepcraft.mazegen.types.YamlInJson;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldguard.bukkit.WGBukkit;
import com.sk89q.worldguard.protection.flags.StateFlag;
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
    public static final String PREFIX = ChatColor.RED + "[MazeGen] "+ChatColor.GREEN;

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
        mazesInProgress = new HashMap<>();
        tasks = new HashMap<>();
        loadData();
        if(mazes==null)
        {
            getLogger().info(PREFIX+"Data file was invalid.");
            mazes = new HashMap<>();
        }
        scheduleTasks();
        getCommand("maze").setExecutor(new CommandMaze(this));
        getServer().getPluginManager().registerEvents(new ListenerPlayerInteract(this), this);
    }

    @Override
    public void onLoad()
    {
        FlagRegistry registry = WGBukkit.getPlugin().getFlagRegistry();
        // The plugin will not load if this flag is taken.
        StateFlag flag = new StateFlag("maze-gen", true);
        registry.register(flag);
        MAZE_GEN_FLAG = flag;
    }

    public void loadData()
    {
        Gson gson = builder.create();
        File dataFile = new File(getDataFolder(), DATA_PATH);
        try
        {
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

    public void scheduleTasks()
    {
        for(String name:mazes.keySet())
        {
            makeGenTimer(name);
        }
    }

    public void makeGenTimer(String name)
    {
        cancelTask(name);
        if(mazes.containsKey(name))
        {
            MazeData data = mazes.get(name);
            if(data.getInterval()==0) return;
            MazeTask task = new MazeTask(this, data, getServer().getConsoleSender());
            task.runTaskTimer(this, 0, (long)(data.getInterval()*60*20));
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
