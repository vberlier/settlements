package com.vberlier.settlements.generator;

import com.vberlier.settlements.util.Vec;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.Objects;

public class Position implements Comparable<Position> {
    public static final int[][] neighbors = new int[][]{{-1, 0}, {0, -1}, {1, 0}, {0, 1}};

    public final int i;
    public final int j;
    private BlockPos highestBlock;
    private BlockPos terrainBlock;
    private IBlockState terrainBlockState;
    private Vec normal;
    private ArrayList<BlockPos> liquids;
    private ArrayList<BlockPos> vegetation;
    private double distanceFromCenter = 0;
    private Slot surface;

    public Position(int i, int j, BlockPos highestBlock) {
        this.i = i;
        this.j = j;
        this.highestBlock = highestBlock;
        this.terrainBlock = highestBlock;
        normal = Vec.up;
        liquids = new ArrayList<>();
        vegetation = new ArrayList<>();
    }

    public int getDecorationHeight() {
        return highestBlock.getY() - terrainBlock.getY();
    }

    public double getVerticality() {
        return Vec.up.cross(normal).length();
    }

    public void addLiquid(BlockPos pos) {
        liquids.add(pos);
    }

    public ArrayList<BlockPos> getLiquids() {
        return liquids;
    }

    public boolean containsLiquids() {
        return liquids.size() > 2;
    }

    public void addVegetation(BlockPos pos) {
        vegetation.add(pos);
    }

    public ArrayList<BlockPos> getVegetation() {
        return vegetation;
    }

    public boolean containsVegetation() {
        return getDecorationHeight() > 2 && vegetation.size() > 0;
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

    public Slot getSurface() {
        return surface;
    }

    public void setSurface(Slot surface) {
        this.surface = surface;
    }

    public IBlockState getTerrainBlockState() {
        return terrainBlockState;
    }

    public void setTerrainBlockState(IBlockState terrainBlockState) {
        this.terrainBlockState = terrainBlockState;
    }

    @Override
    public int compareTo(Position o) {
        int res = Integer.compare(liquids.size(), o.liquids.size());
        if (res != 0) {
            return res;
        }

        res = Double.compare(getVerticality(), o.getVerticality());
        if (res != 0) {
            return res;
        }

        res = Integer.compare(getDecorationHeight(), o.getDecorationHeight());
        if (res != 0) {
            return res;
        }

        res = Double.compare(distanceFromCenter, o.distanceFromCenter);
        if (res != 0) {
            return res;
        }

        return terrainBlock.compareTo(o.terrainBlock);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Position that = (Position) o;
        return i == that.i &&
                j == that.j;
    }

    @Override
    public int hashCode() {
        return Objects.hash(i, j);
    }
}
