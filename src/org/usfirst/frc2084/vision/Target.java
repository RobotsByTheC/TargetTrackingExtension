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

import java.awt.Polygon;
import java.awt.Rectangle;
import static org.usfirst.frc2084.vision.ScoreUtils.ratioToScore;

/**
 *
 * @author Ben Wolsieffer
 */
public class Target {

    private static final int NUM_SCORES = 2;

    public static final double STATIC_TARGET_ASPECT_RATIO = 4.0 / 32.0;
    public static final double HOT_TARGET_ASPECT_RATIO = 23.5 / 4.0;
    public static final double MIN_AREA = 100;

    public static final double MIN_RECTANGULARITY_SCORE = 10;
    public static final double MIN_ASPECT_RATIO_SCORE = 10;

    private final Polygon shape;
    private final Rectangle rect;
    private double score = -1;
    private boolean valid = true;

    public Target(Polygon p) {
        shape = p;
        rect = p.getBounds();

        score = calculateScore();
    }

    private double getPolygonArea(Polygon p) {
        int i, j;
        double area = 0;
        for (i = 0; i < p.npoints; i++) {
            j = (i + 1) % p.npoints;
            area += p.xpoints[i] * p.ypoints[j];
            area -= p.ypoints[i] * p.xpoints[j];
        }
        area /= 2;
        return (area < 0 ? -area : area);
    }

    public double getScore() {
        return score;
    }

    private double calculateScore() {
        double lScore
                = (scoreRectangularity()
                + scoreAspectRatio()) / NUM_SCORES;
        return isValid() ? lScore : 0;
    }

    private double scoreRectangularity() {
        double polyArea = getPolygonArea(shape);
        if (polyArea < MIN_AREA) {
            invalidate();
        }
        double lScore = ratioToScore(polyArea / (rect.width * rect.height));
        if (lScore < MIN_RECTANGULARITY_SCORE) {
            invalidate();
        }
        return lScore;
    }

    private double scoreAspectRatio() {
        double ratio = (double) rect.width / (double) rect.height;
        double ideal = isVertical() ? STATIC_TARGET_ASPECT_RATIO : HOT_TARGET_ASPECT_RATIO;
        double lScore = ratioToScore(ratio / ideal);
        if (lScore < MIN_ASPECT_RATIO_SCORE) {
            invalidate();
        }
        return lScore;
    }

    public Polygon getShape() {
        return shape;
    }

    public Rectangle getRect() {
        return rect;
    }

    private void invalidate() {
        valid = false;
    }

    public boolean isValid() {
        return valid;
    }

    public boolean isVertical() {
        return rect.width < rect.height;
    }
}
