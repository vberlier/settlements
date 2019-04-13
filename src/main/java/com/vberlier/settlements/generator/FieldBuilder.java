package com.vberlier.settlements.generator;

import com.vberlier.settlements.util.Vec;
import net.minecraft.block.Block;
import net.minecraft.block.BlockCrops;
import net.minecraft.block.BlockFarmland;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class FieldBuilder {
    private final World world;
    private final Position[][] terrain;

    public FieldBuilder(World world, Position[][] terrain) {
        this.world = world;
        this.terrain = terrain;
    }

    public void build(Set<Slot> slots) {
        Set<Position> surface = computeConvexSurface(slots);

        for (Position position : surface) {
            BlockPos originalTerrain = position.getTerrainBlock();
            BlockPos pos = new BlockPos(originalTerrain.getX(), world.getHeight(originalTerrain.getX(), originalTerrain.getZ()) - 1, originalTerrain.getZ());

            IBlockState state = world.getBlockState(pos);
            Block block = state.getBlock();

            if (!canReplaceWithFarmland(block)) {
                continue;
            }

            if (position.getNormal().project(Vec.Axis.Y).length() < 0.9) {
                continue;
            }

            if (pos.getX() % 9 == 0 && pos.getZ() % 9 == 0) {
                for (int[] offset : Position.neighbors) {
                    setWheat(pos.add(offset[0], 0, offset[1]));
                }
                world.setBlockState(pos, Blocks.WATER.getDefaultState());
                world.setBlockState(pos.add(0, 1, 0), Blocks.TRAPDOOR.getDefaultState());
            } else {
                setWheat(pos);
            }
        }
    }

    public static boolean canReplaceWithFarmland(Block block) {
        return block == Blocks.DIRT || block == Blocks.GRASS;
    }

    private Set<Position> computeConvexSurface(Set<Slot> slots) {
        Set<Position> positions = slots.stream().flatMap(slot -> Arrays.stream(slot.getSurface())).collect(Collectors.toSet());
        Slot tmpSlot = new Slot(positions, terrain);

        Set<Position> convexHull = tmpSlot.getConvexHull();
        Set<Position> surface = new HashSet<>();

        Stack<Position> positionStack = Stream.of(tmpSlot.getCenter()).collect(Collectors.toCollection(Stack::new));

        while (!positionStack.isEmpty()) {
            Position position = positionStack.pop();

            surface.add(position);

            for (int[] offset : Position.neighbors) {
                int i = position.i + offset[0];
                int j = position.j + offset[1];

                Position neighbor;

                try {
                    neighbor = terrain[i][j];
                } catch (ArrayIndexOutOfBoundsException e) {
                    continue;
                }

                if (surface.contains(neighbor)) {
                    continue;
                }

                if (convexHull.contains(neighbor)) {
                    continue;
                }

                if (neighbor.getLiquids().size() > 0) {
                    continue;
                }

                positionStack.push(neighbor);
            }
        }

        return surface;
    }

    public void setWheat(BlockPos pos) {
        world.setBlockState(pos, Blocks.FARMLAND.getDefaultState().withProperty(BlockFarmland.MOISTURE, 7));
        world.setBlockState(pos.add(0, 1, 0), Blocks.WHEAT.getDefaultState().withProperty(BlockCrops.AGE, 7));
    }
}
