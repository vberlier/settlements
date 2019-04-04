package com.vberlier.settlements.generator;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.gen.structure.StructureBoundingBox;

public class Generator {
    private final World world;
    private final StructureBoundingBox boundingBox;
    private final int originX;
    private final int originZ;
    private final int sizeX;
    private final int sizeZ;
    private final BlockPos[][] heightMap;
    private final BlockPos[][] terrainMap;

    public Generator(World world, StructureBoundingBox boundingBox) {
        this.world = world;
        this.boundingBox = boundingBox;

        originX = boundingBox.minX;
        originZ = boundingBox.minZ;
        sizeX = boundingBox.getXSize();
        sizeZ = boundingBox.getZSize();

        heightMap = new BlockPos[sizeX][sizeZ];
        terrainMap = new BlockPos[sizeX][sizeZ];
    }

    public void buildSettlement() {
        computeMaps();

        for (int i = 0; i < sizeX; i++) {
            for (int j = 0; j < sizeZ; j++) {
                world.setBlockState(heightMap[i][j].add(0, 1, 0), Blocks.STAINED_GLASS.getDefaultState());
                world.setBlockState(terrainMap[i][j], Blocks.RED_NETHER_BRICK.getDefaultState());
            }
        }
    }

    private void computeMaps() {
        for (int i = 0; i < sizeX; i++) {
            for (int j = 0; j < sizeZ; j++) {
                int x = originX + i;
                int z = originZ + j;

                int y = world.getHeight(x, z);
                BlockPos pos = new BlockPos(x, y, z);

                heightMap[i][j] = pos;

                while (nonTerrain(pos) && pos.getY() > 0) {
                    pos = pos.down();
                }

                terrainMap[i][j] = pos;
            }
        }
    }

    private boolean nonTerrain(BlockPos pos) {
        IBlockState state = world.getBlockState(pos);
        Block block = state.getBlock();

        return block.isAir(state, world, pos)
                || block.isPassable(world, pos)
                || block.isFlammable(world, pos, EnumFacing.UP);
    }
}
