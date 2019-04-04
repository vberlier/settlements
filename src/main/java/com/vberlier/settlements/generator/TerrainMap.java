package com.vberlier.settlements.generator;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;

public class TerrainMap extends HeightMap {
    private final HeightMap heightMap;

    public TerrainMap(HeightMap heightMap) {
        super(heightMap.getWorld(), heightMap.getBoundingBox());

        this.heightMap = heightMap;
    }

    @Override
    public void computePosition(int i, int j, int x, int z) {
        BlockPos pos = new BlockPos(x, heightMap.get(i, j), z);

        while (!terrainBlock(pos) && pos.getY() > 0) {
            pos = pos.down();
        }

        map[i][j] = pos.getY();
        positions.add(pos);
    }

    private boolean terrainBlock(BlockPos pos) {
        IBlockState state = world.getBlockState(pos);
        Block block = state.getBlock();

        return !block.isAir(state, world, pos)
                && !block.isPassable(world, pos)
                && !block.isFlammable(world, pos, EnumFacing.UP);
    }

    public HeightMap getHeightMap() {
        return heightMap;
    }
}
