package com.vberlier.settlements.generator;

import com.google.common.graph.ValueGraph;
import com.vberlier.settlements.SettlementsMod;
import com.vberlier.settlements.util.ConvexHull;
import com.vberlier.settlements.util.Point;
import com.vberlier.settlements.util.Vec;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.gen.structure.StructureBoundingBox;
import org.apache.logging.log4j.Logger;

import java.util.*;

public class Slot implements Comparable<Slot> {
    private final Logger logger;

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
    private BlockPos anchor;
    private ArrayList<StructureBoundingBox> hitboxes;

    public Slot(Collection<Position> surface, Position[][] terrain) {
        logger = SettlementsMod.instance.getLogger();

        this.surface = surface.toArray(new Position[0]);
        this.terrain = terrain;

        logger.info("Computing surface edge...");
        edge = computeEdge(surface, terrain).toArray(new Position[0]);
        logger.info("Edge length: " + edge.length);

        Position first = this.surface[0];

        minI = maxI = first.i;
        minJ = maxJ = first.j;

        normal = new Vec(0);
        middle = new Vec(0);

        liquidBlocks = new HashSet<>();
        vegetationBlocks = new HashSet<>();

        logger.info("Computing surface normal and bounding box...");

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

        logger.info("normal: " + normal);
        logger.info("middle: " + middle);

        width = maxI - minI;
        height = maxJ - minJ;

        origin = terrain[minI][minJ];
        BlockPos originBlock = origin.getTerrainBlock();

        logger.info("originBlock: " + originBlock);
        logger.info("width: " + width);
        logger.info("height: " + height);

        center = terrain[(int) (middle.x - originBlock.getX() + minI)][(int) (middle.z - originBlock.getZ() + minJ)];

        convexHull = new HashSet<>();
        computeConvexHull();

        anchor = center.getTerrainBlock();
        hitboxes = new ArrayList<>();
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

    private void computeConvexHull() {
        logger.info("Computing convex hull...");

        if (edge.length < 3) {
            return;
        }

        List<Position> hull = ConvexHull.fromEdge(edge, terrain);

        Position previous = hull.get(0);

        for (int i = 1; i < hull.size(); i++) {
            Position current = hull.get(i);

            for (Point point : new Point(previous).line(current)) {
                convexHull.add(terrain[(int) point.x][(int) point.y]);
            }
        }
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
        return Math.pow(1 - (double) liquidBlocks.size() / (double) surface.length, 2);
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

    public Vec getOrientation(ValueGraph<Slot, Vec> graph) {
        Vec orientation = normal;

        for (Slot adjacentNode : graph.adjacentNodes(this)) {
            orientation = orientation.add(new Vec(adjacentNode.getCenter().getTerrainBlock()).sub(center.getTerrainBlock()).normalize());
        }

        orientation = orientation.project(Vec.Axis.X, Vec.Axis.Z).normalize();

        return orientation.length() == 0 ? Vec.east : orientation;
    }

    public Vec edgeConnection(Slot other) {
        return new Vec(center.getTerrainBlock()).sub(other.center.getTerrainBlock());
    }

    public void setAnchor(BlockPos anchor) {
        this.anchor = anchor;
    }

    public BlockPos getAnchor() {
        return anchor;
    }

    public void addHitboxes(StructureBoundingBox... boxes) {
        for (StructureBoundingBox box : boxes) {
            addHitbox(box);
        }
    }

    public void addHitbox(StructureBoundingBox box) {
        hitboxes.add(box);
    }

    public ArrayList<StructureBoundingBox> getHitboxes() {
        return hitboxes;
    }

    public boolean contains(BlockPos pos) {
        for (StructureBoundingBox hitbox : hitboxes) {
            if (hitbox.isVecInside(pos)) {
                return true;
            }
        }
        return false;
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
