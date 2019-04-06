package com.vberlier.settlements.generator;

import com.vberlier.settlements.util.Vec;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

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
    private final Set<CoordinatesInfo> hull;

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

        hull = new HashSet<>();
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

            edgeLine(edge[p], edge[q]);

            p = q;
        } while (p != leftmost);
    }

    private void edgeLine(CoordinatesInfo origin, CoordinatesInfo endpoint) {
        double x0 = origin.i;
        double y0 = origin.j;
        double x1 = endpoint.i;
        double y1 = endpoint.j;

        double dx = Math.abs(x1 - x0);
        double dy = Math.abs(y1 - y0);

        double sx = x0 < x1 ? 1 : -1;
        double sy = y0 < y1 ? 1 : -1;

        double err = (dx > dy ? dx : -dy) / 2;

        while (true) {
            hull.add(terrainCoordinates[(int) x0][(int) y0]);

            if (x0 == x1 && y0 == y1) {
                break;
            }

            double e2 = err;

            if (e2 > -dx) {
                err -= dy;
                x0 += sx;
            }

            if (e2 < dy) {
                err += dx;
                y0 += sy;
            }
        }
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

    public Set<CoordinatesInfo> getHull() {
        return hull;
    }

    public CoordinatesInfo getCenter() {
        return center;
    }
}
