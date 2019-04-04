package com.vberlier.settlements.util;

import net.minecraft.util.math.BlockPos;

import java.util.Arrays;

public class Vec {
    public static final Vec up = new Vec(0, 1, 0);

    public final double x;
    public final double y;
    public final double z;

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
        return new Vec(x / length, y / length, z / length);
    }

    public static Vec average(Vec ...vectors) {
        return Arrays.stream(vectors).reduce(new Vec(0), Vec::add).div(vectors.length);
    }

    public static Vec average(BlockPos ...positions) {
        return new Vec(Arrays.stream(positions).reduce(new BlockPos(0, 0, 0), BlockPos::add)).div(positions.length);
    }

    public static Vec normal(Vec v1, Vec v2, Vec v3) {
        return v3.sub(v2).cross(v1.sub(v2));
    }

    public static Vec normal(Vec v1, Vec v2, Vec v3, Vec v4) {
        return Vec.average(normal(v1, v2, v3), normal(v1, v3, v4));
    }
}
