package com.meepcraft.mazegen.util;

public class MazePos
{
    public int col;
    public int row;

    public MazePos(int col, int row)
    {
        this.col = col;
        this.row = row;
    }

    @Override
    public int hashCode()
    {
        return col < row ? row * row + col : col * col + col + row;
    }

    @Override
    public boolean equals(Object other)
    {
        if(!(other instanceof MazePos)) return false;
        MazePos otherPos = (MazePos) other;
        return col==otherPos.col&&row==otherPos.row;
    }

    @Override
    public String toString()
    {
        return "("+col+","+row+")";
    }
}
