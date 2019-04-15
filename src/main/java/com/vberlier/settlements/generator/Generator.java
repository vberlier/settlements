package com.vberlier.settlements.generator;

import com.google.common.graph.EndpointPair;
import com.google.common.graph.MutableValueGraph;
import com.google.common.graph.ValueGraphBuilder;
import com.vberlier.settlements.SettlementsMod;
import com.vberlier.settlements.util.Point;
import com.vberlier.settlements.util.Vec;
import net.minecraft.block.Block;
import net.minecraft.block.BlockColored;
import net.minecraft.block.BlockHugeMushroom;
import net.minecraft.block.BlockPlanks;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.gen.structure.StructureBoundingBox;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Generator {
    private final Logger logger;

    private final World world;
    private final StructureBoundingBox boundingBox;
    private final int originX;
    private final int originZ;
    private final int sizeX;
    private final int sizeZ;
    private final double radius;
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
    private final MutableValueGraph<Slot, Vec> graph;

    private int slotSize = 300;
    private double slotFlexibility = 0.45;
    private double normalConnectivity = 16;
    private double safeSlotRadius = 0.75 * Math.sqrt(slotSize / Math.PI);

    public Generator(World world, StructureBoundingBox boundingBox) {
        logger = SettlementsMod.instance.getLogger();

        logger.info("Setting up generator...");

        this.world = world;
        this.boundingBox = boundingBox;

        originX = boundingBox.minX;
        originZ = boundingBox.minZ;
        sizeX = boundingBox.getXSize();
        sizeZ = boundingBox.getZSize();
        radius = (sizeX + sizeZ) / 4.0;

        logger.info("sizeX: " + sizeX);
        logger.info("sizeZ: " + sizeZ);
        logger.info("radius: " + radius);

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

        logger.info("origin: " + origin);
        logger.info("center: " + center);

        graph = ValueGraphBuilder.undirected().build();

        logger.info("Generator ready");
    }

    public void buildSettlement() {
        computeMaps();
        computeVertices();
        computeNormals();
        computeSlotGraph();
        removeShortEdges();
        removeTriangles();
        processGraph();
    }

    private void computeMaps() {
        logger.info("Computing heightMap and terrainMap...");

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

                    if (block.isAir(state, world, pos) || block.isPassable(world, pos) || block.isFlammable(world, pos, EnumFacing.UP) || block instanceof BlockHugeMushroom) {
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
        logger.info("Computing terrain vertices...");

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
        logger.info("Computing terrain normals...");

        for (int i = 1; i < sizeX - 1; i++) {
            for (int j = 1; j < sizeZ - 1; j++) {
                Vec normal = Vec.normal(vertices[i - 1][j - 1], vertices[i - 1][j], vertices[i][j], vertices[i][j - 1]).normalize();
                normals[i][j] = normal;
                positions[i][j].setNormal(normal);
            }
        }
    }

    private void computeSlotGraph() {
        logger.info("Creating graph...");

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
            logger.info("Attempting to create a new slot...");

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
                logger.info("Too small");
                continue;
            }


            logger.info("Creating new node...");
            Slot node = new Slot(surface, positions);

            logger.info("Adding new node to the graph...");
            graph.addNode(node);
        }

        for (Slot node : graph.nodes()) {
            connectNeighbors(node);
        }
    }

    private void connectNeighbors(Slot node) {
        logger.info("Connecting neighbors of node at " + node.getCenter().getTerrainBlock());

        Vec modifier = node.getNormal().mul(normalConnectivity);
        int normalOffsetI = (int) Math.round(modifier.x);
        int normalOffsetJ = (int) Math.round(modifier.z);

        for (Position coordinates : node.getConvexHull()) {
            Slot neighbor = coordinates.getSurface();

            if (neighbor != null && neighbor != node) {
                graph.putEdgeValue(node, neighbor, node.edgeConnection(neighbor));
            }

            int i = coordinates.i + normalOffsetI;
            int j = coordinates.j + normalOffsetJ;

            if (i < 1 || i >= sizeX - 1 || j < 1 || j >= sizeZ - 1) {
                continue;
            }

            neighbor = positions[i][j].getSurface();

            if (neighbor != null && neighbor != node) {
                graph.putEdgeValue(node, neighbor, node.edgeConnection(neighbor));
            }
        }
    }

    private void removeShortEdges() {
        logger.info("Removing short edges...");

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

                if (distance < minEdgeLength || (distance < 1.8 * minEdgeLength && nearTheEdge(pos1) && nearTheEdge(pos2))) {
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

    private boolean nearTheEdge(Position position) {
        double threshold = radius / 2;

        BlockPos block = position.getTerrainBlock();

        return block.getX() - originX < threshold
                || block.getZ() - originZ < threshold
                || originX + sizeX - block.getX() < threshold
                || originZ + sizeZ - block.getZ() < threshold;
    }

    private void removeTriangles() {
        logger.info("Removing triangles...");

        Queue<EndpointPair<Slot>> edgeQueue = new PriorityQueue<>((a, b) -> Double.compare(graph.edgeValue(b.nodeU(), b.nodeV()).length(), graph.edgeValue(a.nodeU(), a.nodeV()).length()));
        edgeQueue.addAll(graph.edges());

        while (!edgeQueue.isEmpty()) {
            EndpointPair<Slot> edge = edgeQueue.poll();
            Slot first = edge.nodeU();
            Slot second = edge.nodeV();

            for (Slot node : graph.adjacentNodes(first)) {
                if (graph.adjacentNodes(node).contains(second)) {
                    graph.removeEdge(first, second);
                    break;
                }
            }
        }
    }

    private void processGraph() {
        logger.info("Processing graph...");

        Queue<Slot> slotsQueue = new PriorityQueue<>(graph.nodes());

        Set<Slot> houses = new HashSet<>();
        HashMap<Slot, Set<Slot>> fieldsMap = new HashMap<>();

        TerrainProcessor terrainProcessor = new TerrainProcessor(world, originX, originZ, positions);

        while (!slotsQueue.isEmpty()) {
            Slot slot = slotsQueue.poll();

            logger.info("Classifying node at " + slot.getCenter().getTerrainBlock());

            // TODO: Add bridges

            // TODO; Add paths

            // TODO: Fix water problem

            // TODO: Fix problem with large selections

            if (slot.getCenter().getLiquids().isEmpty()) {
                if (
                        FieldBuilder.canReplaceWithFarmland(world.getBlockState(slot.getCenter().getTerrainBlock()).getBlock())
                        && slot.getNormal().project(Vec.Axis.Y).length() > 0.9
                        && Math.sin(world.rand.nextInt((int) Math.abs(slot.getCenter().getDistanceFromCenter()) + 1) / radius * Math.PI / 2) > 0.4
                ) {
                    logger.info("Registering as a field");

                    Set<Slot> adjacentFields = graph.adjacentNodes(slot).stream()
                            .filter(fieldsMap::containsKey)
                            .collect(Collectors.toSet());

                    Set<Slot> newSet = adjacentFields.stream()
                            .flatMap(adjacent -> fieldsMap.containsKey(adjacent) ? fieldsMap.get(adjacent).stream() : Stream.of(adjacent))
                            .collect(Collectors.toSet());

                    newSet.add(slot);

                    for (Slot fieldSlot : newSet) {
                        fieldsMap.put(fieldSlot, newSet);
                    }
                } else {
                    logger.info("Registering as a house");
                    houses.add(slot);
                }

                logger.info("Flattening terrain");
                terrainProcessor.flatten(slot, Vec.up, 3 * safeSlotRadius / 4);
            } else {
                logger.info("Ignoring because of water");
            }
        }

        logger.info("Removing isolated fields...");

        fieldsMap.entrySet().removeIf(entry -> {
            if (entry.getValue().size() < 2) {
                logger.info("Turning field at " + entry.getKey().getCenter().getTerrainBlock() + " into a house");
                houses.add(entry.getKey());
                return true;
            }
            return false;
        });

        BlockPlanks.EnumType woodVariant = terrainProcessor.mostCommonWoodVariant();
        logger.info("Most common wood variant harvested: " + woodVariant);

        PathBuilder pathBuilder = new PathBuilder(world, positions, originX, originZ, sizeX, sizeZ, woodVariant);
        HouseBuilder houseBuilder = new HouseBuilder(world, graph, woodVariant, pathBuilder);

        logger.info("Building houses...");

        for (Slot slot : houses) {
            logger.info("Building house at " + slot.getCenter().getTerrainBlock());
            houseBuilder.build(slot, 5 * safeSlotRadius / 3);
        }

        FieldBuilder fieldBuilder = new FieldBuilder(world, graph, positions, woodVariant);

        logger.info("Building fields...");

        for (Set<Slot> slots : new HashSet<>(fieldsMap.values())) {
            logger.info("Building group of " + slots.size() + " fields");
            fieldBuilder.build(slots);
        }

        logger.info("Building paths...");

        pathBuilder.setHitboxes(graph.nodes().stream().flatMap(slot -> slot.getHitboxes().stream()).collect(Collectors.toList()));
        pathBuilder.computeBlocks();

        for (EndpointPair<Slot> edge : graph.edges()) {
            logger.info("Building path from " + edge.nodeU().getAnchor() + " to " + edge.nodeV().getAnchor());
            pathBuilder.build(edge.nodeU(), edge.nodeV());
        }
    }

    private void debugNodes() {
        logger.info("Debugging nodes");

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
        logger.info("Debugging edges");

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
