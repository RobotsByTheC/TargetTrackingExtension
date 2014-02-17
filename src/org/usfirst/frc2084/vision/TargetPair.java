package org.usfirst.frc2084.vision;

import java.awt.Rectangle;
import static org.usfirst.frc2084.vision.ScoreUtils.ratioToScore;

/**
 *
 * @author Ben Wolsieffer
 */
public class TargetPair {

    private static final double HORIZONTAL_DISTANCE_RATIO = 1.2;

    private static final double MIN_HORIZONTAL_DISTANCE_SCORE = 20;
    private static final double MIN_TAPE_WIDTH_SCORE = 20;
    private static final double MIN_VERTICAL_DISTANCE_SCORE = 0;

    private final Target staticTarget;
    private Target hotTarget;
    private boolean hot = false;

    public TargetPair(Target staticTarget, Target hotTarget) {
        this.staticTarget = staticTarget;
        this.hotTarget = hotTarget;

        if (hotTarget != null) {
            test();
        }
    }

    public boolean isHot() {
        return hot;
    }

    private void test() {
        hot = testHorizontalDistance()
                && testVerticalDistance()
                && testTapeWidth();
        if (!hot) {
            hotTarget = null;
        }
    }

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

    private boolean testVerticalDistance() {
        Rectangle hotRect = hotTarget.getRect();
        Rectangle staticRect = staticTarget.getRect();
        return ratioToScore(1.0 - (staticRect.getMaxY() - hotRect.getCenterY()) / (4 * hotRect.getHeight())) >= MIN_VERTICAL_DISTANCE_SCORE;
    }

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
