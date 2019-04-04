package com.vberlier.settlements.generator;

import com.vberlier.settlements.util.Vec;
import net.minecraft.util.math.BlockPos;

public class CoordinatesInfo {
    public final int i;
    public final int j;
    private BlockPos highestBlock;
    private BlockPos terrainBlock;
    private boolean containsLiquids = false;
    private Vec normal;
    private double distanceFromCenter = 0;

    public CoordinatesInfo(int i, int j, BlockPos highestBlock) {
        this.i = i;
        this.j = j;
        this.highestBlock = highestBlock;
        this.terrainBlock = highestBlock;
        normal = Vec.up;
    }

    public void setContainsLiquids(boolean containsLiquids) {
        this.containsLiquids = containsLiquids;
    }

    public boolean containsLiquids() {
        return containsLiquids;
    }

    public int getDecorationHeight() {
        return highestBlock.getY() - terrainBlock.getY();
    }

    public BlockPos getHighestBlock() {
        return highestBlock;
    }

    public void setHighestBlock(BlockPos highestBlock) {
        this.highestBlock = highestBlock;
    }

    public BlockPos getTerrainBlock() {
        return terrainBlock;
    }

    public void setTerrainBlock(BlockPos terrainBlock) {
        this.terrainBlock = terrainBlock;
    }

    public Vec getNormal() {
        return normal;
    }

    public void setNormal(Vec normal) {
        this.normal = normal;
    }

    public double getDistanceFromCenter() {
        return distanceFromCenter;
    }

    public void setDistanceFromCenter(double distanceFromCenter) {
        this.distanceFromCenter = distanceFromCenter;
    }
}
