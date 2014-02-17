package org.usfirst.frc2084.vision;

import org.opencv.core.Mat;
import org.opencv.highgui.VideoCapture;

/**
 *
 * @author ben
 */
public class VideoCaptureThread {

    private final VideoCapture vcap = new VideoCapture();
    private CaptureThread captureThread = new CaptureThread();

    private String ip;
    private boolean ipChanged = false;
    private boolean running = false;

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
                    while (running && !ipChanged) {

                        try {
                            Thread.sleep(1);
                        } catch (InterruptedException ex) {
                        }

                        synchronized (image) {
                            if (!vcap.read(image)) {
                                vcap.release();
                                System.out.println("Disconnected from camera.");
                                break;
                            }
                        }
                    }

                    if (!ipChanged) {
                        try {
                            Thread.sleep(500);
                        } catch (InterruptedException ex) {
                        }
                    }
                }
                vcap.release();
            }
        }
    }

    public VideoCaptureThread(Mat image) {
        this.image = image;
    }

    public void setIP(String ip) {
        this.ip = ip;
        ipChanged = true;
    }

    public void start() {
        if (!running) {
            if (captureThread.getState() != Thread.State.NEW) {
                captureThread = new CaptureThread();
            }
            running = true;
            captureThread.start();
        }
    }

    @SuppressWarnings("CallToThreadStopSuspendOrResumeManager")
    public void stopHard() {
        if (running) {
            stop();
            // Warning: Unsafe code, necessary to work around a bug in VideoCapture
            captureThread.stop();
            vcap.release();
        }
    }

    public void stop() {
        running = false;
    }

    public boolean isConnected() {
        return vcap.isOpened();
    }
}
