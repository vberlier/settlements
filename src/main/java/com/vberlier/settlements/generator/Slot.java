package com.vberlier.settlements.generator;

import com.google.common.graph.ValueGraph;
import com.vberlier.settlements.util.Point;
import com.vberlier.settlements.util.Vec;
import net.minecraft.util.math.BlockPos;

import java.util.*;

public class Slot implements Comparable<Slot> {
    private final Position[] surface;
    private final Position[][] terrain;
    private final Position[] edge;
    private Vec normal;
    private Vec middle;
    private Set<Position> liquidBlocks;
    private Set<Position> vegetationBlocks;
    private int minI;
    private int minJ;
    private int maxI;
    private int maxJ;
    private int width;
    private int height;
    private Position origin;
    private Position center;
    private final Set<Position> convexHull;

    public Slot(Collection<Position> surface, Position[][] terrain) {
        this.surface = surface.toArray(new Position[0]);
        this.terrain = terrain;
        edge = computeEdge(surface, terrain).toArray(new Position[0]);

        Position first = this.surface[0];

        minI = maxI = first.i;
        minJ = maxJ = first.j;

        normal = new Vec(0);
        middle = new Vec(0);

        liquidBlocks = new HashSet<>();
        vegetationBlocks = new HashSet<>();

        for (Position pos : surface) {
            pos.setSurface(this);

            normal = normal.add(pos.getNormal());
            middle = middle.add(pos.getTerrainBlock());

            minI = Math.min(minI, pos.i);
            minJ = Math.min(minJ, pos.j);
            maxI = Math.max(maxI, pos.i);
            maxJ = Math.max(maxJ, pos.j);

            if (pos.containsLiquids()) {
                liquidBlocks.add(pos);
            } else if (pos.containsVegetation()) {
                vegetationBlocks.add(pos);
            }
        }

        normal = normal.div(surface.size());
        middle = middle.div(surface.size());

        width = maxI - minI;
        height = maxJ - minJ;

        origin = terrain[minI][minJ];
        BlockPos originBlock = origin.getTerrainBlock();

        center = terrain[(int) (middle.x - originBlock.getX() + minI)][(int) (middle.z - originBlock.getZ() + minJ)];

        convexHull = new HashSet<>();
        computeConvexHull();
    }

    public static Set<Position> computeEdge(Collection<Position> surface, Position[][] terrain) {
        Set<Position> edge = new HashSet<>();

        for (Position pos : surface) {
            for (int[] offset : Position.neighbors) {
                int i = pos.i + offset[0];
                int j = pos.j + offset[1];

                Position neighbor = terrain[i][j];

                if (!surface.contains(neighbor)) {
                    edge.add(pos);
                    break;
                }
            }
        }

        return edge;
    }

    private int relativeConvexOrientation(Position c1, Position c2, Position c3) {
        int val = (c2.j - c1.j) * (c3.i - c2.i) - (c2.i - c1.i) * (c3.j - c2.j);

        if (val == 0) {
            return 0;
        } else if (val > 0) {
            return 1;
        } else {
            return 2;
        }
    }

    private void computeConvexHull() {
        if (edge.length < 3) {
            return;
        }

        int leftmost = 0;

        for (int i = 1; i < edge.length; i++) {
            if (edge[i].i < edge[leftmost].i) {
                leftmost = i;
            }
        }

        int p = leftmost;

        do {
            int q = (p + 1) % edge.length;

            for (int i = 0; i < edge.length; i++) {
                if (relativeConvexOrientation(edge[p], edge[i], edge[q]) == 2) {
                    q = i;
                }
            }

            for (Point point : new Point(edge[p]).line(edge[q])) {
                convexHull.add(terrain[(int) point.x][(int) point.y]);
            }

            p = q;
        } while (p != leftmost);
    }

    public Vec getMiddle() {
        return middle;
    }

    public Position getOrigin() {
        return origin;
    }

    public Vec getNormal() {
        return normal;
    }

    public double getVerticality() {
        Vec line = normal.cross(Vec.up);
        return line.length() > 0.01 ? Math.pow(normal.cross(line).normalize().project(Vec.Axis.X, Vec.Axis.Z).length(), 2) : 1;
    }

    public double getDryness() {
        return 1 - (double) liquidBlocks.size() / (double) surface.length;
    }

    public Position[] getSurface() {
        return surface;
    }

    public Set<Position> getConvexHull() {
        return convexHull;
    }

    public Position getCenter() {
        return center;
    }

    public Position[] getEdge() {
        return edge;
    }

    public int getMinI() {
        return minI;
    }

    public int getMinJ() {
        return minJ;
    }

    public int getMaxI() {
        return maxI;
    }

    public int getMaxJ() {
        return maxJ;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public Set<Position> getLiquidBlocks() {
        return liquidBlocks;
    }

    public Set<Position> getVegetationBlocks() {
        return vegetationBlocks;
    }

    public Vec getOrientation(ValueGraph<Slot, Integer> graph) {
        Vec orientation = normal;

        for (Slot adjacentNode : graph.adjacentNodes(this)) {
            orientation = orientation.add(new Vec(adjacentNode.getCenter().getTerrainBlock()).sub(center.getTerrainBlock()).normalize());
        }

        return orientation.project(Vec.Axis.X, Vec.Axis.Z);
    }

    @Override
    public int compareTo(Slot o) {
        int res = Integer.compare(liquidBlocks.size(), o.liquidBlocks.size());
        if (res != 0) {
            return res;
        }

        res = Double.compare(getVerticality(), o.getVerticality());
        if (res != 0) {
            return res;
        }

        res = Integer.compare(vegetationBlocks.size(), o.vegetationBlocks.size());
        if (res != 0) {
            return res;
        }

        res = Double.compare(center.getDistanceFromCenter(), o.center.getDistanceFromCenter());
        if (res != 0) {
            return res;
        }

        return center.getTerrainBlock().compareTo(o.center.getTerrainBlock());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Slot that = (Slot) o;
        return center.equals(that.center);
    }

    @Override
    public int hashCode() {
        return Objects.hash(center);
    }
}
