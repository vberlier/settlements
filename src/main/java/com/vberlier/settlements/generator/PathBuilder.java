package com.vberlier.settlements.generator;

import net.minecraft.block.*;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class PathBuilder {
    private final World world;
    private final BlockPlanks.EnumType woodVariant;

    public PathBuilder(World world, BlockPlanks.EnumType woodVariant) {
        this.world = world;
        this.woodVariant = woodVariant;
    }

    public BlockPos setBlockOrSlab(BlockPos pos) {
        BlockPos below = pos.add(0, -1, 0);

        if (world.isAirBlock(pos) && !world.isAirBlock(below) && setBlock(below)) {
            for (int[] offset : Position.neighbors) {
                BlockPos neighbor = pos.add(offset[0], 0, offset[1]);

                if (!world.isAirBlock(neighbor) && !(world.getBlockState(neighbor).getBlock() instanceof BlockSlab)) {
                    setSlab(pos);
                    break;
                }
            }
            return below;
        } else {
            if (setBlock(pos)) {
                return pos;
            }
        }

        return null;
    }

    public boolean setBlock(BlockPos pos) {
        IBlockState state = world.getBlockState(pos);
        Block block = state.getBlock();

        if (!world.isAirBlock(pos.add(0, 1, 0))) {
            return false;
        }

        if (block instanceof BlockDirt || block instanceof BlockGrass || block instanceof BlockSand) {
            world.setBlockState(pos, randomGrassyState());
            world.setBlockState(pos.add(0, -1, 0), Blocks.DIRT.getDefaultState());
        } else if (block instanceof BlockStone) {
            world.setBlockState(pos, randomStonyState());
        } else {
            return false;
        }

        return true;
    }

    public boolean setPlanks(BlockPos pos) {
        IBlockState state = world.getBlockState(pos);
        Block block = state.getBlock();

        if (world.containsAnyLiquid(new AxisAlignedBB(pos))) {
            world.setBlockState(pos, Blocks.PLANKS.getDefaultState().withProperty(BlockPlanks.VARIANT, woodVariant));
        } else if (world.isAirBlock(pos)) {
            world.setBlockState(pos, Blocks.WOODEN_SLAB.getDefaultState().withProperty(BlockPlanks.VARIANT, woodVariant).withProperty(BlockSlab.HALF, BlockSlab.EnumBlockHalf.TOP));
        } else {
            return false;
        }

        return true;
    }

    public boolean setSlab(BlockPos pos) {
        BlockPos below = pos.add(0, -1, 0);

        IBlockState state = world.getBlockState(below);
        Block block = state.getBlock();

        if (block instanceof BlockDirt || block instanceof BlockGrass || block instanceof BlockGrassPath) {
            world.setBlockState(pos, Blocks.WOODEN_SLAB.getDefaultState().withProperty(BlockPlanks.VARIANT, woodVariant));
        } else if (block instanceof BlockGravel || block == Blocks.COBBLESTONE || block instanceof BlockStone) {
            world.setBlockState(pos, Blocks.STONE_SLAB.getDefaultState().withProperty(BlockStoneSlab.VARIANT, BlockStoneSlab.EnumType.COBBLESTONE));
        } else {
            return false;
        }

        return true;
    }

    private IBlockState randomGrassyState() {
        switch (world.rand.nextInt(7)) {
            case 0:
                return Blocks.DIRT.getDefaultState().withProperty(BlockDirt.VARIANT, BlockDirt.DirtType.COARSE_DIRT);
            case 1:
                return Blocks.GRAVEL.getDefaultState();
            case 2:
                return Blocks.GRASS.getDefaultState();
            default:
                return Blocks.GRASS_PATH.getDefaultState();
        }
    }

    private IBlockState randomStonyState() {
        switch (world.rand.nextInt(7)) {
            case 0:
                return Blocks.GRAVEL.getDefaultState();
            case 1:
                return Blocks.DIRT.getDefaultState().withProperty(BlockDirt.VARIANT, BlockDirt.DirtType.COARSE_DIRT);
            case 2:
                return Blocks.STONE.getDefaultState();
            default:
                return Blocks.COBBLESTONE.getDefaultState();
        }
    }
}
