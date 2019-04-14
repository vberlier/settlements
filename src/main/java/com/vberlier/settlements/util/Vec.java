package com.vberlier.settlements.util;

import com.google.common.base.MoreObjects;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.BlockPos;

import java.util.*;

public class Vec implements Comparable<Vec> {
    public enum Direction {
        UP, DOWN, EAST, WEST, SOUTH, NORTH;

        public Vec vec() {
            switch (this) {
                case UP:
                    return Vec.up;
                case DOWN:
                    return Vec.down;
                case EAST:
                    return Vec.east;
                case WEST:
                    return Vec.west;
                case SOUTH:
                    return Vec.south;
                default:
                    return Vec.north;
            }
        }

        public Rotation rotation() {
            switch (this) {
                case EAST:
                    return Rotation.NONE;
                case WEST:
                    return Rotation.CLOCKWISE_180;
                case SOUTH:
                    return Rotation.CLOCKWISE_90;
                default:
                    return Rotation.COUNTERCLOCKWISE_90;
            }
        }
    }

    public enum Axis {
        X, Y, Z;

        public Direction direction() {
            switch (this) {
                case X:
                    return Direction.EAST;
                case Y:
                    return Direction.UP;
                default:
                    return Direction.SOUTH;
            }
        }

        public Vec vec() {
            return direction().vec();
        }

        public Rotation rotation() {
            return direction().rotation();
        }
    }

    public static final Vec zero = new Vec(0);
    public static final Vec one = new Vec(1);

    public static final Vec up = new Vec(0, 1, 0);
    public static final Vec down = new Vec(0, -1, 0);
    public static final Vec east = new Vec(1, 0, 0);
    public static final Vec west = new Vec(-1, 0, 0);
    public static final Vec south = new Vec(0, 0, 1);
    public static final Vec north = new Vec(0, 0, -1);

    public double x;
    public double y;
    public double z;

    public Vec(BlockPos pos) {
        this(pos.getX(), pos.getY(), pos.getZ());
    }

    public Vec(double n) {
        this(n, n, n);
    }

    public Vec(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public Vec add(Vec v) {
        return new Vec(x + v.x, y + v.y, z + v.z);
    }

    public Vec add(double x, double y, double z) {
        return add(new Vec(x, y, z));
    }

    public Vec add(BlockPos pos) {
        return add(pos.getX(), pos.getY(), pos.getZ());
    }

    public Vec add(double n) {
        return add(new Vec(n));
    }

    public Vec sub(Vec v) {
        return new Vec(x - v.x, y - v.y, z - v.z);
    }

    public Vec sub(double x, double y, double z) {
        return sub(new Vec(x, y, z));
    }

    public Vec sub(BlockPos pos) {
        return sub(pos.getX(), pos.getY(), pos.getZ());
    }

    public Vec sub(double n) {
        return sub(new Vec(n));
    }

    public Vec mul(Vec v) {
        return new Vec(x * v.x, y * v.y, z * v.z);
    }

    public Vec mul(double x, double y, double z) {
        return mul(new Vec(x, y, z));
    }

    public Vec mul(BlockPos pos) {
        return mul(pos.getX(), pos.getY(), pos.getZ());
    }

    public Vec mul(double n) {
        return mul(new Vec(n));
    }

    public Vec div(Vec v) {
        return new Vec(x / v.x, y / v.y, z / v.z);
    }

    public Vec div(double x, double y, double z) {
        return div(new Vec(x, y, z));
    }

    public Vec div(BlockPos pos) {
        return div(pos.getX(), pos.getY(), pos.getZ());
    }

    public Vec div(double n) {
        return div(new Vec(n));
    }

    public double dot(Vec v) {
        return x * v.x + y * v.y + z * v.z;
    }

    public double dot(double x, double y, double z) {
        return dot(new Vec(x, y, z));
    }

    public double dot(BlockPos pos) {
        return dot(pos.getX(), pos.getY(), pos.getZ());
    }

    public double dot(double n) {
        return dot(new Vec(n));
    }

    public Vec cross(Vec v) {
        return new Vec(y * v.z - z * v.y, z * v.x - x * v.z, x * v.y - y * v.x);
    }

    public Vec cross(double x, double y, double z) {
        return cross(new Vec(x, y, z));
    }

    public Vec cross(BlockPos pos) {
        return cross(pos.getX(), pos.getY(), pos.getZ());
    }

    public Vec cross(double n) {
        return cross(new Vec(n));
    }

    public BlockPos block() {
        return new BlockPos(Math.round(x), Math.round(y), Math.round(z));
    }

    public double length() {
        return Math.sqrt(x * x + y * y + z * z);
    }

    public Vec normalize() {
        double length = length();

        if (length == 0) {
            return new Vec(0);
        }

        return new Vec(x / length, y / length, z / length);
    }

    public Direction direction() {
        double absX = Math.abs(x);
        double absY = Math.abs(y);
        double absZ = Math.abs(z);

        if (absY > absX && absY > absZ) {
            return y > 0 ? Direction.UP : Direction.DOWN;
        }

        if (absX > absZ) {
            return x > 0 ? Direction.EAST : Direction.WEST;
        } else {
            return z > 0 ? Direction.SOUTH : Direction.NORTH;
        }
    }

    public Rotation rotation() {
        return direction().rotation();
    }

    public Axis axis() {
        Direction direction = direction();

        switch (direction) {
            case UP:
            case DOWN:
                return Axis.Y;
            case EAST:
            case WEST:
                return Axis.X;
            default:
                return Axis.Z;
        }
    }

    public Vec project(Axis... axis) {
        Vec factor = new Vec(0);

        for (Axis a : axis) {
            switch (a) {
                case X:
                    factor.x = 1;
                    break;
                case Y:
                    factor.y = 1;
                    break;
                default:
                    factor.z = 1;
                    break;
            }
        }

        return mul(factor);
    }

    public Vec inverse() {
        return mul(-1);
    }

    public Vec rotateY(double angle) {
        double cos = Math.cos(-angle);
        double sin = Math.sin(-angle);
        return new Vec(x * cos - z * sin, y, x * sin + z * cos);
    }

    public double angleY() {
        Vec projection = project(Axis.X, Axis.Z);
        return -Math.acos(projection.dot(Vec.south) / projection.length());
    }

    public static Vec average(Vec... vectors) {
        return Arrays.stream(vectors).reduce(new Vec(0), Vec::add).div(vectors.length);
    }

    public static Vec average(BlockPos... positions) {
        return new Vec(Arrays.stream(positions).reduce(new BlockPos(0, 0, 0), BlockPos::add)).div(positions.length);
    }

    public static Vec normal(Vec v1, Vec v2, Vec v3) {
        return v3.sub(v2).cross(v1.sub(v2));
    }

    public static Vec normal(Vec v1, Vec v2, Vec v3, Vec v4) {
        return Vec.average(normal(v1, v2, v3), normal(v1, v3, v4));
    }

    public static Vec planeLineIntersection(Vec planePoint, Vec planeNormal, Vec linePoint, Vec lineDirection) {
        double denominator = planeNormal.dot(lineDirection);

        if (Math.abs(denominator) < 0.01) {
            return null;
        }

        double t = (planeNormal.dot(planePoint) - planeNormal.dot(linePoint)) / denominator;
        return linePoint.add(lineDirection.mul(t));
    }

    public static ArrayList<Vec> interpolateBezier(Vec p1, Vec p2, Vec p3, Vec p4, double precision) {
        ArrayList<Vec> result = new ArrayList<>();

        double distance = p1.sub(p2).length() + p2.sub(p3).length() + p3.sub(p4).length();
        double step = (1 / precision) / distance;

        for (double t = 0; t < 1; t += step) {
            double a = Math.pow(1 - t, 3);
            double b = 3 * Math.pow(1 - t, 2) * t;
            double c = 3 * (1 - t) * Math.pow(t, 2);
            double d = Math.pow(t, 3);

            result.add(p1.mul(a).add(p2.mul(b)).add(p3.mul(c)).add(p4.mul(d)));
        }

        return result;
    }

    public static ArrayList<Vec> interpolateDisk(Vec origin, Vec normal, double radius, double precision) {
        ArrayList<Vec> result = new ArrayList<>();

        Vec x = normal.cross(origin).normalize();
        Vec z = normal.cross(x).normalize();

        double step = 1 / precision;

        for (double i = -radius; i <= radius; i += step) {
            for (double j = -radius; j <= radius; j += step) {
                Vec point = origin.add(x.mul(i)).add(z.mul(j));

                if (origin.sub(point).length() < radius) {
                    result.add(point);
                }
            }
        }

        return result;
    }

    @Override
    public int compareTo(Vec o) {
        int res = Double.compare(y, o.y);
        if (res != 0) {
            return res;
        }

        res = Double.compare(x, o.x);
        if (res != 0) {
            return res;
        }

        return Double.compare(z, o.z);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Vec vec = (Vec) o;
        return Double.compare(vec.x, x) == 0 &&
                Double.compare(vec.y, y) == 0 &&
                Double.compare(vec.z, z) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y, z);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this).add("x", x).add("y", y).add("z", z).toString();
    }
}
