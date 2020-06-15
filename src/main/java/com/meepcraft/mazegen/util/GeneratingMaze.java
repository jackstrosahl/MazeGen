package com.meepcraft.mazegen.util;

import com.meepcraft.mazegen.Main;
import com.sk89q.worldedit.BlockVector;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.MaxChangedBlocksException;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.bukkit.BukkitUtil;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldguard.bukkit.WGBukkit;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;

import java.security.InvalidParameterException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

public class GeneratingMaze
{
    private final int minX;
    private final int minZ;
    private final int maxX;
    private final int maxZ;
    private final int offsetX;
    private final int offsetZ;
    private final World world;
    private final int floorLevel;
    private final int topLevel;
    private final BaseBlock wallBlock;
    private final int wallSize;
    private final int pathSize;
    private final int cols;
    private final int rows;
    private HashSet<MazePos> unvisited;
    private MazeCell[][] cells;
    private HashMap<MazePos, MazeCell> openings;
    private SecureRandom random;
    private EditSession editSession;
    private CommandSender creator;
    private Main main;
    private String name;
    private boolean fromTimer;
    private double interval;
    public GeneratingMaze(Main main, MazeData data, CommandSender creator, String name, boolean fromTimer)
    {
        this.random = new SecureRandom();

        int firstX = data.getFirstCorner().getBlockX();
        int firstZ = data.getFirstCorner().getBlockZ();
        int secondX = data.getSecondCorner().getBlockX();
        int secondZ = data.getSecondCorner().getBlockZ();
        this.minX = Math.min(firstX,secondX);
        this.minZ = Math.min(firstZ,secondZ);
        this.maxX = Math.max(firstX,secondX);
        this.maxZ = Math.max(firstZ,secondZ);
        this.world = data.getFirstCorner().getWorld();
        this.floorLevel = data.getFirstCorner().getBlockY();
        this.topLevel = data.getTop().getBlockY();
        this.wallBlock = data.getWallBlock();
        //Wall Size is defined as 'radius' (not including center block)
        //Wall size of 0=1 Wide paths.
        //Wall size of 1=3 Wide Paths.
        this.pathSize =data.getPathSize();
        if(pathSize <0) throw new InvalidParameterException();
        this.wallSize = this.pathSize +1;

        this.cols = (int)((maxX-minX)/(wallSize *2.0));
        this.rows = (int)((maxZ-minZ)/(wallSize *2.0));
        this.offsetX = ((maxX-minX+1)-(cols* wallSize *2))/2;
        this.offsetZ = ((maxZ-minZ+1)-(rows* wallSize *2))/2;
        this.creator=creator;
        this.unvisited = new HashSet<>();
        this.main = main;
        this.openings = data.getOpenings();
        this.name = name;
        this.interval=data.getInterval();
        this.fromTimer=fromTimer;
    }

    public int xFromCol(int col)
    {
        return minX + offsetX + (col * wallSize * 2) + wallSize;
    }

    public int zFromRow(int row)
    {
        return minZ + offsetZ+ (row * wallSize * 2) + wallSize;
    }

    public MazePos posFromXZ(int x, int z)
    {
        int col= Math.round((x-minX- wallSize -offsetX)/(wallSize *2.0f));
        int row = Math.round((z-minZ- wallSize -offsetZ)/(wallSize *2.0f));
        if(col<-1||col>cols||row<-1||row>rows) throw new InvalidParameterException();
        return new MazePos(col, row);
    }

    public MazePos posFromXZ(Location loc)
    {
        return posFromXZ(loc.getBlockX(), loc.getBlockZ());
    }


    public void removeWalls()
    {
        RegionManager regions = WGBukkit.getRegionManager(world);
        editSession = WorldEdit.getInstance().getEditSessionFactory().
                getEditSession(BukkitUtil.getLocalWorld(world),-1);
        for(int col = 0;col<cols;col++)
        {
            for(int row = 0;row<rows;row++)
            {
                int ox = xFromCol(col);
                int oz = zFromRow(row);
                ProtectedRegion test = new ProtectedCuboidRegion("dummy",
                        new BlockVector(ox- wallSize,floorLevel,oz- wallSize),
                        new BlockVector(ox+ wallSize,topLevel, oz+ wallSize));
                boolean valid = true;
                ApplicableRegionSet set = regions.getApplicableRegions(test);
                if(set.testState(null, Main.MAZE_GEN_FLAG))
                {
                    setBlocks(new BlockVector(ox - wallSize, floorLevel + 1, oz - wallSize),
                            new BlockVector(ox + wallSize, topLevel, oz + wallSize),
                            true, new BaseBlock(0));
                }
            }
        }
        editSession.flushQueue();
        editSession.commit();
    }

    public boolean run()
    {
        editSession = WorldEdit.getInstance().getEditSessionFactory().
                getEditSession(BukkitUtil.getLocalWorld(world),-1);
        RegionManager regions = WGBukkit.getRegionManager(world);
        // Initialize Grid with MazeCells, respecting WG flag
        // Also fill unvisited with a MazePos for every cell
        cells = new MazeCell[cols][rows];
        HashSet<String> denyRegions = new HashSet<>();
        for(int col = 0;col<cols;col++)
        {
            for(int row = 0;row<rows;row++)
            {
                int ox = xFromCol(col);
                int oz = zFromRow(row);
                ProtectedRegion test = new ProtectedCuboidRegion("dummy",
                        new BlockVector(ox- wallSize,floorLevel,oz- wallSize),
                        new BlockVector(ox+ wallSize,topLevel, oz+ wallSize));
                boolean valid = true;
                ApplicableRegionSet set = regions.getApplicableRegions(test);
                for(ProtectedRegion r: set)
                {
                    if(r.getFlag(Main.MAZE_GEN_FLAG)== StateFlag.State.DENY)
                    {
                        valid = false;
                        denyRegions.add(r.getId());
                        break;
                    }
                }
                if(!valid)
                {
                    cells[col][row] =null;
                }
                else
                {
                    setBlocks(new BlockVector(ox- wallSize,floorLevel+1,oz- wallSize),
                            new BlockVector(ox+ wallSize,topLevel,oz+ wallSize), true);
                    cells[col][row] = new MazeCell();
                    unvisited.add(new MazePos(col, row));
                }
            }
        }
        String bisected = Main.PREFIX+"This maze cannot generate!  Some WG region(s) with flag "
                +Main.MAZE_GEN_FLAG.getName()+ ":DENY bisects it, meaning there is no path " +
                "to connect the entire maze together.  Potential Regions: "+String.join(",",denyRegions)+".";

        for(int col = 0;col<cols;col++)
        {
            int count =0;
            for (int row = 0; row < rows; row++)
            {
                if(cells[col][row]==null) count++;
            }
            if(count==rows)
            {
                sendMessage(bisected);
                return false;
            }
        }

        for (int row = 0; row < rows; row++)
        {
            int count =0;
            for(int col = 0;col<cols;col++)
            {
                if(cells[col][row]==null) count++;
            }
            if(count==cols)
            {
                sendMessage(bisected);
                return false;
            }
        }
        editSession.flushQueue();
        if(unvisited.size()==0) return false;
        unvisited.remove(unvisited.iterator().next());
        // Build the maze, modifying the MazeCells (Wilson's Algorithm)
        while(!unvisited.isEmpty())
        {
            ArrayList<MazePos> path = new ArrayList<>();
            MazePos cur = unvisited.iterator().next();
            path.add(cur);
            while(unvisited.contains(cur))
            {
                // Random Step, looping this creates a random walk
                cur = randomStep(cur);
                int index = path.indexOf(cur);
                if(index!=-1)
                {
                    // Erase the loop this step created
                    path.subList(index,path.size()).clear();
                }
                path.add(cur);
            }
            addPath(path);
        }
        MazeCell blank = new MazeCell();
        for(int col = 0;col<cols;col++)
        {
            for (int row = 0; row < rows; row++)
            {
                MazeCell cur = cells[col][row];
                if(cur==null) cur = blank;
                int ox = xFromCol(col);
                int oz = zFromRow(row);

                MazeCell override = openings.getOrDefault(new MazePos(col, row), blank);
                if(cur.isConnectedPosX()||override.isConnectedPosX())
                {
                    setBlocks(new BlockVector(ox+ wallSize,floorLevel+1,oz- pathSize),
                           new BlockVector(ox+ wallSize, topLevel,oz+ pathSize));
                }
                if(cur.isConnectedNegX()||override.isConnectedNegX())
                {
                    setBlocks(new BlockVector(ox- wallSize,floorLevel+1,oz- pathSize),
                            new BlockVector(ox- wallSize, topLevel,oz+ pathSize));
                }
                if(cur.isConnectedPosZ()||override.isConnectedPosZ())
                {
                    setBlocks(new BlockVector(ox- pathSize,floorLevel+1,oz+ wallSize),
                            new BlockVector(ox+ pathSize, topLevel,oz+ wallSize));
                }
                if(cur.isConnectedNegZ()||override.isConnectedNegZ())
                {
                    setBlocks(new BlockVector(ox- pathSize,floorLevel+1,oz- wallSize),
                            new BlockVector(ox+ pathSize, topLevel,oz- wallSize));
                }
            }
        }
        editSession.commit();
        String reason = fromTimer ? " after " + interval + " minutes (converted to ticks)." :
                ", this was triggered manually.";
        sendMessage(Main.PREFIX + name + " has regenerated" + reason);
        return true;
    }

    private void sendMessage(String message)
    {
        if(creator==null) return;
        creator.sendMessage(message);
    }

    private void setBlocks(BlockVector minPos, BlockVector maxPos)
    {
        setBlocks(minPos,maxPos,false);
    }

    public void setBlocks(BlockVector minPos, BlockVector maxPos, boolean walls)
    {
        setBlocks(minPos,maxPos,walls,wallBlock);
    }

    private void setBlocks(BlockVector minPos, BlockVector maxPos, boolean walls, BaseBlock setWalls)
    {
        CuboidRegion region = new CuboidRegion(minPos, maxPos);
        try
        {
            if(walls)
            {
                editSession.makeCuboidWalls(region, setWalls);
            }
            else
            {
                editSession.setBlocks(region, new BaseBlock(0));
                editSession.flushQueue();
            }
        }
        catch(MaxChangedBlocksException e)
        {
            System.out.println("MazeGen could not place some blocks.");
        }
    }

    private void addPath(List<MazePos> path)
    {
        for(int i=1;i<path.size();i++)
        {
            MazePos lastPos = path.get(i-1);
            MazeCell lastCell = cells[lastPos.col][lastPos.row];
            MazePos curPos = path.get(i);
            MazeCell curCell = cells[curPos.col][curPos.row];
            int diffCol = curPos.col-lastPos.col;
            int diffRow = curPos.row-lastPos.row;

            // Pos X
            if(diffCol==1)
            {
                lastCell.connectedPosX();
                curCell.connectedNegX();
            }
            // Neg X
            else if(diffCol==-1)
            {
                lastCell.connectedNegX();
                curCell.connectedPosX();
            }
            // Pos Z
            else if(diffRow==1)
            {
                lastCell.connectedPosZ();
                curCell.connectedNegZ();
            }
            // Neg Z
            else if(diffRow==-1)
            {
                lastCell.connectedNegZ();
                curCell.connectedPosZ();
            }
        }
        unvisited.removeAll(path);
    }

    private MazePos randomStep(MazePos cur)
    {
        int dir = random.nextInt(4);
        int col = cur.col;
        int row = cur.row;

        switch(dir)
        {
            // Pos X
            case 0:
                col++;
                break;
            // Neg Z
            case 1:
                row--;
                break;
            // Neg X
            case 2:
                col--;
                break;
            // Pos Z
            case 3:
                row++;
                break;
        }
        boolean valid = true;
        if(col<0||col>=cols||row<0||row>=rows) valid = false;
        else if(cells[col][row]==null) valid = false;
        if(!valid) return randomStep(cur);

        return new MazePos(col, row);
    }
}
