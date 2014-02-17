/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 *//*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.usfirst.frc2084.vision;

import java.awt.Point;
import java.awt.Polygon;

/**
 * Thus is a utility class for performing useful functions for manipulating and
 * finding information about polygons, including computing areas, centroids,
 * testing whether a point is inside or outside of a polygon, and for rotating a
 * poloygon by a given angle.
 *
 * <b>See Also</b> org.shodor.math11.Line
 */
public class PolygonUtils {

    /**
     * Computes the area of any two-dimensional polygon.
     *
     * @param p The polygon to compute the area of input as an array of points
     *
     * @return The area of the polygon.
     */
    public static double getPolygonArea(Polygon p) {
        int i, j;
        double area = 0;
        for (i = 0; i < p.npoints; i++) {
            j = (i + 1) % p.npoints;
            area += p.xpoints[i] * p.ypoints[j];
            area -= p.ypoints[i] * p.xpoints[j];
        }
        area /= 2.0;
        return (Math.abs(area));
    }

    /**
     * Finds the centroid of a polygon with integer verticies.
     *
     * @param pg The polygon to find the centroid of.
     * @return The centroid of the polygon.
     */
    public static Point getCentroid(Polygon pg) {

        if (pg == null) {
            return null;
        }

        int N = pg.npoints;
        Point[] polygon = new Point[N];

        for (int q = 0; q < N; q++) {
            polygon[q] = new Point(pg.xpoints[q], pg.ypoints[q]);
        }

        double cx = 0, cy = 0;
        double A = getPolygonArea(pg);
        int i, j;

        double factor;
        for (i = 0; i < N; i++) {
            j = (i + 1) % N;
            factor = (polygon[i].x * polygon[j].y - polygon[j].x * polygon[i].y);
            cx += (polygon[i].x + polygon[j].x) * factor;
            cy += (polygon[i].y + polygon[j].y) * factor;
        }
        factor = 1.0 / (6.0 * A);
        cx *= factor;
        cy *= factor;
        return new Point((int) Math.abs(Math.round(cx)), (int) Math.abs(Math
                .round(cy)));
    }

    /**
     * Rotates the polygon and returns it's center of mass.
     *
     * @param pg The polygon to rotate around it's center of mass. Vertices are
     * integer points. Used mainly for physical coordinate rotation.
     * @param rotAngle rotation angle in radians
     * @param rotationPoint the point to rotate around
     */
    public static void rotatePolygon(Polygon pg, double rotAngle, Point rotationPoint) {

        double x, y;
        for (int i = 0; i < pg.npoints; i++) {
            x = pg.xpoints[i] - rotationPoint.x;
            y = pg.ypoints[i] - rotationPoint.y;
            pg.xpoints[i] = rotationPoint.x
                    + (int) Math.round(((x * Math.cos(rotAngle)) - (y * Math
                            .sin(rotAngle))));
            pg.ypoints[i] = rotationPoint.y
                    + (int) Math.round(((x * Math.sin(rotAngle)) + (y * Math
                            .cos(rotAngle))));
        }
    }

    public static Point rotatePolygon(Polygon pg, double rotAngle) {
        Point centroid = getCentroid(pg);
        rotatePolygon(pg, rotAngle, centroid);
        return centroid;
    }

    public static double angle(Point p1, Point p2) {
        double dtheta = Math.atan2(p2.y, p2.x) - Math.atan2(p1.y, p1.x);
        while (dtheta > Math.PI) {
            dtheta -= 2.0 * Math.PI;
        }
        while (dtheta < -1.0 * Math.PI) {
            dtheta += 2.0 * Math.PI;
        }
        return dtheta;
    }

}
