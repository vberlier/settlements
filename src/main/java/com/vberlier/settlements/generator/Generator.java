package com.vberlier.settlements.generator;

import com.google.common.graph.EndpointPair;
import com.google.common.graph.MutableValueGraph;
import com.google.common.graph.ValueGraphBuilder;
import com.vberlier.settlements.util.Point;
import com.vberlier.settlements.util.Vec;
import net.minecraft.block.Block;
import net.minecraft.block.BlockColored;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.gen.structure.StructureBoundingBox;

import java.util.HashSet;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;

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
    private final Position[][] positions;
    private final int verticesSizeX;
    private final int verticesSizeZ;
    private final Vec[][] vertices;
    private final Vec[][] normals;
    private final Vec origin;
    private final Vec center;
    private final MutableValueGraph<Slot, Integer> graph;

    private int slotSize = 300;
    private double slotFlexibility = 0.45;

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
        positions = new Position[sizeX][sizeZ];

        verticesSizeX = sizeX - 1;
        verticesSizeZ = sizeZ - 1;
        vertices = new Vec[verticesSizeX][verticesSizeZ];

        normals = new Vec[sizeX][sizeZ];

        for (int i = 0; i < sizeX; i++) {
            normals[i][0] = new Vec(0, 1, 0);
            normals[i][sizeZ - 1] = new Vec(0, 1, 0);
        }

        for (int i = 0; i < sizeZ; i++) {
            normals[0][i] = new Vec(0, 1, 0);
            normals[sizeX - 1][i] = new Vec(0, 1, 0);
        }

        origin = new Vec(originX, 0, originZ);
        center = origin.add((double) sizeX / 2.0, 0, (double) sizeZ / 2.0);

        graph = ValueGraphBuilder.undirected().build();
    }

    public void buildSettlement() {
        computeMaps();
        computeVertices();
        computeNormals();
        computeSurfaceGraph();
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

                Position coordinates = new Position(i, j, pos);
                this.positions[i][j] = coordinates;

                coordinates.setDistanceFromCenter(center.sub(x, 0, z).length());

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

    private void computeVertices() {
        for (int i = 0; i < verticesSizeX; i++) {
            for (int j = 0; j < verticesSizeZ; j++) {
                vertices[i][j] = Vec.average(terrainMap[i][j], terrainMap[i + 1][j], terrainMap[i + 1][j + 1], terrainMap[i][j + 1]);
            }
        }
    }

    private void computeNormals() {
        for (int i = 1; i < sizeX - 1; i++) {
            for (int j = 1; j < sizeZ - 1; j++) {
                Vec normal = Vec.normal(vertices[i - 1][j - 1], vertices[i - 1][j], vertices[i][j], vertices[i][j - 1]).normalize();
                normals[i][j] = normal;
                positions[i][j].setNormal(normal);
            }
        }
    }

    private void computeSurfaceGraph() {
        Queue<Position> nextBlocks = new PriorityQueue<>();
        Set<Position> availableBlocks = new HashSet<>();

        for (int i = 1; i < sizeX - 1; i++) {
            for (int j = 1; j < sizeZ - 1; j++) {
                Position coordinates = positions[i][j];
                nextBlocks.add(coordinates);
                availableBlocks.add(coordinates);
            }
        }

        int[][] offsets = new int[][]{{-1, 0}, {0, -1}, {1, 0}, {0, 1}};

        while (!nextBlocks.isEmpty()) {
            Position origin = nextBlocks.poll();

            Set<Position> surface = new HashSet<>();
            Set<Position> edge = new HashSet<>();

            availableBlocks.remove(origin);
            surface.add(origin);
            edge.add(origin);

            Vec normal = origin.getNormal();

            int prevCount = 0;
            int count = surface.size();

            while (prevCount != count && surface.size() < slotSize) {
                for (Position coordinates : edge) {
                    for (int[] offset : offsets) {
                        int i = coordinates.i + offset[0];
                        int j = coordinates.j + offset[1];

                        if (i < 1 || i >= sizeX - 1 || j < 1 || j >= sizeZ - 1) {
                            continue;
                        }

                        Position neighbor = positions[i][j];

                        if (!availableBlocks.contains(neighbor)) {
                            continue;
                        }

                        if (neighbor.getNormal().cross(normal).length() > slotFlexibility) {
                            continue;
                        }

                        nextBlocks.remove(neighbor);
                        availableBlocks.remove(neighbor);
                        surface.add(neighbor);
                    }
                }

                edge = new HashSet<>();
                normal = new Vec(0);

                for (Position coordinates : surface) {
                    normal = normal.add(coordinates.getNormal());

                    for (int[] offset : offsets) {
                        int i = coordinates.i + offset[0];
                        int j = coordinates.j + offset[1];

                        Position neighbor = positions[i][j];

                        if (!surface.contains(neighbor)) {
                            edge.add(coordinates);
                            break;
                        }
                    }
                }

                normal = normal.div(surface.size());

                prevCount = count;
                count = surface.size();
            }

            if (surface.size() < slotSize) {
                continue;
            }

            Slot node = new Slot(normal, surface, edge, positions);
            graph.addNode(node);
        }

        int color = 0;

        for (Slot node : graph.nodes()) {
            for (Position coordinates : node.getConvexHull()) {
                Slot neighbor = coordinates.getSurface();

                if (neighbor != null && neighbor != node) {
                    graph.putEdgeValue(node, neighbor, 1);
                }
            }

            for (Position coordinates : node.getSurfaceCoordinates()) {
                world.setBlockState(coordinates.getTerrainBlock().add(0, 1, 0), Blocks.CONCRETE.getDefaultState().withProperty(BlockColored.COLOR, EnumDyeColor.byMetadata(color % 16)));
            }

            color++;
        }

        for (EndpointPair<Slot> nodes : graph.edges()) {
            Position first = nodes.nodeU().getCenter();
            Position second = nodes.nodeV().getCenter();

            for (Point point : new Point(first).line(second)) {
                world.setBlockState(terrainMap[(int) point.x][(int) point.y].add(0, 2, 0), Blocks.BEDROCK.getDefaultState());
            }
        }
    }

    public int getSlotSize() {
        return slotSize;
    }

    public void setSlotSize(int slotSize) {
        this.slotSize = slotSize;
    }

    public double getSlotFlexibility() {
        return slotFlexibility;
    }

    public void setSlotFlexibility(double slotFlexibility) {
        this.slotFlexibility = slotFlexibility;
    }
}
