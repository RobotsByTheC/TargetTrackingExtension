package org.usfirst.frc2084.vision;

import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.MatOfInt;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.highgui.Highgui;
import org.opencv.imgproc.Imgproc;
import static org.usfirst.frc2084.vision.TargetTrackingExtension.IMAGE_SIZE;
import org.usfirst.frc2084.vision.properties.Range;

/**
 *
 * @author Ben Wolsieffer
 */
public class TargetTrackingProcessor {

    private static final Scalar MIN_THRESHOLD = new Scalar(0, 0, 50);
    private static final Scalar MAX_THRESHOLD = new Scalar(255, 255, 200);

    private static final Scalar HOT_TARGET_COLOR = new Scalar(255, 0, 0);
    private static final Scalar STATIC_TARGET_COLOR = new Scalar(0, 255, 0);
    private static final Scalar OTHER_TARGET_COLOR = new Scalar(0, 0, 255);

    /**
     * The minimum number of frames the algorithm must process in order to
     * determine the state of the target.
     */
    private static final int MIN_FRAMES = 4;
    private static final double MIN_HOT_FRAME_RATIO = 0.6;
    private static final double MAX_HOT_FRAME_RATIO = 0.4;
    private static final long AUTONOMOUS_START_WAIT = 100;
    /**
     * The number of frames where a hot goal was found since the match started.
     */
    private int hotFrameCount = 0;
    /**
     * The total number of frames processed since the match started. These
     * counters are used to filter out mistakes in the algorithm (ie. it missed
     * detecting the goal in one frame).
     */
    private int totalFrames = 0;
    private long autonomousStartTime = -1;

    public void init() {
        totalFrames = 0;
        hotFrameCount = 0;
        autonomousStartTime = -1;
    }

    public BufferedImage processImage(Mat image) {

        boolean autonomousRunning = TargetTrackingCommunication.isAutonomousVisionRunning();
        if (autonomousRunning) {
            if (autonomousStartTime == -1) {
                autonomousStartTime = System.currentTimeMillis();
            }
            if (System.currentTimeMillis() - autonomousStartTime < AUTONOMOUS_START_WAIT){
                autonomousRunning = false;
            }
        }

        // Convert the image to HSV, threshold it and find contours
        List<MatOfPoint> contours = findContours(threshold(convertToHsv(image)));

        // Array to hold blobs that possibly could be targets
        ArrayList<Target> possibleTargets = new ArrayList<>();

        // Convert the contours to Targets
        for (MatOfPoint contour : contours) {
            possibleTargets.add(new Target(convexHull(contour)));
        }

        // Object to store the static target in
        Target staticTarget = null;
        // Used in the loop for recording the maximum score found for a target
        double maxStaticScore = 0;

        // Object to store a possible target in. If it is null after the 
        // algorithm is run then the goal is not hot
        Target hotTarget = null;
        double maxHotScore = 0;

        // Loop through all the possible targets
        for (Target b : possibleTargets) {
            // Check if the target is valid (ie. it exceeds all the minimum
            // values for the different tests
            if (b.isValid()) {
                // Get the bounding rectangle for the target
                Rectangle rect = b.getRect();
                // Draw the bounding rectangle in red around all the targets
                Core.rectangle(image,
                        new Point(rect.x, rect.y),
                        new Point(rect.getMaxX(), rect.getMaxY()),
                        OTHER_TARGET_COLOR, 5);
                // Get the target's score
                double score = b.getScore();
                // If it is vertical (height > width) then it could be a static 
                // target
                if (b.isVertical()) {
                    // If this target exceeds the max score then it is the best
                    // canidate for a static target.
                    if (score > maxStaticScore) {
                        maxStaticScore = score;
                        staticTarget = b;
                    }
                } else { // Horizontal - hot target
                    // same thing for the hot target.
                    if (score > maxHotScore) {
                        maxHotScore = score;
                        hotTarget = b;
                    }
                }
            }
        }

        // If the algorithm found a suitable static target, try to form a target
        // pair.
        if (staticTarget != null) {
            // This object hold a pair of targets
            TargetPair targets = new TargetPair(staticTarget, hotTarget);

            // Draw a rectangle in green around the static target.
            Rectangle staticRect = staticTarget.getRect();
            Core.rectangle(image,
                    new Point(staticRect.x, staticRect.y),
                    new Point(staticRect.getMaxX(), staticRect.getMaxY()),
                    STATIC_TARGET_COLOR, 5);

            // If the algorithm found a hot target, process it
            if (hotTarget != null && targets.isHot()) {
                // Draw a rectangle in blue around the hot target.
                Rectangle hotRect = hotTarget.getRect();
                Core.rectangle(image,
                        new Point(hotRect.x, hotRect.y),
                        new Point(hotRect.getMaxX(), hotRect.getMaxY()),
                        HOT_TARGET_COLOR, 5);
                System.out.println("HOT");
                // If the robot reports that its autonomous is running, then
                // record that a hot goal was detected
                if (autonomousRunning) {
                    hotFrameCount++;
                }
            } else { // No hot goal was found
                System.out.println("NOT HOT");

            }
            // If the robot's autonomous is running, record that the algorithm 
            // has processed another frame.
            if (autonomousRunning) {
                totalFrames++;
            }
        }

        if (autonomousRunning) {
            // Filter out mistakes in the algorithm by making sure that the 
            // algorithm has already processed a certain number of frames before
            // reporting its findings to the robot.
            if (totalFrames >= MIN_FRAMES) {
                // Calulate the ratio of frames in which a hot goal was detected
                // the total number of frames.
                double hotFrameRatio = ((double) hotFrameCount) / ((double) totalFrames);

                // If this ratio is greater than MIN_HOT_FRAME_RATIO, then the
                // target is considered hot, if it is less than 
                // MAX_HOT_FRAME_RATIO, then it is considered not hot.
                if (hotFrameRatio >= MIN_HOT_FRAME_RATIO) {
                    setTargetState(TargetTrackingCommunication.State.HOT);
                } else if (hotFrameRatio <= MAX_HOT_FRAME_RATIO) {
                    setTargetState(TargetTrackingCommunication.State.NOT_HOT);
                } else {
                    TargetTrackingCommunication.setState(TargetTrackingCommunication.State.UNKNOWN);
                }
            }
        } else {
            init();
        }

        BufferedImage outImg = matToBufferedImage(image);
        return outImg;
    }

    public void setTargetState(TargetTrackingCommunication.State state) {
        TargetTrackingCommunication.setState(state);
        TargetTrackingCommunication.setAutonomousVisionRunning(false);
        TargetTrackingCommunication.setCameraEnabled(false);
        System.out.println("Told robot: " + state);
        init();
    }

    public BufferedImage matToBufferedImage(Mat m) {
        MatOfByte matOfByte = new MatOfByte();

        Highgui.imencode(".jpg", m, matOfByte);

        byte[] byteArray = matOfByte.toArray();
        BufferedImage bufImage = null;

        try {
            InputStream in = new ByteArrayInputStream(byteArray);
            bufImage = ImageIO.read(in);
        } catch (IOException e) {
        }
        return bufImage;
    }

    private final Mat hsvImage = new Mat(IMAGE_SIZE, CvType.CV_8UC3);

    private Mat convertToHsv(Mat image) {
        Imgproc.cvtColor(image, hsvImage, Imgproc.COLOR_BGR2HSV);
        return hsvImage;
    }

    private final Mat thresholdImage = new Mat(IMAGE_SIZE, CvType.CV_8UC1);

    private Mat threshold(Mat image) {
        Core.inRange(image, MIN_THRESHOLD, MAX_THRESHOLD, thresholdImage);
        Imgproc.medianBlur(thresholdImage, thresholdImage, 13);
        return thresholdImage;
    }

    private final Mat contoursImage = new Mat(IMAGE_SIZE, CvType.CV_8UC1);

    private List<MatOfPoint> findContours(Mat image) {
        Mat hierarchy = new Mat();
        ArrayList<MatOfPoint> contours = new ArrayList<>();
        image.copyTo(contoursImage);
        Imgproc.findContours(contoursImage, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);
        return contours;
    }

    private Polygon convexHull(MatOfPoint contour) {
        MatOfInt hullMatrix = new MatOfInt();
        Imgproc.convexHull(contour, hullMatrix); //perform convex hull, gap filler
        Polygon p = new Polygon(); // temporary polygon to store /all/ points of a blob

        Point[] points = contour.toArray();

        for (Point pt : points) {
            p.addPoint((int) pt.x, (int) pt.y);
        }
        return p;
    }

    public void setHThreshold(Range threshold) {
        MIN_THRESHOLD.val[0] = threshold.getMin();
        MAX_THRESHOLD.val[0] = threshold.getMax();
    }

    public void setSThreshold(Range threshold) {
        MIN_THRESHOLD.val[1] = threshold.getMin();
        MAX_THRESHOLD.val[1] = threshold.getMax();
    }

    public void setVThreshold(Range threshold) {
        MIN_THRESHOLD.val[2] = threshold.getMin();
        MAX_THRESHOLD.val[2] = threshold.getMax();
    }

    public Range getHThreshold() {
        return new Range((int) MIN_THRESHOLD.val[0], (int) MAX_THRESHOLD.val[0]);
    }

    public Range getSThreshold() {
        return new Range((int) MIN_THRESHOLD.val[1], (int) MAX_THRESHOLD.val[1]);
    }

    public Range getVThreshold() {
        return new Range((int) MIN_THRESHOLD.val[2], (int) MAX_THRESHOLD.val[2]);
    }
}
