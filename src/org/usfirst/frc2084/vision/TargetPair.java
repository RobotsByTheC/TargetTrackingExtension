/* 
 * Copyright (c) 2014 RobotsByTheC. All rights reserved.
 *
 * Open Source Software - may be modified and shared by FRC teams. The code must
 * be accompanied by the BSD license file in the root directory of the project.
 */
package org.usfirst.frc2084.vision;

import java.awt.Rectangle;
import static org.usfirst.frc2084.vision.ScoreUtils.ratioToScore;

/**
 * This class represents a possible pair of targets (hot and static). It also
 * determines whether they actually are targets.
 *
 * @author Ben Wolsieffer
 */
public class TargetPair {

    private static final double HORIZONTAL_DISTANCE_RATIO = 1.2;

    public static double MIN_HORIZONTAL_DISTANCE_SCORE = 20;
    public static double MIN_TAPE_WIDTH_SCORE = 20;
    public static double MIN_VERTICAL_DISTANCE_SCORE = 0;

    private final Target staticTarget;
    private Target hotTarget;
    private boolean hot = false;

    /**
     * Creates a possible target pair. If {@code hotTarget} is null, then the
     * algorithm assumes the static target is valid because it has nothing to
     * test it against. Otherwise it tests the targets to see if they match
     * certain criteria.
     *
     * @param staticTarget
     * @param hotTarget
     */
    public TargetPair(Target staticTarget, Target hotTarget) {
        this.staticTarget = staticTarget;
        this.hotTarget = hotTarget;

        if (hotTarget != null) {
            test();
        }
    }

    /**
     * Gets whether or not the target is hot.
     *
     * @return whether or not the target is hot
     */
    public boolean isHot() {
        return hot;
    }

    /**
     * Tests the pair to see if they form a valid pair.
     */
    private void test() {
        hot = testHorizontalDistance()
                && testVerticalDistance()
                && testTapeWidth();
        if (!hot) {
            hotTarget = null;
        }
    }

    /**
     * Tests the horizontal distance between the targets to see if it is within
     * a certain range.
     *
     * @return whether the targets pass the horizontal distance test
     */
    private boolean testHorizontalDistance() {
        Rectangle hotRect = hotTarget.getRect();
        Rectangle staticRect = staticTarget.getRect();

        double hotCenter = hotRect.getCenterX();
        double dist;
        if (hotRect.getMaxX() < staticRect.getMinX()) {
            dist = staticRect.getMinX() - hotCenter;
        } else if (staticRect.getMaxX() < hotRect.getMinX()) {
            dist = hotCenter - staticRect.getMaxX();
        } else {
            return false;
        }

        return ratioToScore((dist / hotRect.getWidth()) / HORIZONTAL_DISTANCE_RATIO) >= MIN_HORIZONTAL_DISTANCE_SCORE;

    }

    /**
     * Tests the vertical distance between the targets to see if it is within a
     * certain range. This currently does not work.
     *
     * @return whether the targets pass the vertical distance test
     */
    private boolean testVerticalDistance() {
        Rectangle hotRect = hotTarget.getRect();
        Rectangle staticRect = staticTarget.getRect();
        return ratioToScore(1.0 - (staticRect.getMaxY() - hotRect.getCenterY()) / (4 * hotRect.getHeight())) >= MIN_VERTICAL_DISTANCE_SCORE;
    }

    /**
     * Tests the relative width of both targets to see if they fall within a
     * certain range.
     *
     * @return whether the targets pass the tape width test
     */
    private boolean testTapeWidth() {
        return ratioToScore(hotTarget.getRect().getHeight() / staticTarget.getRect().getWidth()) >= MIN_TAPE_WIDTH_SCORE;
    }

    public Target getStaticTarget() {
        return staticTarget;
    }

    public Target getHotTarget() {
        return hotTarget;
    }
}
