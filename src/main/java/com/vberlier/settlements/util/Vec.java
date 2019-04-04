package com.vberlier.settlements.util;

import net.minecraft.util.math.BlockPos;

import java.util.Arrays;

public class Vec {
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

    public Vec sub(Vec v) {
        return new Vec(x - v.x, y - v.y, z - v.z);
    }

    public Vec sub(double x, double y, double z) {
        return sub(new Vec(x, y, z));
    }

    public Vec mul(Vec v) {
        return new Vec(x * v.x, y * v.y, z * v.z);
    }

    public Vec mul(double x, double y, double z) {
        return mul(new Vec(x, y, z));
    }

    public Vec div(Vec v) {
        return new Vec(x / v.x, y / v.y, z / v.z);
    }

    public Vec div(double x, double y, double z) {
        return div(new Vec(x, y, z));
    }

    public double dot(Vec v) {
        return x * v.x + y * v.y + z * v.z;
    }

    public double dot(double x, double y, double z) {
        return dot(new Vec(x, y, z));
    }

    public Vec cross(Vec v) {
        return new Vec(y * v.z - z * v.y, z * v.x - x * v.z, x * v.y - y * v.x);
    }

    public Vec cross(double x, double y, double z) {
        return cross(new Vec(x, y, z));
    }

    public BlockPos block() {
        return new BlockPos(x, y, z);
    }

    public static Vec average(Vec ...vectors) {
        return Arrays.stream(vectors).reduce(new Vec(0), Vec::add).div(new Vec(vectors.length));
    }
}
