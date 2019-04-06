package com.vberlier.settlements.generator;

import com.vberlier.settlements.util.Point;
import com.vberlier.settlements.util.Vec;
import net.minecraft.util.math.BlockPos;

import java.util.*;

public class TerrainSurface {
    private final Vec normal;
    private final CoordinatesInfo[] surfaceCoordinates;
    private final CoordinatesInfo[] edge;
    private final CoordinatesInfo[][] terrainCoordinates;
    private Vec middle;
    private int minI;
    private int minJ;
    private int maxI;
    private int maxJ;
    private int width;
    private int height;
    private CoordinatesInfo origin;
    private CoordinatesInfo center;
    private final Set<CoordinatesInfo> convexHull;

    public TerrainSurface(Vec normal, Collection<CoordinatesInfo> surfaceCoordinates, Collection<CoordinatesInfo> edge, CoordinatesInfo[][] terrainCoordinates) {
        this.normal = normal;
        this.surfaceCoordinates = surfaceCoordinates.toArray(new CoordinatesInfo[0]);
        this.edge = edge.toArray(new CoordinatesInfo[0]);
        this.terrainCoordinates = terrainCoordinates;

        CoordinatesInfo first = this.surfaceCoordinates[0];

        minI = maxI = first.i;
        minJ = maxJ = first.j;

        middle = new Vec(0);

        for (CoordinatesInfo coordinates : surfaceCoordinates) {
            coordinates.setSurface(this);

            middle = middle.add(coordinates.getTerrainBlock());

            minI = Math.min(minI, coordinates.i);
            minJ = Math.min(minJ, coordinates.j);
            maxI = Math.max(maxI, coordinates.i);
            maxJ = Math.max(maxJ, coordinates.j);
        }

        middle = middle.div(surfaceCoordinates.size());

        width = maxI - minI;
        height = maxJ - minJ;

        origin = terrainCoordinates[minI][minJ];
        BlockPos originBlock = origin.getTerrainBlock();

        center = terrainCoordinates[(int) (middle.x - originBlock.getX() + minI)][(int) (middle.z - originBlock.getZ() + minJ)];

        convexHull = new HashSet<>();
        computeConvexHull();
    }

    private int orientation(CoordinatesInfo c1, CoordinatesInfo c2, CoordinatesInfo c3) {
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
                convexHull.add(terrainCoordinates[(int) point.x][(int) point.y]);
            }

            p = q;
        } while (p != leftmost);
    }

    public Vec getMiddle() {
        return middle;
    }

    public CoordinatesInfo getOrigin() {
        return origin;
    }

    public Vec getNormal() {
        return normal;
    }

    public CoordinatesInfo[] getSurfaceCoordinates() {
        return surfaceCoordinates;
    }

    public Set<CoordinatesInfo> getConvexHull() {
        return convexHull;
    }

    public CoordinatesInfo getCenter() {
        return center;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TerrainSurface that = (TerrainSurface) o;
        return center.equals(that.center);
    }

    @Override
    public int hashCode() {
        return Objects.hash(center);
    }

    public CoordinatesInfo[] getEdge() {
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
}
