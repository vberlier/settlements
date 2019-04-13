package com.vberlier.settlements.generator;

import net.minecraft.block.Block;
import net.minecraft.block.BlockCrops;
import net.minecraft.block.BlockFarmland;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class FieldBuilder {
    private final World world;

    public FieldBuilder(World world) {
        this.world = world;
    }

    public void build(Slot slot) {
        BlockPos originalCenter = slot.getCenter().getTerrainBlock();
        BlockPos center = new BlockPos(originalCenter.getX(), world.getHeight(originalCenter.getX(), originalCenter.getZ()) - 1, originalCenter.getZ());

        for (Position position : slot.getSurface()) {
            BlockPos originalTerrain = position.getTerrainBlock();
            BlockPos pos = new BlockPos(originalTerrain.getX(), world.getHeight(originalTerrain.getX(), originalTerrain.getZ()) - 1, originalTerrain.getZ());

            if (Math.abs(pos.getY() - center.getY()) > 3) {
                continue;
            }

            IBlockState state = world.getBlockState(pos);
            Block block = state.getBlock();

            if (block != Blocks.DIRT && block != Blocks.GRASS) {
                continue;
            }

            if (pos.getX() % 9 == 0 && pos.getZ() % 9 == 0) {
                for (int[] offset : Position.neighbors) {
                    setWheat(pos.add(offset[0], 0, offset[1]));
                }
                world.setBlockState(pos, Blocks.WATER.getDefaultState());
            } else {
                setWheat(pos);
            }
        }
    }

    public void setWheat(BlockPos pos) {
        world.setBlockState(pos, Blocks.FARMLAND.getDefaultState().withProperty(BlockFarmland.MOISTURE, 7));
        world.setBlockState(pos.add(0, 1, 0), Blocks.WHEAT.getDefaultState().withProperty(BlockCrops.AGE, 7));
    }
}
