package com.vberlier.settlements.generator;

import com.vberlier.settlements.util.Point;
import com.vberlier.settlements.util.Vec;
import net.minecraft.util.math.BlockPos;

import java.util.*;

public class Slot {
    private final Position[] surface;
    private final Position[] edge;
    private final Position[][] terrain;
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

    public Slot(Collection<Position> surface, Collection<Position> edge, Position[][] terrain) {
        this.surface = surface.toArray(new Position[0]);
        this.edge = edge.toArray(new Position[0]);
        this.terrain = terrain;

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

    private int orientation(Position c1, Position c2, Position c3) {
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
                if (orientation(edge[p], edge[i], edge[q]) == 2) {
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

    public Position[] getSurface() {
        return surface;
    }

    public Set<Position> getConvexHull() {
        return convexHull;
    }

    public Position getCenter() {
        return center;
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
}
