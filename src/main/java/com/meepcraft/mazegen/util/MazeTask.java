package com.meepcraft.mazegen.util;

import com.meepcraft.mazegen.Main;
import org.bukkit.command.CommandSender;
import org.bukkit.scheduler.BukkitRunnable;

public class MazeTask extends BukkitRunnable
{
    private Main main;
    private MazeData data;
    private CommandSender creator;
    private String name;
    public MazeTask(Main main, MazeData data, CommandSender creator, String name)
    {
        super();
        this.main=main;
        this.data=data;
        this.creator=creator;
        this.name = name;
    }

    @Override
    public void run()
    {
        GeneratingMaze generatingMaze = new GeneratingMaze(main, data, creator,name,true);
        generatingMaze.run();
    }
}
