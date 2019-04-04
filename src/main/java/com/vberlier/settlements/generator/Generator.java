package com.vberlier.settlements.generator;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
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
    private final int[][] heights;
    private final BlockPos[][] heightMap;
    private final BlockPos[][] terrainMap;
    private final CoordinatesInfo[][] coordinatesInfos;

    public Generator(World world, StructureBoundingBox boundingBox) {
        this.world = world;
        this.boundingBox = boundingBox;

        originX = boundingBox.minX;
        originZ = boundingBox.minZ;
        sizeX = boundingBox.getXSize();
        sizeZ = boundingBox.getZSize();

        heights = new int[sizeX][sizeZ];
        heightMap = new BlockPos[sizeX][sizeZ];
        terrainMap = new BlockPos[sizeX][sizeZ];
        coordinatesInfos = new CoordinatesInfo[sizeX][sizeZ];
    }

    public void buildSettlement() {
        computeMaps();

        for (int i = 0; i < sizeX; i++) {
            for (int j = 0; j < sizeZ; j++) {
                CoordinatesInfo coordinates = this.coordinatesInfos[i][j];

                if (coordinates.getDecorationHeight() < 3 && !coordinates.containsLiquids()) {
                    world.setBlockState(coordinates.getHighestBlock().add(0, 1, 0), Blocks.STAINED_GLASS.getDefaultState());
                    world.setBlockState(coordinates.getTerrainBlock(), Blocks.BEDROCK.getDefaultState());
                }
            }
        }
    }

    private void computeMaps() {
        for (int i = 0; i < sizeX; i++) {
            for (int j = 0; j < sizeZ; j++) {
                int x = originX + i;
                int z = originZ + j;

                int y = world.getHeight(x, z);
                heights[i][j] = y;

                BlockPos pos = new BlockPos(x, y, z);
                heightMap[i][j] = pos;

                CoordinatesInfo coordinates = new CoordinatesInfo(i, j, pos);
                this.coordinatesInfos[i][j] = coordinates;

                while (pos.getY() > 0) {
                    IBlockState state = world.getBlockState(pos);
                    Block block = state.getBlock();

                    if (!coordinates.containsLiquids() && world.containsAnyLiquid(new AxisAlignedBB(pos))) {
                        coordinates.setContainsLiquids(true);
                    }

                    if (block.isAir(state, world, pos) || block.isPassable(world, pos) || block.isFlammable(world, pos, EnumFacing.UP)) {
                        pos = pos.down();
                    } else {
                        break;
                    }
                }

                terrainMap[i][j] = pos;
                coordinates.setTerrainBlock(pos);
            }
        }
    }
}
