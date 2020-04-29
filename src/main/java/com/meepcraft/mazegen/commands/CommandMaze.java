package com.meepcraft.mazegen.commands;

import com.meepcraft.mazegen.Main;
import com.meepcraft.mazegen.types.GeneratingMaze;
import com.meepcraft.mazegen.types.MazeData;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CommandMaze implements CommandExecutor
{
    private Main main;

    public CommandMaze(Main main)
    {
        this.main = main;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args)
    {
        if (!(sender instanceof Player))
        {
            sender.sendMessage(Main.PREFIX + "This command can only be used as a player.");
            return false;
        }
        if (!sender.hasPermission("mazegen.admin")) {
            sender.sendMessage(Main.PREFIX + "The permssion 'mazegen.admin' is required to use this command.");
        }
        Player player = (Player) sender;
        switch(args[0].toLowerCase())
        {
            case "create":
                player.sendMessage(Main.PREFIX+"Maze creation started!");
                player.sendMessage(Main.PREFIX+"Click the first corner of the maze's floor.");
                main.mazesInProgress.put(player.getUniqueId(), new MazeData());
                break;
            case "save":
                if(args.length<2)
                {
                    player.sendMessage(Main.PREFIX+"Please specify a name for this maze:  /maze save <name>.");
                    return true;
                }
                if(main.mazesInProgress.get(player.getUniqueId()).getTop()==null)
                {
                    player.sendMessage(Main.PREFIX+"The maze you are creating is not finished, you can however run" +
                            "/maze cancel.");
                }
                player.sendMessage(Main.PREFIX+"Saved "+args[1]+".  You can generate it now with: /maze gen "+args[1]+"," +
                        " or set a timer with /maze interval " +args[1]+" <time in minutes>.");
                main.mazes.put(args[1],main.mazesInProgress.remove(player.getUniqueId()));
                main.saveData();
                break;
            case "interval":
            { // Braces to scope variables
                if (args.length < 3)
                {
                    player.sendMessage(Main.PREFIX + "Please specify a maze and interval : " +
                            "/maze interval <name> <interval>");
                    return true;
                }
                MazeData data = main.mazes.get(args[1]);
                if (data == null)
                {
                    player.sendMessage(Main.PREFIX + "Sorry, there is no maze under that name.");
                    return true;
                }
                double interval = 0;
                try
                {
                    interval = Double.parseDouble(args[2]);
                    if (interval < 0) throw new NumberFormatException();
                } catch (NumberFormatException e)
                {
                    player.sendMessage(Main.PREFIX + "That interval is incorrect.");
                    return true;
                }
                data.setInterval(interval);
                if (interval == 0)
                {
                    player.sendMessage(Main.PREFIX + args[1] + " has been disabled.");
                } else
                {
                    player.sendMessage(Main.PREFIX + args[1] + " will regenerate every " + interval + " minutes.");
                }
                main.makeGenTimer(args[1]);
                break;
            }
            case "cancel":
                player.sendMessage(Main.PREFIX+"Maze creation cancelled.");
                main.mazesInProgress.remove(player.getUniqueId());
                break;
            case "gen":
            {
                if (args.length < 2)
                {
                    player.sendMessage(Main.PREFIX + "Please specify a maze to generate: /maze gen <name>");
                    return true;
                }
                MazeData data = main.mazes.get(args[1]);
                if (data == null)
                {
                    player.sendMessage(Main.PREFIX + "Sorry, there is no maze under that name.");
                    return true;
                }
                player.sendMessage(Main.PREFIX + "Generating " + args[1] + ".");
                GeneratingMaze generatingMaze = new GeneratingMaze(main, data, sender);
                generatingMaze.run();
                break;
            }
            default:
            {
                MazeData data = main.mazes.get(args[0]);
                if(data==null)
                {
                    player.sendMessage(Main.PREFIX+args[0]+" isn't recognized as a maze or subcommand.");
                }
                player.sendMessage(data.toString());
                break;
            }

        }

        return true;
    }
}
