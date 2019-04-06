package com.vberlier.settlements.util;

import com.vberlier.settlements.generator.CoordinatesInfo;

import java.util.ArrayList;
import java.util.Objects;

public class Point implements Comparable<Point> {
    public final double x;
    public final double y;

    public Point(CoordinatesInfo coordinates) {
        this(coordinates.i, coordinates.j);
    }

    public Point(double n) {
        this(n, n);
    }

    public Point(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public ArrayList<Point> line(CoordinatesInfo other) {
        return line(new Point(other));
    }

    public ArrayList<Point> line(Point other) {
        ArrayList<Point> result = new ArrayList<>();

        double x0 = x;
        double y0 = y;
        double x1 = other.x;
        double y1 = other.y;

        double dx = Math.abs(x1 - x0);
        double dy = Math.abs(y1 - y0);

        double sx = x0 < x1 ? 1 : -1;
        double sy = y0 < y1 ? 1 : -1;

        double err = (dx > dy ? dx : -dy) / 2;

        while (true) {
            result.add(new Point(x0, y0));

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

        return result;
    }

    @Override
    public int compareTo(Point o) {
        int res = Double.compare(x, o.x);
        if (res != 0) {
            return res;
        }

        return Double.compare(y, o.y);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Point point = (Point) o;
        return Double.compare(point.x, x) == 0 &&
                Double.compare(point.y, y) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y);
    }
}
