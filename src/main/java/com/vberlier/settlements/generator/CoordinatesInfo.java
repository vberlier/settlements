package com.vberlier.settlements.generator;

import com.vberlier.settlements.util.Vec;
import net.minecraft.util.math.BlockPos;

import java.util.Objects;

public class CoordinatesInfo implements Comparable<CoordinatesInfo> {
    public final int i;
    public final int j;
    private BlockPos highestBlock;
    private BlockPos terrainBlock;
    private boolean containsLiquids = false;
    private Vec normal;
    private double distanceFromCenter = 0;
    private TerrainSurface surface;

    public CoordinatesInfo(int i, int j, BlockPos highestBlock) {
        this.i = i;
        this.j = j;
        this.highestBlock = highestBlock;
        this.terrainBlock = highestBlock;
        normal = Vec.up;
    }

    public int getDecorationHeight() {
        return highestBlock.getY() - terrainBlock.getY();
    }

    public double getVerticality() {
        return Vec.up.cross(normal).length();
    }

    public void setContainsLiquids(boolean containsLiquids) {
        this.containsLiquids = containsLiquids;
    }

    public boolean containsLiquids() {
        return containsLiquids;
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

    @Override
    public int compareTo(CoordinatesInfo o) {
        int res = Boolean.compare(containsLiquids, o.containsLiquids);
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
        CoordinatesInfo that = (CoordinatesInfo) o;
        return i == that.i &&
                j == that.j;
    }

    @Override
    public int hashCode() {
        return Objects.hash(i, j);
    }

    public TerrainSurface getSurface() {
        return surface;
    }

    public void setSurface(TerrainSurface surface) {
        this.surface = surface;
    }
}
