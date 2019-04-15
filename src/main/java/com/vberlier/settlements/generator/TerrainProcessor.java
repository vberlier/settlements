package com.vberlier.settlements.generator;

import com.vberlier.settlements.util.Point;
import com.vberlier.settlements.util.Vec;
import net.minecraft.block.*;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.*;

public class TerrainProcessor {
    private static Vec[] samples = {new Vec(-0.25, 0, -0.25), new Vec(0.25, 0, -0.25), new Vec(0.25, 0, 0.25), new Vec(-0.25, 0, 0.25)};

    private final World world;
    private int originX;
    private int originZ;
    private Position[][] terrain;

    private Map<BlockPlanks.EnumType, Integer> woodVariants = new HashMap<>();

    public TerrainProcessor(World world, int originX, int originZ, Position[][] terrain) {
        this.world = world;
        this.originX = originX;
        this.originZ = originZ;
        this.terrain = terrain;
    }

    public void flatten(Slot slot, Vec centerNormal, double slotRadius) {
        Set<BlockPos> surface = new HashSet<>();

        Position centerPosition = slot.getCenter();
        Vec originalCenter = new Vec(centerPosition.getTerrainBlock());

        for (Vec vec : Vec.interpolateDisk(originalCenter, centerNormal, slotRadius - 1, 6)) {
            surface.add(vec.block());
        }

        for (Position edgePosition : slot.getConvexHull()) {
            Vec samplingOrigin = new Vec(edgePosition.getTerrainBlock());

            for (Vec sample : samples) {
                Vec edge = samplingOrigin.add(sample);
                Vec edgeNormal = edgePosition.getNormal();

                Vec line = originalCenter.sub(edge);

                if (line.length() < slotRadius) {
                    continue;
                }

                Vec projectedPoint = Vec.planeLineIntersection(originalCenter, centerNormal, edge, centerNormal);
                Vec center = originalCenter.sub(originalCenter.sub(projectedPoint).normalize().mul(slotRadius));

                double handleSize = center.sub(edge).length() / 3;

                Vec centerIntersection = Vec.planeLineIntersection(center, centerNormal, edge, edgeNormal);
                if (centerIntersection == null) {
                    System.out.println("No intersection");
                    continue;
                }

                Vec edgeIntersection = Vec.planeLineIntersection(edge, edgeNormal, center, centerNormal);
                if (edgeIntersection == null) {
                    System.out.println("No intersection");
                    continue;
                }

                Vec centerHandleDirection = center.sub(centerIntersection).normalize();
                Vec edgeHandleDirection = edge.sub(edgeIntersection).normalize();

                Vec centerControl = center.add(centerHandleDirection.mul(handleSize));
                Vec edgeControl = edge.add(edgeHandleDirection.mul(handleSize));

                for (Vec vec : Vec.interpolateBezier(edge, edgeControl, centerControl, center, 6)) {
                    surface.add(vec.block());
                }
            }
        }

        Map<Point, Set<Double>> heightsSets = new HashMap<>();

        for (BlockPos block : surface) {
            Point key = new Point(block.getX(), block.getZ());
            Set<Double> heights = heightsSets.computeIfAbsent(key, k -> new HashSet<>());
            heights.add((double) block.getY());
        }

        surface = new HashSet<>();

        int minX = (int) originalCenter.x;
        int minZ = (int) originalCenter.z;
        int maxX = minX;
        int maxZ = minZ;

        for (Map.Entry<Point, Set<Double>> entry : heightsSets.entrySet()) {
            Point point = entry.getKey();
            Set<Double> heights = entry.getValue();

            BlockPos block = new BlockPos(point.x, heights.stream().mapToDouble(d -> d).average().getAsDouble(), point.y);

            surface.add(block);

            minX = Math.min(minX, block.getX());
            minZ = Math.min(minZ, block.getZ());
            maxX = Math.max(maxX, block.getX());
            maxZ = Math.max(maxZ, block.getZ());
        }

        int surfaceSizeX = maxX - minX + 1;
        int surfaceSizeZ = maxZ - minZ + 1;

        int[][] heightsArray = new int[surfaceSizeX][surfaceSizeZ];

        for (BlockPos block : surface) {
            heightsArray[block.getX() - minX][block.getZ() - minZ] = block.getY();
        }

        Set<BlockPos> liquidEdge = new HashSet<>();

        for (BlockPos block : surface) {
            int x = block.getX();
            int z = block.getZ();

            int heightSum = 0;
            int totalHeights = 0;

            for (int i = -1; i < 2; i++) {
                for (int j = -1; j < 2; j++) {
                    int a = Math.max(0, Math.min(x - minX + i, surfaceSizeX - 1));
                    int b = Math.max(0, Math.min(z - minZ + j, surfaceSizeZ - 1));

                    int height = heightsArray[a][b];

                    if (height > 0) {
                        heightSum += height;
                        totalHeights++;
                    }
                }
            }

            int y = heightSum / totalHeights;

            removeVegetation(x, z);
            clearBlocksAbove(x, y, z);
            fillBlocksBelow(x, y, z);

            BlockPos pos = new BlockPos(x, world.getHeight(x, z) - 1, z);

            if (!world.containsAnyLiquid(new AxisAlignedBB(pos))) {
                for (int[] offset : Position.neighbors) {
                    BlockPos neighbor = new BlockPos(x + offset[0], world.getHeight(x + offset[0], z + offset[1]) - 1, z + offset[1]);

                    if (world.containsAnyLiquid(new AxisAlignedBB(neighbor))) {
                        liquidEdge.add(new BlockPos(x, neighbor.getY(), z));
                    }
                }
            }
        }

        for (Position position : slot.getConvexHull()) {
            BlockPos originalTerrain = position.getTerrainBlock();
            cleanupAdjacentVegetation(originalTerrain.getX(), originalTerrain.getZ());
        }

        for (int i = 0; i < 7; i++) {
            cleanupBlocks(minX, minZ, maxX, maxZ);
        }

        for (BlockPos pos : liquidEdge) {
            for (int i = 0; i < 12; i++) {
                world.setBlockState(pos.add(0, -i, 0), Blocks.COBBLESTONE.getDefaultState());
            }
        }
    }

    private void removeVegetation(int x, int z) {
        BlockPos current = new BlockPos(x, world.getHeight(x, z), z);

        while (current.getY() > 0) {
            IBlockState state = world.getBlockState(current);
            Block block = state.getBlock();

            if (!block.isAir(state, world, current) && !block.isPassable(world, current) && !block.isFlammable(world, current, EnumFacing.UP) && !(block instanceof BlockHugeMushroom)) {
                break;
            }

            if (block instanceof BlockOldLog) {
                BlockPlanks.EnumType variant = state.getValue(BlockOldLog.VARIANT);
                woodVariants.put(variant, woodVariants.getOrDefault(variant, 0) + 1);
            }

            if (block instanceof BlockNewLog) {
                BlockPlanks.EnumType variant = state.getValue(BlockNewLog.VARIANT);
                woodVariants.put(variant, woodVariants.getOrDefault(variant, 0) + 1);
            }

            if (!world.containsAnyLiquid(new AxisAlignedBB(current))) {
                world.setBlockToAir(current);
            }

            current = current.down();
        }
    }

    private void clearBlocksAbove(int x, int y, int z) {
        BlockPos current = new BlockPos(x, world.getHeight(x, z), z);

        while (current.getY() > y) {
            if (!world.containsAnyLiquid(new AxisAlignedBB(current))) {
                world.setBlockToAir(current);
            }
            current = current.down();
        }
    }

    private void fillBlocksBelow(int x, int y, int z) {
        BlockPos current = new BlockPos(x, y, z);

        do {
            IBlockState state = world.getBlockState(current);
            Block block = state.getBlock();

            if (!block.isAir(state, world, current) && !block.isPassable(world, current) && !block.isFlammable(world, current, EnumFacing.UP) && !(block instanceof BlockHugeMushroom)) {
                break;
            }

            current = current.down();
        } while (current.getY() > 0);

        Position position;

        try {
            position = terrain[x - originX][z - originZ];
        } catch (ArrayIndexOutOfBoundsException e) {
            System.out.println("Couldn't access position object: x = " + x + ", z = " + z);
            return;
        }

        IBlockState state = position.getTerrainBlockState();
        Block block = state.getBlock();

        if (block instanceof BlockGrass) {
            block = Blocks.DIRT;
            state = block.getDefaultState();
        }

        for (int i = current.getY(); i < y; i++) {
            world.setBlockState(new BlockPos(current.getX(), i, current.getZ()), state);
        }

        if (block instanceof BlockDirt) {
            world.setBlockState(current, Blocks.GRASS.getDefaultState());
        } else {
            world.setBlockState(current, state);
        }
    }

    private void cleanupBlocks(int minX, int minZ, int maxX, int maxZ) {
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                int y = world.getHeight(x, z);

                int neighbors = 0;
                boolean liquidFlag = false;

                for (int[] offset : Position.neighbors) {
                    BlockPos neighbor = new BlockPos(x + offset[0], y, z + offset[1]);
                    IBlockState state = world.getBlockState(neighbor);
                    Block block = state.getBlock();

                    if (!block.isAir(state, world, neighbor) && !block.isPassable(world, neighbor) && !block.isFlammable(world, neighbor, EnumFacing.UP) && !(block instanceof BlockHugeMushroom)) {
                        neighbors++;
                    }

                    if (world.containsAnyLiquid(new AxisAlignedBB(neighbor))) {
                        liquidFlag = true;
                        break;
                    }
                }

                if (liquidFlag || neighbors > 2) {
                    continue;
                }

                BlockPos block = new BlockPos(x, y, z);

                world.setBlockToAir(block);

                block = block.down();

                if (world.getBlockState(block).getBlock() instanceof BlockDirt) {
                    world.setBlockState(block, Blocks.GRASS.getDefaultState());
                }
            }
        }
    }

    private void cleanupAdjacentVegetation(int x, int z) {
        BlockPos current = new BlockPos(x, world.getHeight(x, z), z);
        IBlockState state = world.getBlockState(current);
        Block block = state.getBlock();

        while (block.isAir(state, world, current) || block.isPassable(world, current) || block.isFlammable(world, current, EnumFacing.UP) || block instanceof BlockHugeMushroom) {
            world.setBlockToAir(current);

            current = current.down();
            state = world.getBlockState(current);
            block = state.getBlock();
        }

        for (int i = -1; i < 2; i++) {
            for (int j = -1; j < 2; j++) {
                if (i == 0 && j == 0) {
                    continue;
                }

                current = new BlockPos(x + i, world.getHeight(x + i, z + j), z + j);

                state = world.getBlockState(current);
                block = state.getBlock();

                while (block.isAir(state, world, current) || block.isPassable(world, current) || block.isFlammable(world, current, EnumFacing.UP) || block instanceof BlockHugeMushroom) {
                    if (block.isWood(world, current) || block instanceof BlockHugeMushroom) {
                        cleanupAdjacentVegetation(current.getX(), current.getZ());
                        break;
                    }

                    current = current.down();
                    state = world.getBlockState(current);
                    block = state.getBlock();
                }
            }
        }
    }

    public Map<BlockPlanks.EnumType, Integer> getWoodVariants() {
        return woodVariants;
    }

    public BlockPlanks.EnumType mostCommonWoodVariant() {
        BlockPlanks.EnumType variant = BlockPlanks.EnumType.OAK;
        int count = 0;

        for (Map.Entry<BlockPlanks.EnumType, Integer> entry : woodVariants.entrySet()) {
            if (entry.getValue() > count) {
                variant = entry.getKey();
                count = entry.getValue();
            }
        }

        return variant;
    }

    public static void smoothPass(Vec[][] input, Vec[][] result) {
        int sizeX = result.length;
        int sizeZ = result[0].length;

        for (int i = 1; i < sizeX - 1; i++) {
            for (int j = 1; j < sizeZ - 1; j++) {
                Vec neighbors = Vec.zero;

                for (int x = -1; x < 2; x++) {
                    for (int z = -1; z < 2; z++) {
                        neighbors = neighbors.add(input[i + x][j + z]);
                    }
                }

                result[i][j] = neighbors.div(9);
            }
        }
    }
}
