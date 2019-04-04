package com.vberlier.settlements.generator;

import net.minecraft.util.math.BlockPos;

public class CoordinatesInfo {
    public final int i;
    public final int j;
    private BlockPos highestBlock;
    private BlockPos terrainBlock;
    private boolean containsLiquids = false;

    public CoordinatesInfo(int i, int j, BlockPos highestBlock) {
        this.i = i;
        this.j = j;
        this.highestBlock = highestBlock;
        this.terrainBlock = highestBlock;
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
}
