package com.vberlier.settlements.generator;

import com.google.common.graph.MutableValueGraph;
import com.google.common.graph.ValueGraphBuilder;
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
    private final CoordinatesInfo[][] coordinatesInfos;
    private final int verticesSizeX;
    private final int verticesSizeZ;
    private final Vec[][] vertices;
    private final Vec[][] normals;
    private final Vec origin;
    private final Vec center;
    private final MutableValueGraph<TerrainSurface, Integer> graph;

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
        computeNodes();
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
                coordinatesInfos[i][j].setNormal(normal);
            }
        }
    }

    private void computeNodes() {
        Queue<CoordinatesInfo> queue = new PriorityQueue<>();
        Set<CoordinatesInfo> set = new HashSet<>();

        for (int i = 1; i < sizeX - 1; i++) {
            for (int j = 1; j < sizeZ - 1; j++) {
                CoordinatesInfo coordinates = coordinatesInfos[i][j];
                queue.add(coordinates);
                set.add(coordinates);
            }
        }

        int[][] offsets = new int[][]{{-1, 0}, {0, -1}, {1, 0}, {0, 1}};

        int color = 0;

        while (!queue.isEmpty()) {
            CoordinatesInfo origin = queue.poll();

            Set<CoordinatesInfo> surface = new HashSet<>();
            Set<CoordinatesInfo> edge = new HashSet<>();

            set.remove(origin);
            surface.add(origin);
            edge.add(origin);

            Vec normal = origin.getNormal();

            int prevCount = 0;
            int count = surface.size();

            while (prevCount != count && surface.size() < 200) {
                Set<CoordinatesInfo> newEdge = new HashSet<>();

                for (CoordinatesInfo coordinates : edge) {
                    boolean changed = false;

                    for (int[] offset : offsets) {
                        int i = coordinates.i + offset[0];
                        int j = coordinates.j + offset[1];

                        if (i < 1 || i >= sizeX - 1 || j < 1 || j >= sizeZ - 1) {
                            continue;
                        }

                        CoordinatesInfo neighbor = coordinatesInfos[i][j];

                        if (!set.contains(neighbor)) {
                            continue;
                        }

                        if (neighbor.getNormal().cross(normal).length() > 0.45) {
                            continue;
                        }

                        queue.remove(neighbor);
                        set.remove(neighbor);
                        surface.add(neighbor);
                        newEdge.add(neighbor);
                        changed = true;
                    }

                    if (changed) {
                        continue;
                    }

                    newEdge.add(coordinates);
                }

                edge = newEdge;

                normal = new Vec(0);

                for (CoordinatesInfo coordinates : surface) {
                    normal = normal.add(coordinates.getNormal());
                }

                normal = normal.div(surface.size());

                prevCount = count;
                count = surface.size();
            }

            if (surface.size() < 200) {
                continue;
            }

            TerrainSurface node = new TerrainSurface(normal, surface, coordinatesInfos);

            for (CoordinatesInfo coordinates : node.getEdge()) {
                world.setBlockState(coordinates.getTerrainBlock().add(0, 1, 0), Blocks.WOOL.getDefaultState().withProperty(BlockColored.COLOR, EnumDyeColor.byMetadata(color)));
            }

            for (int i = 0; i < 5; i++) {
                world.setBlockState(node.getNormal().mul(i).add(node.getOrigin().getTerrainBlock()).block(), Blocks.REDSTONE_BLOCK.getDefaultState());
            }

            color++;
            color %= 16;
        }
    }
}
