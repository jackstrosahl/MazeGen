package com.meepcraft.mazegen.types;

import com.meepcraft.mazegen.Main;
import org.bukkit.command.CommandSender;
import org.bukkit.scheduler.BukkitRunnable;

public class MazeTask extends BukkitRunnable
{
    private Main main;
    private MazeData data;
    private CommandSender creator;
    public MazeTask(Main main, MazeData data, CommandSender creator)
    {
        super();
        this.main=main;
        this.data=data;
        this.creator=creator;
    }

    @Override
    public void run()
    {
        GeneratingMaze generatingMaze = new GeneratingMaze(main, data, creator);
        generatingMaze.run();
    }
}
