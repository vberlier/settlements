package com.vberlier.settlements.generator;

import com.google.common.graph.EndpointPair;
import com.google.common.graph.MutableValueGraph;
import com.google.common.graph.ValueGraphBuilder;
import com.vberlier.settlements.util.Point;
import com.vberlier.settlements.util.Vec;
import net.minecraft.block.Block;
import net.minecraft.block.BlockColored;
import net.minecraft.block.BlockPlanks;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.gen.structure.StructureBoundingBox;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
    private double normalConnectivity = 16;
    private double safeSlotRadius = 0.75 * Math.sqrt(slotSize / Math.PI);

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
        computeSlotGraph();
        removeShortEdges();
        removeTriangles();
        processGraph();

        debugEdges();
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

                IBlockState state = world.getBlockState(pos);

                while (pos.getY() > 0) {
                    state = world.getBlockState(pos);
                    Block block = state.getBlock();

                    if (world.containsAnyLiquid(new AxisAlignedBB(pos))) {
                        coordinates.addLiquid(pos);
                    }

                    if (block.isWood(world, pos) || block.isLeaves(state, world, pos)) {
                        coordinates.addVegetation(pos);
                    }

                    if (block.isAir(state, world, pos) || block.isPassable(world, pos) || block.isFlammable(world, pos, EnumFacing.UP)) {
                        pos = pos.down();
                    } else {
                        break;
                    }
                }

                terrainMap[i][j] = pos;
                coordinates.setTerrainBlock(pos);
                coordinates.setTerrainBlockState(state);
            }
        }
    }

    private void computeVertices() {
        Vec[][] firstPass = vertices.clone();

        for (int i = 0; i < verticesSizeX; i++) {
            for (int j = 0; j < verticesSizeZ; j++) {
                vertices[i][j] = Vec.average(terrainMap[i][j], terrainMap[i + 1][j], terrainMap[i + 1][j + 1], terrainMap[i][j + 1]);
                firstPass[i][j] = vertices[i][j];
            }
        }

        TerrainProcessor.smoothPass(firstPass, vertices);
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

    private void computeSlotGraph() {
        Queue<Position> nextBlocks = new PriorityQueue<>();
        Set<Position> availableBlocks = new HashSet<>();

        for (int i = 1; i < sizeX - 1; i++) {
            for (int j = 1; j < sizeZ - 1; j++) {
                Position coordinates = positions[i][j];
                nextBlocks.add(coordinates);
                availableBlocks.add(coordinates);
            }
        }

        while (!nextBlocks.isEmpty()) {
            Position origin = nextBlocks.poll();

            Set<Position> surface = new HashSet<>();

            availableBlocks.remove(origin);
            surface.add(origin);

            Set<Position> edge = Slot.computeEdge(surface, positions);

            Vec normal = origin.getNormal();

            int prevCount = 0;
            int count = surface.size();

            while (prevCount != count && surface.size() < slotSize) {
                for (Position coordinates : edge) {
                    for (int[] offset : Position.neighbors) {
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

                        if (origin.getLiquids().size() > 0 != neighbor.getLiquids().size() > 0) {
                            continue;
                        }

                        nextBlocks.remove(neighbor);
                        availableBlocks.remove(neighbor);
                        surface.add(neighbor);
                    }
                }

                edge = Slot.computeEdge(surface, positions);
                normal = new Vec(0);

                for (Position coordinates : surface) {
                    normal = normal.add(coordinates.getNormal());
                }

                normal = normal.div(surface.size());

                prevCount = count;
                count = surface.size();
            }

            if (surface.size() < slotSize) {
                continue;
            }

            Slot node = new Slot(surface, positions);
            graph.addNode(node);
        }

        for (Slot node : graph.nodes()) {
            connectNeighbors(node);
        }
    }

    private void connectNeighbors(Slot node) {
        Vec modifier = node.getNormal().mul(normalConnectivity);
        int normalOffsetI = (int) Math.round(modifier.x);
        int normalOffsetJ = (int) Math.round(modifier.z);

        for (Position coordinates : node.getConvexHull()) {
            Slot neighbor = coordinates.getSurface();

            if (neighbor != null && neighbor != node) {
                graph.putEdgeValue(node, neighbor, 1);
            }

            int i = coordinates.i + normalOffsetI;
            int j = coordinates.j + normalOffsetJ;

            if (i < 1 || i >= sizeX - 1 || j < 1 || j >= sizeZ - 1) {
                continue;
            }

            neighbor = positions[i][j].getSurface();

            if (neighbor != null && neighbor != node) {
                graph.putEdgeValue(node, neighbor, 2);
            }
        }
    }

    private void removeShortEdges() {
        double minEdgeLength = 2 * safeSlotRadius;

        boolean changed = true;

        while (changed) {
            changed = false;

            for (EndpointPair<Slot> edge : graph.edges()) {
                Slot first = edge.nodeU();
                Slot second = edge.nodeV();

                Position pos1 = first.getCenter();
                Position pos2 = second.getCenter();

                double distance = new Vec(pos1.getTerrainBlock()).sub(pos2.getTerrainBlock()).length();

                if (distance < minEdgeLength) {
                    graph.removeNode(first);
                    graph.removeNode(second);

                    Slot midpoint = new Slot(Stream.concat(Arrays.stream(first.getSurface()), Arrays.stream(second.getSurface())).collect(Collectors.toList()), positions);
                    graph.addNode(midpoint);
                    connectNeighbors(midpoint);

                    changed = true;

                    break;
                }
            }
        }
    }

    private void removeTriangles() {
        boolean changed = true;

        while (changed) {
            changed = false;

            for (EndpointPair<Slot> edge : graph.edges()) {
                Slot first = edge.nodeU();
                Slot second = edge.nodeV();

                boolean foundOtherEdge = false;

                for (Slot node : graph.adjacentNodes(first)) {
                    if (graph.adjacentNodes(node).contains(second)) {
                        foundOtherEdge = true;

                        graph.removeEdge(first, second);
                        changed = true;

                        break;
                    }
                }

                if (foundOtherEdge) {
                    break;
                }
            }
        }
    }

    private void processGraph() {
        Queue<Slot> slotsQueue = new PriorityQueue<>(graph.nodes());
        Set<Slot> houses = new HashSet<>();

        TerrainProcessor terrainProcessor = new TerrainProcessor(world, originX, originZ, positions);

        while (!slotsQueue.isEmpty()) {
            Slot slot = slotsQueue.poll();

            // TODO: Add fields

            // TODO: Add bridges

            // TODO; Add paths

            // TODO: Terrain cleanup & fix water problem

            // TODO: Don't use hardcoded house layout

            if (!slot.getCenter().containsLiquids()) {
                terrainProcessor.flatten(slot, Vec.up, 3 * safeSlotRadius / 4);
                houses.add(slot);
            }
        }

        HouseBuilder houseBuilder = new HouseBuilder(world, graph, terrainProcessor.mostCommonWoodVariant());

        for (Slot slot : houses) {
            houseBuilder.build(slot);
        }
    }

    private void debugNodes() {
        int color = 0;

        for (Slot node : graph.nodes()) {
            for (Position position : node.getSurface()) {
                world.setBlockState(position.getTerrainBlock(), Blocks.STAINED_GLASS.getDefaultState().withProperty(BlockColored.COLOR, EnumDyeColor.byMetadata(color)));
            }

            color++;
            color %= 16;
        }
    }

    private void debugEdges() {
        for (Slot slot : graph.nodes()) {
            for (int i = 0; i < 5; i++) {
                world.setBlockState(slot.getCenter().getTerrainBlock().add(0, i, 0), Blocks.REDSTONE_BLOCK.getDefaultState());
            }
        }

        for (EndpointPair<Slot> edge : graph.edges()) {
            for (Point point : new Point(edge.nodeU().getCenter()).line(edge.nodeV().getCenter())) {
                BlockPos pos = positions[(int) point.x][(int) point.y].getTerrainBlock();
                world.setBlockState(new BlockPos(pos.getX(), 100, pos.getZ()), Blocks.IRON_BLOCK.getDefaultState());
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

    public double getNormalConnectivity() {
        return normalConnectivity;
    }

    public void setNormalConnectivity(double normalConnectivity) {
        this.normalConnectivity = normalConnectivity;
    }
}
