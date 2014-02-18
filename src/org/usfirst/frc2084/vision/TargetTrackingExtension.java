package org.usfirst.frc2084.vision;

import edu.wpi.first.smartdashboard.gui.DashboardPrefs;
import edu.wpi.first.smartdashboard.gui.StaticWidget;
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
 *
 * @author Ben Wolsieffer
 */
public class TargetTrackingExtension extends StaticWidget {

    private final TargetTrackingProcessor processor = new TargetTrackingProcessor();

    public static final String NAME = "Team 2084 Hot Target Tracker";

    private static final Range COLOR_RANGE = new Range(0, 255);
    private static final Range SCORE_RANGE = new Range(0, 100);

    public final RangeProperty hThreshold = new RangeProperty(this, "H Threshold", COLOR_RANGE, processor.getHThreshold());
    public final RangeProperty sThreshold = new RangeProperty(this, "S Threshold", COLOR_RANGE, processor.getSThreshold());
    public final RangeProperty vThreshold = new RangeProperty(this, "V Threshold", COLOR_RANGE, processor.getVThreshold());
//    public final SliderProperty minRectangularityScore = new SliderProperty(this, "Min Rectangluarity", SCORE_RANGE);

    public static Size IMAGE_SIZE = new Size(800, 600);

    private long lastFPSCheck = 0;
    private int lastFPS = 0;
    private int fpsCounter = 0;

    private BufferedImage imageToDraw;

    public class ProcessingThread extends Thread {

        boolean destroyed = false;

        public ProcessingThread() {
            super("Target Tracker Processing Thread");
        }

        private final Mat processingImage = new Mat(IMAGE_SIZE, CvType.CV_8UC3);

        @Override
        @SuppressWarnings({"SleepWhileInLoop", "ConfusingArrayVararg", "PrimitiveArrayArgumentToVariableArgMethod"})
        public void run() {
            while (!destroyed) {
                // If the camera is enabled
                if (TargetTrackingCommunication.isCameraEnabled()) {
                    // Start the camera if it is not running
                    captureThread.start();

                    synchronized (image) {
                        image.copyTo(processingImage);
                    }
                    imageToDraw = processor.processImage(processingImage);

                    fpsCounter++;
                    if (System.currentTimeMillis() - lastFPSCheck > 500) {
                        lastFPSCheck = System.currentTimeMillis();
                        lastFPS = fpsCounter * 2;
                        fpsCounter = 0;
                    }
                    repaint();
                } else {
                    captureThread.stop();
                }
                try {
                    Thread.sleep(1);
                } catch (InterruptedException ex) {
                }
            }
        }

        @Override
        public void destroy() {
            destroyed = true;
        }
    }

    private final Mat image = new Mat(IMAGE_SIZE, CvType.CV_8UC3);

    private final ProcessingThread processingThread = new ProcessingThread();
    private final VideoCaptureThread captureThread = new VideoCaptureThread(image);
    private final int team = DashboardPrefs.getInstance().team.getValue();
    public final IPAddressProperty ipProperty = new IPAddressProperty(this, "Camera IP Address", new int[]{10, (team / 100), (team % 100), 11});

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
        TargetTrackingCommunication.setCameraEnabled(true);

        captureThread.setIP(ipProperty.getSaveValue());
        captureThread.start();
        processingThread.start();
        revalidate();
        repaint();
    }

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
        }

    }

    @Override
    public void disconnect() {
        captureThread.stop();
        processingThread.destroy();
        super.disconnect();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;
        if (captureThread.isConnected() && imageToDraw != null) {
            BufferedImage drawnImage = imageToDraw;
            int width = getBounds().width;
            int height = getBounds().height;
            double scale = Math.min((double) width / (double) image.width(), (double) height / (double) image.height());

            g2d.drawImage(drawnImage, (int) (width - (scale * image.width())) / 2, (int) (height - (scale * drawnImage.getHeight())) / 2,
                    (int) ((width + scale * drawnImage.getWidth()) / 2), (int) (height + scale * drawnImage.getHeight()) / 2,
                    0, 0, drawnImage.getWidth(), drawnImage.getHeight(), null);
            g2d.setColor(Color.WHITE);
            g2d.drawString("FPS: " + lastFPS, 10, 15);
        } else {
            g2d.setColor(Color.PINK);
            g2d.fillRect(0, 0, getBounds().width, getBounds().height);
            g2d.setColor(Color.BLACK);
            g2d.drawString("NO CONNECTION", 10, 15);
        }
    }

}
