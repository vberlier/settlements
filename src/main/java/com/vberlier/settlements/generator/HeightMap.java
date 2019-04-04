package com.vberlier.settlements.generator;

import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.gen.structure.StructureBoundingBox;

import java.util.ArrayList;

public class HeightMap {
    protected final World world;
    protected final StructureBoundingBox boundingBox;

    protected final int xSize;
    protected final int zSize;

    protected final int[][] map;
    protected final BlockPos origin;
    protected final ArrayList<BlockPos> positions;

    protected int minY;
    protected int maxY;

    public HeightMap(World world, StructureBoundingBox boundingBox) {
        this.world = world;
        this.boundingBox = boundingBox;

        xSize = boundingBox.getXSize();
        zSize = boundingBox.getZSize();

        map = new int[xSize][zSize];
        origin = new BlockPos(boundingBox.minX, boundingBox.minY, boundingBox.minZ);
        positions = new ArrayList<>(xSize * zSize);

        minY = 256;
        maxY = 0;
    }

    public void compute() {
        for (int i = 0, x = origin.getX(); i < xSize; i++) {
            for (int j = 0, z = origin.getZ(); j < zSize; j++) {
                computePosition(i, j, x, z);
                int y = map[i][j];
                minY = Math.min(y, minY);
                maxY = Math.max(y, maxY);
                z++;
            }
            x++;
        }
    }

    public void computePosition(int i, int j, int x, int z) {
        int y =  world.getHeight(x, z);
        map[i][j] = y;
        positions.add(new BlockPos(x, y, z));
    }

    public int get(int i, int j) {
        return map[i][j];
    }

    public World getWorld() {
        return world;
    }

    public StructureBoundingBox getBoundingBox() {
        return boundingBox;
    }

    public int getxSize() {
        return xSize;
    }

    public int getzSize() {
        return zSize;
    }

    public int[][] getMap() {
        return map;
    }

    public BlockPos getOrigin() {
        return origin;
    }

    public ArrayList<BlockPos> getPositions() {
        return positions;
    }

    public int getMinY() {
        return minY;
    }

    public int getMaxY() {
        return maxY;
    }
}
