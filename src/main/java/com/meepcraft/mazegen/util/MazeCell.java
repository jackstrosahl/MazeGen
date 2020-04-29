package com.meepcraft.mazegen.util;

import java.util.Map;

public class MazeCell
{
    private boolean connectedPosX, connectedNegX,connectedPosZ,connectedNegZ;

    public MazeCell(Map<String, Object> map)
    {
        connectedPosX=(boolean)map.get("connectedPosX");
        connectedNegX=(boolean)map.get("connectedNegX");
        connectedPosZ=(boolean)map.get("connectedPosZ");
        connectedNegZ=(boolean)map.get("connectedNegZ");
    }

    public MazeCell()
    {
        connectedPosX=false;
        connectedNegX=false;
        connectedPosZ=false;
        connectedNegZ=false;
    }

    public boolean isConnectedPosX()
    {
        return connectedPosX;
    }

    public void connectedPosX()
    {
        this.connectedPosX = true;
    }

    public boolean isConnectedNegX()
    {
        return connectedNegX;
    }

    public void connectedNegX()
    {
        this.connectedNegX = true;
    }

    public boolean isConnectedPosZ()
    {
        return connectedPosZ;
    }

    public void connectedPosZ()
    {
        this.connectedPosZ = true;
    }

    public boolean isConnectedNegZ()
    {
        return connectedNegZ;
    }

    public void connectedNegZ()
    {
        this.connectedNegZ = true;
    }

    public void booleanOr(MazeCell other)
    {
        if(other.isConnectedPosX()) connectedPosX();
        if(other.isConnectedNegX()) connectedNegX();
        if(other.isConnectedPosZ()) connectedPosZ();
        if(other.isConnectedNegZ()) connectedNegZ();
    }

    @Override
    public String toString()
    {
        return "MazeCell{" +
                "connectedPosX=" + connectedPosX +
                ", connectedNegX=" + connectedNegX +
                ", connectedPosZ=" + connectedPosZ +
                ", connectedNegZ=" + connectedNegZ +
                '}';
    }
}
