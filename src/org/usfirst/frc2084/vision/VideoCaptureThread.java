/* 
 * Copyright (c) 2014 RobotsByTheC. All rights reserved.
 *
 * Open Source Software - may be modified and shared by FRC teams. The code must
 * be accompanied by the BSD license file in the root directory of the project.
 */
package org.usfirst.frc2084.vision;

import java.awt.image.BufferedImage;
import org.opencv.core.Mat;
import org.opencv.highgui.VideoCapture;

/**
 * This class creates a thread which constantly captures frames from the Axis
 * camera. It is necessary to do this in a separate thread from the vision
 * processing because new frames become available much faster than they can be
 * processed, and the frames will begin to backup if they are not constantly
 * read.
 *
 * @author Ben Wolsieffer
 */
public class VideoCaptureThread {

    /**
     * OpenCV video capture that is used for getting the video. I used this
     * instead of the pure Java method that the normal SmartDashboard video
     * viewer uses because it eliminates a conversion from {@link BufferedImage}
     * to {@link Mat}.
     */
    private final VideoCapture vcap = new VideoCapture();
    /**
     * The Java {@link Thread} that captures the video.
     */
    private CaptureThread captureThread = new CaptureThread();

    /**
     * The camera's IP address.
     */
    private String ip;
    private boolean ipChanged = false;
    /**
     * Flag that is set if the camera's IP address is changed.
     */
    private boolean running = false;

    /**
     * The image to write read the camera data into.
     */
    private final Mat image;

    private class CaptureThread extends Thread {

        public CaptureThread() {
            super("Target Tracker Capture Thread");
        }

        long lastRepaint = 0;

        @Override
        @SuppressWarnings({"SleepWhileInLoop", "CallToPrintStackTrace", "ConfusingArrayVararg", "PrimitiveArrayArgumentToVariableArgMethod"})
        public void run() {
            while (running) {
                System.out.println("Connecting to camera at: \"" + ip + "\"...");
                ipChanged = false;
                if (vcap.open("http://" + ip + "/mjpg/video.mjpg")) {
                    System.out.println("Connected to camera.");
                    // Capture the image until the thread is stopped or the ip is changed.
                    while (running && !ipChanged) {
                        // Read the image. This needs to be synchronized to
                        // prevent data corruption by the processing thread.
                        synchronized (image) {
                            if (!vcap.read(image)) {
                                // If the read fails (in thoery when the camera 
                                // is disconnected), close the video capture. In
                                // reality vcap.read() just blocks when the
                                // camera is disconnected.
                                vcap.release();
                                System.out.println("Disconnected from camera.");
                                break;
                            }
                        }
                        try {
                            Thread.sleep(1);
                        } catch (InterruptedException ex) {
                        }
                    }
                }
                vcap.release();
                try {
                    Thread.sleep(100);
                } catch (InterruptedException ex) {
                }
            }
        }
    }

    public VideoCaptureThread(Mat image) {
        this.image = image;
    }

    /**
     * Sets the IP address of the camera to connect to. If the camera is
     * currently connected, this causes it to reconnect to the new camera.
     *
     * @param ip
     */
    public void setIP(String ip) {
        this.ip = ip;
        ipChanged = true;
    }

    /**
     * Starts the capture thread if it is not already running.
     */
    public void start() {
        if (!running) {
            if (captureThread.getState() != Thread.State.NEW) {
                captureThread = new CaptureThread();
            }
            running = true;
            captureThread.start();
        }
    }

    /**
     * Stops the capture thread by calling {@link Thread#stop()} on it. This
     * should only be used if the thread needs to be stopped after the camera
     * was disconnected. When {@link VideoCapture} loses connection, reads block
     * until it is reconnected.
     */
    @SuppressWarnings("CallToThreadStopSuspendOrResumeManager")
    public void stopHard() {
        if (running) {
            stop();
            // Warning: Unsafe code, necessary to work around a bug in VideoCapture
            captureThread.stop();
            vcap.release();
        }
    }

    /**
     * Stop the video capture thread normally.
     */
    public void stop() {
        running = false;
    }

    /**
     * Returns whether or not the video capture thread is connected.
     *
     * @return whether the camera is connected
     */
    public boolean isConnected() {
        return vcap.isOpened();
    }
}
