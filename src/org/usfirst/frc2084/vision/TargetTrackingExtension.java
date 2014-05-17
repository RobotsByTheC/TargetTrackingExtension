/* 
 * Copyright (c) 2014 RobotsByTheC. All rights reserved.
 *
 * Open Source Software - may be modified and shared by FRC teams. The code must
 * be accompanied by the BSD license file in the root directory of the project.
 */
package org.usfirst.frc2084.vision;

import edu.wpi.first.smartdashboard.gui.DashboardPrefs;
import edu.wpi.first.smartdashboard.gui.StaticWidget;
import edu.wpi.first.smartdashboard.properties.DoubleProperty;
import edu.wpi.first.smartdashboard.properties.IPAddressProperty;
import edu.wpi.first.smartdashboard.properties.Property;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.image.BufferedImage;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.usfirst.frc2084.vision.properties.Range;
import org.usfirst.frc2084.vision.properties.RangeProperty;

/**
 * Team 2084's custom vision processing SmartDashboard extension. It uses our
 * algorithm to detect whether the target the camera is pointing at is hot and
 * tells the robot about it. This extension requires the use of our patched
 * SmartDashboard in order to load the native OpenCV libraries that it requires.
 *
 * @author Ben Wolsieffer
 */
public class TargetTrackingExtension extends StaticWidget {

    /**
     * The processor is what actually implements the machine vision algorithm.
     */
    private final TargetTrackingProcessor processor = new TargetTrackingProcessor();

    /**
     * The {@code NAME} field is used by the SmartDashboard to give the
     * extension a name in the menus.
     */
    public static final String NAME = "Team 2084 Hot Target Tracker";

    /**
     * The range of values (0-255) that a color property can hold.
     */
    private static final Range COLOR_RANGE = new Range(0, 255);

    // Various properties that appear in the properties editor of the extension.
    public final RangeProperty hThreshold = new RangeProperty(this, "H Threshold", COLOR_RANGE, processor.getHThreshold());
    public final RangeProperty sThreshold = new RangeProperty(this, "S Threshold", COLOR_RANGE, processor.getSThreshold());
    public final RangeProperty vThreshold = new RangeProperty(this, "V Threshold", COLOR_RANGE, processor.getVThreshold());
    public final DoubleProperty minArea = new DoubleProperty(this, "Min Blob Area", Target.MIN_AREA);
    public final DoubleProperty minRectangularityScore = new DoubleProperty(this, "Min Rectangluarity", Target.MIN_RECTANGULARITY_SCORE);
    public final DoubleProperty minAspectRatioScore = new DoubleProperty(this, "Min Aspect Ratio Score", Target.MIN_ASPECT_RATIO_SCORE);
    public final DoubleProperty minHorizontalDistanceScore = new DoubleProperty(this, "Min Horizontal Distance Score", TargetPair.MIN_HORIZONTAL_DISTANCE_SCORE);
    public final DoubleProperty minTapeWidthScore = new DoubleProperty(this, "Min Tape Width Score", TargetPair.MIN_TAPE_WIDTH_SCORE);
    public final DoubleProperty minVerticalDistanceScore = new DoubleProperty(this, "Min Vertical Distance Score", TargetPair.MIN_VERTICAL_DISTANCE_SCORE);

    /**
     * The size of the image which is grabbed from the camera and operated on by
     * the vision algorithm. This makes it easy to change the image size
     * throughout the extension.
     */
    public final static Size IMAGE_SIZE = new Size(800, 600);

    // Variables used to keep track of the FPS.
    private long lastFPSCheck = 0;
    private int lastFPS = 0;
    private int fpsCounter = 0;

    /**
     * Image used to transfer data between the processing loop and the UI
     * drawing thread. It probably should be made more thread-safe but I haven't
     * had a problem and it doesn't matter that much if a few frames get
     * corrupted.
     */
    private BufferedImage imageToDraw;

    /**
     * A separate {@link Thread} that runs the vision processing algorithm. It
     * basically calls
     * {@link TargetTrackingProcessor#processImage(org.opencv.core.Mat)} and
     * monitors the FPS. I moved this out of the UI thread to keep it smooth.
     */
    public class ProcessingThread extends Thread {

        boolean destroyed = false;

        public ProcessingThread() {
            super("Target Tracker Processing Thread");
        }

        /**
         * Thread-local copy of the image to process.
         */
        private final Mat processingImage = new Mat(IMAGE_SIZE, CvType.CV_8UC3);

        @Override
        @SuppressWarnings("SleepWhileInLoop")
        public void run() {
            // Run until the extension is removed or the SmartDashboard closes
            while (!destroyed) {
                // If the camera is enabled, run the processing loop
                if (TargetTrackingCommunication.isCameraEnabled()) {
                    // Start the camera if it is not running
                    captureThread.start();

                    // Possibly would prevent the algorithm from running after 
                    // the camera was enabled but before it was connected.
//                    if (captureThread.isConnected()) {
//                    
                    // Copy the image from the video capture to a thread local
                    // copy. This is to fix a bug where the image was being
                    // overwritten by the next video frame capture before the 
                    // algorithm completed because the video capture thread runs
                    // much faster than this thread. It was basically resulting 
                    // in screen tearing.
                    synchronized (image) {
                        image.copyTo(processingImage);
                    }
                    // Process the image. The return value is what will be drawn
                    // to the screen.
                    imageToDraw = processor.processImage(processingImage);

                    // Update FPS
                    fpsCounter++;
                    if (System.currentTimeMillis() - lastFPSCheck > 500) {
                        lastFPSCheck = System.currentTimeMillis();
                        lastFPS = fpsCounter * 2;
                        fpsCounter = 0;
                    }
                    // Draw the new image (this is thread-safe)
                    repaint();
//                    }
                } else {
                    // If the camera is not enabled, stop the capture thread.
                    // This does nothing if the camera is already disabled.
                    captureThread.stop();
                }
                try {
                    Thread.sleep(1);
                } catch (InterruptedException ex) {
                }
            }
        }

        /**
         * Sets a flag to tell the thread to stop.
         */
        @Override
        public void destroy() {
            destroyed = true;
        }
    }

    /**
     * Image retrieved from the camera.
     */
    private final Mat image = new Mat(IMAGE_SIZE, CvType.CV_8UC3);

    /**
     * The {@link Thread} which runs the {@link TargetTrackingProcessor}.
     */
    private final ProcessingThread processingThread = new ProcessingThread();
    /**
     * The thread which captures the image from the camera.
     */
    private final VideoCaptureThread captureThread = new VideoCaptureThread(image);
    /**
     * Team number.
     */
    private final int team = DashboardPrefs.getInstance().team.getValue();
    /**
     * The IP address of the camera.
     */
    public final IPAddressProperty ipProperty = new IPAddressProperty(this, "Camera IP Address", new int[]{10, (team / 100), (team % 100), 11});

    /**
     * This method is called when the SmartDashboard is started or the extension
     * is added.
     */
    @Override
    public void init() {

        setPreferredSize(new Dimension((int) IMAGE_SIZE.width, (int) IMAGE_SIZE.height));
        addComponentListener(new ComponentAdapter() {

            @Override
            public void componentResized(ComponentEvent e) {
                double imageRatio = IMAGE_SIZE.width / IMAGE_SIZE.height;
                double compRatio = (double) getWidth() / (double) getHeight();
                Dimension d;
                if (compRatio > imageRatio) {
                    d = new Dimension((int) (imageRatio * (double) getHeight()), getHeight());
                } else {
                    d = new Dimension(getWidth(), (int) ((getWidth() * IMAGE_SIZE.height) / IMAGE_SIZE.width));
                }
                setSavedSize(d);
                setSize(d);
            }

        });

        // Enable the camera (there are a lot of seemingly redundant 
        // communication calls to make sure everything works in every situation).
        // We had lots of problems where the if the robot and SmartDashboard 
        // were started in the wrong order then the vision would not work.
        TargetTrackingCommunication.setCameraEnabled(true);

        // Set initial saved values for the properties
        processor.setHThreshold(hThreshold.getValue());
        processor.setSThreshold(sThreshold.getValue());
        processor.setVThreshold(vThreshold.getValue());
        Target.MIN_RECTANGULARITY_SCORE = minRectangularityScore.getValue();
        Target.MIN_AREA = minArea.getValue();
        Target.MIN_ASPECT_RATIO_SCORE = minAspectRatioScore.getValue();
        TargetPair.MIN_HORIZONTAL_DISTANCE_SCORE = minHorizontalDistanceScore.getValue();
        TargetPair.MIN_TAPE_WIDTH_SCORE = minTapeWidthScore.getValue();
        TargetPair.MIN_VERTICAL_DISTANCE_SCORE = minVerticalDistanceScore.getValue();
        captureThread.setIP(ipProperty.getSaveValue());

        // Start everything
        captureThread.start();
        processingThread.start();
        revalidate();
        repaint();
    }

    /**
     * Called whenever the user changes a property in the SmartDashboard menu
     * this widget.
     *
     * @param property the property that changed
     */
    @Override
    public void propertyChanged(Property property) {
        if (property == ipProperty) {
            captureThread.setIP(ipProperty.getSaveValue());
        } else if (property instanceof RangeProperty) {
            Range r = ((RangeProperty) property).getValue();
            if (property == hThreshold) {
                processor.setHThreshold(r);
            } else if (property == sThreshold) {
                processor.setSThreshold(r);
            } else if (property == vThreshold) {
                processor.setVThreshold(r);
            }
        } else if (property instanceof DoubleProperty) {
            double d = ((DoubleProperty) property).getValue();
            if (property == minRectangularityScore) {
                Target.MIN_RECTANGULARITY_SCORE = d;
            } else if (property == minArea) {
                Target.MIN_AREA = d;
            } else if (property == minAspectRatioScore) {
                Target.MIN_ASPECT_RATIO_SCORE = d;
            } else if (property == minHorizontalDistanceScore) {
                TargetPair.MIN_HORIZONTAL_DISTANCE_SCORE = d;
            } else if (property == minTapeWidthScore) {
                TargetPair.MIN_TAPE_WIDTH_SCORE = d;
            } else if (property == minVerticalDistanceScore) {
                TargetPair.MIN_VERTICAL_DISTANCE_SCORE = d;
            }
        }
    }

    /**
     * Called from the event thread when the SmartDashboard is started or this
     * extension is added. It is not called when the robot disconnects as the
     * name implies.
     */
    @Override
    public void disconnect() {
        // Stop all the threads
        captureThread.stop();
        processingThread.destroy();
        super.disconnect();
    }

    /**
     * Paints the image and the frame rate onto the SmartDashobard.
     *
     * @param g the {@link Graphics} object to paint onto
     */
    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;
        // If the capture thread is connected and there is a image to draw, draw it.
        if (captureThread.isConnected() && imageToDraw != null) {
            BufferedImage drawnImage = imageToDraw;
            int width = getBounds().width;
            int height = getBounds().height;
            // Scale the image to fit in the component and draw it.
            double scale = Math.min((double) width / (double) image.width(), (double) height / (double) image.height());

            g2d.drawImage(drawnImage, (int) (width - (scale * image.width())) / 2, (int) (height - (scale * drawnImage.getHeight())) / 2,
                    (int) ((width + scale * drawnImage.getWidth()) / 2), (int) (height + scale * drawnImage.getHeight()) / 2,
                    0, 0, drawnImage.getWidth(), drawnImage.getHeight(), null);
            // Draw the FPs indicator.
            g2d.setColor(Color.WHITE);
            g2d.drawString("FPS: " + lastFPS, 10, 15);
        } else {
            // If the camera is not connected, make the background pink and say
            // "NO CONNECTION"
            g2d.setColor(Color.PINK);
            g2d.fillRect(0, 0, getBounds().width, getBounds().height);
            g2d.setColor(Color.BLACK);
            g2d.drawString("NO CONNECTION", 10, 15);
        }
    }
}
