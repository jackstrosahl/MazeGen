package com.meepcraft.mazegen.commands;

import com.meepcraft.mazegen.Main;
import com.meepcraft.mazegen.util.GeneratingMaze;
import com.meepcraft.mazegen.util.MazeData;
import com.sun.tools.javac.jvm.Gen;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.UUID;

public class CommandMaze implements CommandExecutor
{
    private Main main;
    private final LinkedHashMap<String, String> helpStrings = new LinkedHashMap<>();
    private final String wrongSender = Main.PREFIX + "This command can only be used as a player.";
    private final HashMap<UUID, String> editing = new HashMap<>();
    public CommandMaze(Main main)
    {
        this.main = main;
        helpStrings.put("create", ": Starts the creation of a maze.  Before running this command, you should have " +
                "built the maze's floor, and a single pillar that represents the maze wall's height and block.  " +
                "Additionally, have marked any non-maze areas within the selection with the WG flag 'maze-gen':DENY.");
        helpStrings.put("cancel", ": Cancel's creating a maze (/maze create).");
        helpStrings.put("save", " [name]: Saves the maze currently being created (/maze create) under [name].  " +
                "When called without a maze name, will save changes to a maze being edited.");
        helpStrings.put("delete", " <maze>: Deletes <maze>, cancels regeneration, and removes walls.  Call this " +
                "BEFORE removing any WG regions with flag 'maze-gen':DENY in this maze.");
        helpStrings.put("interval"," <maze> <interval>: Sets <maze> to regenerate every <interval> minute(s).");
        helpStrings.put("pathsize", " <maze> <pathsize>: Sets <maze> to have paths with radius <pathsize>.");
        helpStrings.put("gen"," <maze>: Regenerates <maze> immediately.");
        helpStrings.put("list",": lists all mazes.");
        helpStrings.put("<maze>",": Displays information on <maze>.");
    }

    public String getHelpString(String sub)
    {
        String help = helpStrings.get(sub);
        if(help!=null)
        {
            return Main.PREFIX+"/maze "+sub+help;
        }
        else
        {
            ArrayList<String> out = new ArrayList<>();
            for(String key: helpStrings.keySet())
                out.add(getHelpString(key));
            return String.join("\n", out);
        }
    }

    private boolean checkPlayer(Player player, CommandSender sender)
    {
        if(player==null)
        {
            sender.sendMessage(wrongSender);
            return false;
        }
        return true;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args)
    {
        // Shouldn't actually happen as plugin.yml links this permission to the command.
        if (!sender.hasPermission("mazegen.admin")) {
            sender.sendMessage(Main.PREFIX + "The permssion 'mazegen.admin' is required to use this command.");
        }
        Player player = sender instanceof Player ? (Player) sender:null;
        if(args.length==0)
        {
            sender.sendMessage(getHelpString(""));
            return true;
        }
        switch(args[0].toLowerCase())
        {
            case "create":
                if(!checkPlayer(player, sender)) return true;
                main.mazesInProgress.put(player.getUniqueId(), new MazeData());
                player.sendMessage(Main.PREFIX+"Maze creation started!");
                player.sendMessage(Main.PREFIX+"Click the first corner of the maze's floor.");
                break;
            case "save":
                if(!checkPlayer(player, sender)) return true;
                if(args.length<2)
                {
                    String name = editing.remove(player.getUniqueId());
                    if(name!=null)
                    {
                        main.mazes.put(name, main.mazesInProgress.remove(player.getUniqueId()));
                        main.saveData();
                        player.sendMessage(Main.PREFIX+"Saved changes to "+name+".");
                        return true;
                    }
                    else
                    {
                        player.sendMessage(Main.PREFIX + "Please specify a name for this maze:  /maze save <name>.");
                        return true;
                    }
                }
                if(!main.mazesInProgress.containsKey(player.getUniqueId()))
                {
                    player.sendMessage(Main.PREFIX+"You are not creating or editing a maze!  /maze create");
                }
                if(main.mazesInProgress.get(player.getUniqueId()).getTop()==null)
                {
                    player.sendMessage(Main.PREFIX+"The maze you are creating is not finished, you can however run" +
                            "/maze cancel.");
                    return true;
                }
                if(main.mazes.containsKey(args[1]))
                {
                    player.sendMessage(Main.PREFIX+"A maze already exists under that name, try another.");
                    return true;
                }
                main.mazes.put(args[1],main.mazesInProgress.remove(player.getUniqueId()));
                main.saveData();
                player.sendMessage(Main.PREFIX+"Saved "+args[1]+".  You can generate it now with: /maze gen "+args[1]+"," +
                        " or set a timer with /maze interval " +args[1]+" <time in minutes>.");
                break;
            case "delete":
            {
                if (args.length < 2)
                {
                    sender.sendMessage(Main.PREFIX + "Please specify a name for the maze:  /maze delete <name>.");
                    return true;
                }
                if (!main.mazes.containsKey(args[1]))
                {
                    sender.sendMessage(Main.PREFIX + "Sorry, there is no maze under that name.");
                    return true;
                }
                // Not used to generate, but remove walls
                GeneratingMaze generatingMaze = new GeneratingMaze(main, main.mazes.get(args[1]),
                        null, "", false);
                generatingMaze.removeWalls();
                main.cancelTask(args[1]);
                main.mazes.remove(args[1]);
                main.saveData();
                sender.sendMessage(Main.PREFIX + args[1] + " has been deleted.");
                break;
            }
            case "interval":
            { // Braces to scope variables
                if (args.length < 3)
                {
                    sender.sendMessage(Main.PREFIX + "Please specify a maze and interval : " +
                            "/maze interval <name> <interval>");
                    return true;
                }
                MazeData data = main.mazes.get(args[1]);
                if (data == null)
                {
                    sender.sendMessage(Main.PREFIX + "Sorry, there is no maze under that name.");
                    return true;
                }
                double interval = 0;
                try
                {
                    interval = Double.parseDouble(args[2]);
                    if (interval < 0) throw new NumberFormatException();
                } catch (NumberFormatException e)
                {
                    sender.sendMessage(Main.PREFIX + "That interval is incorrect.");
                    return true;
                }
                data.setInterval(interval);
                main.saveData();
                main.makeGenTimer(args[1], false);
                if (interval == 0)
                {
                    sender.sendMessage(Main.PREFIX + args[1] + " will not regenerate on an interval.");
                } else
                {
                    sender.sendMessage(Main.PREFIX + args[1] + " will regenerate every " + interval + " minutes.");
                }
                break;
            }
            case "cancel":
            {
                if (!checkPlayer(player, sender)) return true;
                if (!main.mazesInProgress.containsKey(player.getUniqueId()))
                {
                    player.sendMessage(Main.PREFIX + "You are not currently creating or editing a maze!  /maze create");
                    return true;
                }
                MazeData data = main.mazesInProgress.remove(player.getUniqueId());
                editing.remove(player.getUniqueId());
                if (data.getTop() != null)
                {
                    GeneratingMaze generatingMaze = new GeneratingMaze(main, data, null,
                            "", false);
                    generatingMaze.removeWalls();
                }
                player.sendMessage(Main.PREFIX + "Maze creation/edit cancelled.");
                break;
            }
            case "list":
                if(main.mazes.size()==0)
                    sender.sendMessage(Main.PREFIX+"No mazes exist!  Create one with /maze create.");
                else
                    sender.sendMessage(Main.PREFIX+"The following mazes exist: "+String.join(", ",main.mazes.keySet())+".");
                break;
            case "gen":
            {
                if (args.length < 2)
                {
                    sender.sendMessage(Main.PREFIX + "Please specify a maze to generate: /maze gen <name>");
                    return true;
                }
                MazeData data = main.mazes.get(args[1]);
                if (data == null)
                {
                    sender.sendMessage(Main.PREFIX + "Sorry, there is no maze under that name.");
                    return true;
                }
                GeneratingMaze generatingMaze = new GeneratingMaze(main, data, sender, args[1], false);
                generatingMaze.run();
                break;
            }
            case "openings":
            {
                if(!checkPlayer(player, sender)) return true;
                if (args.length < 2)
                {
                    player.sendMessage(Main.PREFIX + "Please specify a maze: " +
                            "/maze openings <maze>.");
                    return true;
                }
                MazeData data = main.mazes.get(args[1]);
                if (data == null)
                {
                    player.sendMessage(Main.PREFIX + "Sorry, there is no maze under that name.");
                    return true;
                }
                data.getOpenings().clear();
                GeneratingMaze generatingMaze = new GeneratingMaze(main, data, null,
                        "", false);
                generatingMaze.run();
                main.mazesInProgress.put(player.getUniqueId(), data);
                editing.put(player.getUniqueId(), args[1]);
                player.sendMessage(Main.PREFIX+"Openings removed, you can now specify new ones by clicking" +
                        " on walls that should not generate.");
                break;
            }
            case "pathsize":
            {
                if (args.length < 3)
                {
                    sender.sendMessage(Main.PREFIX + "Please specify a maze and path size : " +
                            "/maze pathsize <name> <path size>");
                    return true;
                }
                MazeData data = main.mazes.get(args[1]);
                if (data == null)
                {
                    sender.sendMessage(Main.PREFIX + "Sorry, there is no maze under that name.");
                    return true;
                }
                int pathSize;
                try
                {
                    pathSize = Integer.parseInt(args[2]);
                    if (pathSize < 0) throw new NumberFormatException();
                } catch (NumberFormatException e)
                {
                    sender.sendMessage(Main.PREFIX + "That path size is incorrect.");
                    return true;
                }
                // Remove old walls
                GeneratingMaze generatingMaze = new GeneratingMaze(main, data,
                        null, "", false);
                generatingMaze.removeWalls();

                // Set new path size
                data.setPathSize(pathSize);
                data.getOpenings().clear();

                // Generate on new size
                generatingMaze = new GeneratingMaze(main, data,
                        null, "", false);
                generatingMaze.run();

                main.saveData();

                sender.sendMessage(Main.PREFIX+args[1]+"'s paths now have a radius of "+pathSize);
                sender.sendMessage(Main.PREFIX+"Its openings have been reset.  Run /maze openings "+args[1]+" again to " +
                        "set them.");
                break;
            }
            default:
            {
                MazeData data = main.mazes.get(args[0]);
                if(data==null)
                {
                    sender.sendMessage(Main.PREFIX+args[0]+" isn't recognized as a maze or subcommand.");
                    return true;
                }
                sender.sendMessage(data.toString());
                break;
            }

        }

        return true;
    }
}
