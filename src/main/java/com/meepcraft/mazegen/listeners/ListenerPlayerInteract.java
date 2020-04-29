package com.meepcraft.mazegen.listeners;

import com.meepcraft.mazegen.Main;
import com.meepcraft.mazegen.util.GeneratingMaze;
import com.meepcraft.mazegen.util.MazeCell;
import com.meepcraft.mazegen.util.MazeData;
import com.meepcraft.mazegen.util.MazePos;
import com.sk89q.worldedit.BlockVector;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.bukkit.BukkitUtil;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

import java.security.InvalidParameterException;
import java.util.HashMap;

public class ListenerPlayerInteract implements Listener
{
    private Main main;

    public ListenerPlayerInteract(Main main)
    {
        this.main = main;
    }

    public static String locString(Location loc)
    {
        return "("+String.join(",",
                loc.getBlockX() + "", loc.getBlockY() + "", loc.getBlockZ() + "")+")";
    }
    private ChatColor highlight = ChatColor.GREEN;
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent e)
    {
        if (e.getAction() != Action.RIGHT_CLICK_BLOCK&&e.getAction()!=Action.LEFT_CLICK_BLOCK) return;
        Player player = e.getPlayer();
        MazeData data = main.mazesInProgress.get(player.getUniqueId());
        if (data == null) return;
        e.setCancelled(true);
        if (data.getFirstCorner() == null)
        {
            Location loc = e.getClickedBlock().getLocation();
            player.sendMessage(Main.PREFIX + "Set first corner of the maze floor to " + locString(loc) + ".");
            player.sendMessage(Main.PREFIX +highlight+"Next, you can click the opposite corner of the maze-floor.");
            data.setFirstCorner(loc);
        }
        else if (data.getSecondCorner() == null)
        {
            Location firstCorner = data.getFirstCorner();
            Location loc = e.getClickedBlock().getLocation();
            if (!loc.getWorld().equals(firstCorner.getWorld()))
            {
                player.sendMessage(Main.PREFIX + "Mazes cannot span worlds!  " +
                        "This maze is in " + firstCorner.getWorld().getName() + ".");
                return;
            }
            int yLevel = loc.getBlockY();
            if (yLevel != firstCorner.getBlockY())
            {
                player.sendMessage(Main.PREFIX + "Maze floors must be one block thick!  " +
                        "The floor is at Y level " + firstCorner.getBlockY() + ".");
                return;
            }
            player.sendMessage(Main.PREFIX + "Set second corner of maze to " + locString(loc) + ".");
            player.sendMessage(Main.PREFIX+highlight+"Next, click a block that represents the Y level of the top of " +
                    "the maze, and what block the maze walls are.");
            player.sendMessage(Main.PREFIX+ ChatColor.RED+"NOTE: This will cause the maze to generate.  " +
                    "Ensure you have WG regions defined with maze-gen:DENY inside the maze if necessary.");
            data.setSecondCorner(loc);
        }
        else if(data.getTop()==null)
        {
            Block clicked = e.getClickedBlock();
            Location loc = clicked.getLocation();
            Location firstCorner = data.getFirstCorner();
            if (!loc.getWorld().equals(firstCorner.getWorld()))
            {
                player.sendMessage(Main.PREFIX + "Mazes cannot span worlds!  " +
                        "This maze is in " + firstCorner.getWorld().getName() + ".");
                return;
            }
            else if (loc.getBlockY() <= firstCorner.getBlockY())
            {
                player.sendMessage(Main.PREFIX + "The top of the maze must be above the floor!  " +
                        "The floor is at Y level " + firstCorner.getBlockY() + ".");
                return;
            }
            BaseBlock baseBlock = BukkitUtil.getLocalWorld(firstCorner.getWorld())
                    .getBlock(new BlockVector(loc.getBlockX(),loc.getBlockY(), loc.getBlockZ()));
            data.setWallBlock(baseBlock);
            data.setTop(loc);
            player.sendMessage(Main.PREFIX + "Set the maze wall to " + baseBlock.toString()
                    + " at Y level " + loc.getBlockY() + ".  ");

            GeneratingMaze generatingMaze = new GeneratingMaze(main, data, null,
                    "", false);
            generatingMaze.run();
            player.sendMessage(Main.PREFIX+highlight+"Next, you can specify entrances/exits by clicking" +
                    " on a wall that should not generate.");
        }
        else
        {
            HashMap<MazePos, MazeCell> openings = data.getOpenings();
            int x = e.getClickedBlock().getX();
            int z = e.getClickedBlock().getZ();
            // Useful to get coords from, also regenerate when we specify an opening
            GeneratingMaze generatingMaze = new GeneratingMaze(main, data,
                    null,"", false);
            MazePos lastPos;
            try
            {
                lastPos = generatingMaze.posFromXZ(x,z);
            }
            catch(InvalidParameterException err)
            {
                player.sendMessage(Main.PREFIX+"You cannot select a block outside the maze.");
                return;
            }
            int ox = generatingMaze.xFromCol(lastPos.col);
            int oz = generatingMaze.zFromRow(lastPos.row);
            int diffX = x-ox;
            int diffZ = z-oz;
            int absX = Math.abs(diffX);
            int absZ = Math.abs(diffZ);
            MazeCell lastCell = new MazeCell();
            MazeCell curCell = new MazeCell();
            int curCol = lastPos.col;
            int curRow = lastPos.row;
            if(absZ<absX)
            {
                // Pos X
                if(diffX>0)
                {
                    lastCell.connectedPosX();
                    curCell.connectedNegX();
                    curCol++;
                }
                // Neg X
                else if(diffX<0)
                {
                    lastCell.connectedNegX();
                    curCell.connectedPosX();
                    curCol--;
                }
            }
            else if(absX<absZ)
            {
                // Pos Z
                if(diffZ>0)
                {
                    lastCell.connectedPosZ();
                    curCell.connectedNegZ();
                    curRow++;
                }
                // Neg Z
                else if(diffZ<0)
                {
                    lastCell.connectedNegZ();
                    curCell.connectedPosZ();
                    curRow--;
                }
            }
            else
            {
                player.sendMessage(Main.PREFIX+"Unable to determine what wall you've selected, please try again.");
                return;
            }
            MazeCell lastCellToSet = openings.getOrDefault(lastPos, new MazeCell());
            lastCellToSet.booleanOr(lastCell);
            openings.put(lastPos, lastCellToSet);
            MazePos nextPos = new MazePos(curCol,curRow);
            MazeCell curCellToSet = openings.getOrDefault(nextPos, new MazeCell());
            curCellToSet.booleanOr(curCell);
            openings.put(nextPos, curCellToSet);
            player.sendMessage(Main.PREFIX+"Added opening, regenerating.  You can finish anytime by running " +
                    "/maze save <name>.  If you're editing this maze, instead run /maze save.");
            generatingMaze.run();
        }
    }
}
