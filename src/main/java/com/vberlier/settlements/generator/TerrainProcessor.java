package com.vberlier.settlements.generator;

import com.vberlier.settlements.util.Point;
import com.vberlier.settlements.util.Vec;
import net.minecraft.block.Block;
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
            Vec centerDirection = originalCenter.sub(samplingOrigin).normalize();

            while (edgePosition.getLiquids().size() > 0 != centerPosition.getLiquids().size() > 0) {
                samplingOrigin = samplingOrigin.add(centerDirection);
                edgePosition = terrain[(int) Math.round(samplingOrigin.x - originX)][(int) Math.round(samplingOrigin.z - originZ)];
            }

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
        }

        for (int i = 0; i < 4; i++) {
            cleanupBlocks(minX, minZ, maxX, maxZ);
        }

        for (int i = 0; i < surfaceSizeX; i++) {
            for (int j = 0; j < surfaceSizeZ; j++) {
                int x = minX + i;
                int z = minZ + j;
                int y =  world.getHeight(x, z);

                heightsArray[i][j] = y;
                BlockPos block = new BlockPos(x, y, z);

                Position position  = terrain[x - originX][z - originZ];
                position.setTerrainBlock(block);
                position.setTerrainBlockState(world.getBlockState(block));
            }
        }

        recomputeNormals(minX, minZ, heightsArray);
    }

    private void removeVegetation(int x, int z) {
        BlockPos current = new BlockPos(x, world.getHeight(x, z), z);

        while (current.getY() > 0) {
            IBlockState state = world.getBlockState(current);
            Block block = state.getBlock();

            if (!block.isAir(state, world, current) && !block.isPassable(world, current) && !block.isFlammable(world, current, EnumFacing.UP)) {
                break;
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

            if (!block.isAir(state, world, current) && !block.isPassable(world, current) && !block.isFlammable(world, current, EnumFacing.UP)) {
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

        if (block.equals(Blocks.GRASS)) {
            block = Blocks.DIRT;
            state = block.getDefaultState();
        }

        for (int i = current.getY(); i < y; i++) {
            world.setBlockState(new BlockPos(current.getX(), i, current.getZ()), state);
        }

        if (block.equals(Blocks.DIRT)) {
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

                for (int[] offset : Position.neighbors) {
                    BlockPos neighbor = new BlockPos(x + offset[0], y, z + offset[1]);

                    if (!world.isAirBlock(neighbor)) {
                        neighbors++;
                    }
                }

                if (neighbors < 2) {
                    world.setBlockToAir(new BlockPos(x, y, z));
                    continue;
                }

                y++;

                int upperNeighbors = 0;
                IBlockState state = Blocks.AIR.getDefaultState();

                for (int[] offset : Position.neighbors) {
                    BlockPos neighbor = new BlockPos(x + offset[0], y, z + offset[1]);

                    if (!world.isAirBlock(neighbor)) {
                        upperNeighbors++;
                        state = world.getBlockState(neighbor);
                    }
                }

                if (upperNeighbors > 2) {
                    world.setBlockState(new BlockPos(x, y, z), state);
                }
            }
        }
    }

    private void recomputeNormals(int minX, int minZ, int[][] heightsArray) {
        int verticesSizeX = heightsArray.length - 1;
        int verticesSizeZ = heightsArray[0].length - 1;
        Vec[][] vertices = new Vec[verticesSizeX][verticesSizeZ];

        Vec[][] firstPass = vertices.clone();

        for (int i = 0; i < verticesSizeX; i++) {
            for (int j = 0; j < verticesSizeZ; j++) {
                Vec neighbors = Vec.zero;

                for (int x = 0; x < 2; x++) {
                    for (int z = 0; z < 2; z++) {
                        neighbors = neighbors.add(new Vec(minX + i + x, heightsArray[i + x][j + z], minZ + j + z));
                    }
                }

                firstPass[i][j] = neighbors.div(4);
            }
        }

        smoothPass(firstPass, vertices);

        for (int i = 1; i < verticesSizeX; i++) {
            for (int j = 1; j < verticesSizeZ; j++) {
                Vec normal = Vec.normal(vertices[i - 1][j - 1], vertices[i - 1][j], vertices[i][j], vertices[i][j - 1]).normalize();
                terrain[i + minX - originX][j + minZ - originZ].setNormal(normal);
            }
        }
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
