/* 
 * Copyright (c) 2014 RobotsByTheC. All rights reserved.
 *
 * Open Source Software - may be modified and shared by FRC teams. The code must
 * be accompanied by the BSD license file in the root directory of the project.
 */
package org.usfirst.frc2084.vision;

import java.awt.Polygon;
import java.awt.Rectangle;
import static org.usfirst.frc2084.vision.ScoreUtils.ratioToScore;

/**
 * An object that represents a potential target. It runs a number of tests to
 * determine if it could possibly be a goal and how well it matches.
 *
 * @author Ben Wolsieffer
 */
public class Target {

    /**
     * The number of tests that produce a score. Used for calculating the
     * average score.
     */
    private static final int NUM_SCORES = 2;

    /**
     * The ideal aspect ratio for the static (vertical) target.
     */
    public static final double STATIC_TARGET_ASPECT_RATIO = 4.0 / 32.0;
    /**
     * The ideal aspect ratio for the hot (horizontal) target.
     */
    public static final double HOT_TARGET_ASPECT_RATIO = 23.5 / 4.0;
    /**
     * The minimum area that the bounding rectangle of a blob can have for it to
     * be considered a goal. We found that certain values would work when the
     * targets were nearby, but when the robot was a the starting position, the
     * goal would appear to be too small and get filtered out.
     */
    public static double MIN_AREA = 100;

    /**
     * The minimum rectangularity score a blob can have to be considered a
     * target.
     */
    public static double MIN_RECTANGULARITY_SCORE = 10;
    /**
     * The minimum aspect ratio score a blob can have to be considered a target.
     */
    public static double MIN_ASPECT_RATIO_SCORE = 10;

    /**
     * The shape of the blob that was found by OpenCV.
     */
    private final Polygon shape;
    /**
     * The bounding rectangle of the polygon. This is assumed to be the shape of
     * the target, which means that if the the target is at an angle, its scores
     * will be less accurate.
     */
    private final Rectangle rect;
    /**
     * The score of this target.
     */
    private double score = -1;
    /**
     * Stores whether or not the target meets the minimum score requirements.
     */
    private boolean valid = true;

    /**
     * Creates a new possible target based on the specified blob and calculates
     * its score.
     *
     * @param p the shape of the possible target
     */
    public Target(Polygon p) {
        shape = p;
        rect = p.getBounds();

        score = calculateScore();
    }

    /**
     * Calculates the area of a polygon.
     *
     * @param p the polygon to perform the calculation on
     * @return the area of the polygon
     */
    private static double getPolygonArea(Polygon p) {
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

    /**
     * Gets the score of this target.
     *
     * @return the target's score
     */
    public double getScore() {
        return score;
    }

    /**
     * Calculates this target's score. If the target is not valid, it returns 0.
     * This is called in the constructor.
     *
     * @return this target's score
     */
    private double calculateScore() {
        double lScore
                = (scoreRectangularity()
                + scoreAspectRatio()) / NUM_SCORES;
        return isValid() ? lScore : 0;
    }

    /**
     * Calculate the rectangularity score for this target. The rectangularity is
     * the ratio between the area of the polygon blob and its bounding
     * rectangle. This ratio is converted to a score using
     * {@link ScoreUtils#ratioToScore(double)}.
     *
     * @return this target's rectangularity score
     */
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

    /**
     * Calculate the aspect ratio score for this target. This is calculated by
     * dividing the target's ratio by the target's ideal ratio. This ratio is
     * converted to a score using {@link ScoreUtils#ratioToScore(double)}.
     *
     * @return this target's rectangularity score
     */
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
