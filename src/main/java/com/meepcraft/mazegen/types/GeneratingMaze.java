package com.meepcraft.mazegen.types;

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
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.scheduler.BukkitRunnable;

import java.security.InvalidParameterException;
import java.security.SecureRandom;
import java.util.ArrayList;
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
    private final int pathSize;
    private final int wallSize;
    private final int cols;
    private final int rows;
    private HashSet<MazePos> unvisited;
    private MazeCell[][] cells;
    private SecureRandom random;
    private EditSession editSession;
    private CommandSender creator;
    private Main main;
    public GeneratingMaze(Main main, MazeData data, CommandSender creator)
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
        //Path Size is defined as 'radius'
        //Path size of 1=1 Wide paths.
        //Path size of 2=3 Wide Paths.
        this.pathSize = 2;
        if(pathSize<1) throw new InvalidParameterException();
        this.wallSize=pathSize-1;
        this.cols = (int)((maxX-minX)/(pathSize*2.0));
        this.rows = (int)((maxZ-minZ)/(pathSize*2.0));
        this.offsetX = ((maxX-minX+1)-(cols*pathSize*2))/2;
        this.offsetZ = ((maxZ-minZ+1)-(rows*pathSize*2))/2;
        this.creator=creator;
        this.unvisited = new HashSet<>();
        this.main = main;
    }

    public int xFromCol(int col)
    {
        return minX + offsetX + (col * pathSize * 2) + pathSize;
    }

    public int zFromRow(int row)
    {
        return minZ + offsetZ+ (row * pathSize * 2) + pathSize;
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
                        new BlockVector(ox-pathSize,floorLevel,oz-pathSize),
                        new BlockVector(ox+pathSize,topLevel, oz+pathSize));
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
                    setBlocks(new BlockVector(ox-pathSize,floorLevel+1,oz-pathSize),
                            new BlockVector(ox+pathSize,topLevel,oz+pathSize), true);
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
                creator.sendMessage(bisected);
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
                creator.sendMessage(bisected);
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
        for(int col = 0;col<cols;col++)
        {
            for (int row = 0; row < rows; row++)
            {
                MazeCell cur = cells[col][row];
                if(cur==null) continue;
                int ox = xFromCol(col);
                int oz = zFromRow(row);

                if(cur.isConnectedPosX())
                {
                    setBlocks(new BlockVector(ox+pathSize,floorLevel+1,oz-wallSize),
                           new BlockVector(ox+pathSize, topLevel,oz+wallSize));
                }
                if(cur.isConnectedNegX())
                {
                    setBlocks(new BlockVector(ox-pathSize,floorLevel+1,oz-wallSize),
                            new BlockVector(ox-pathSize, topLevel,oz+wallSize));
                }
                if(cur.isConnectedPosZ())
                {
                    setBlocks(new BlockVector(ox-wallSize,floorLevel+1,oz+pathSize),
                            new BlockVector(ox+wallSize, topLevel,oz+pathSize));
                }
                if(cur.isConnectedNegZ())
                {
                    setBlocks(new BlockVector(ox-wallSize,floorLevel+1,oz-pathSize),
                            new BlockVector(ox+wallSize, topLevel,oz-pathSize));
                }
            }
        }
        editSession.commit();
        return true;
    }

    private void setBlocks(BlockVector minPos, BlockVector maxPos)
    {
        setBlocks(minPos,maxPos,false);
    }

    private void setBlocks(BlockVector minPos, BlockVector maxPos, boolean walls)
    {
        CuboidRegion region = new CuboidRegion(minPos, maxPos);
        try
        {
            if(walls) {
            editSession.makeCuboidWalls(region, wallBlock);
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
//        String out="";
//        for(MazePos pos:path)
//        {
//            out+=pos.toString();
//        }
//        out+="\n";
        for(int i=1;i<path.size();i++)
        {
            MazePos lastPos = path.get(i-1);
            MazeCell lastCell = cells[lastPos.col][lastPos.row];
            MazePos curPos = path.get(i);
            MazeCell curCell = cells[curPos.col][curPos.row];
            int diffCol = curPos.col-lastPos.col;
            int diffRow = curPos.row-lastPos.row;

            //out+=",";
            // Pos X
            if(diffCol==1)
            {
                lastCell.connectedPosX();
                curCell.connectedNegX();
                //out+="Pos X";
            }
            // Neg X
            else if(diffCol==-1)
            {
                lastCell.connectedNegX();
                curCell.connectedPosX();
                //out+="Neg X";
            }
            // Pos Z
            else if(diffRow==1)
            {
                lastCell.connectedPosZ();
                curCell.connectedNegZ();
                //out+="Pos Z";
            }
            // Neg Z
            else if(diffRow==-1)
            {
                lastCell.connectedNegZ();
                curCell.connectedPosZ();
                //out+="Neg Z";
            }
        }
        //System.out.println(out);
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
