package com.vberlier.settlements.generator;

import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.gen.structure.StructureBoundingBox;

import java.util.ArrayList;

public class HeightMap {
    private final int xSize;
    private final int zSize;
    private final int[][] map;
    private final BlockPos origin;
    private final ArrayList<BlockPos> positions;

    public HeightMap(World world, StructureBoundingBox boundingBox) {
        xSize = boundingBox.getXSize();
        zSize = boundingBox.getZSize();

        map = new int[xSize][zSize];
        origin = new BlockPos(boundingBox.minX, boundingBox.minY, boundingBox.minZ);
        positions = new ArrayList<>(xSize * zSize);

        for (int i = 0, x = origin.getX(); i < xSize; i++) {
            for (int j = 0, z = origin.getZ(); j < zSize; j++) {
                int y =  world.getHeight(x, z);
                map[i][j] = y;
                positions.add(new BlockPos(x, y, z));
                z++;
            }
            x++;
        }
    }

    public int get(BlockPos pos) {
        return map[pos.getX() - origin.getX()][pos.getZ() - origin.getZ()];
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
}
