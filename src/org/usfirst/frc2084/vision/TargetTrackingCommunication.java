/* 
 * Copyright (c) 2014 RobotsByTheC. All rights reserved.
 *
 * Open Source Software - may be modified and shared by FRC teams. The code must
 * be accompanied by the BSD license file in the root directory of the project.
 */
package org.usfirst.frc2084.vision;

import edu.wpi.first.wpilibj.networktables.NetworkTable;

/**
 * This class is what communicates with the robot using NetworkTables. It is
 * duplicated almost identically no the robot side. Both sides call it a lot to
 * make sure all the field are always in the right state.
 *
 * @author Ben Wolsieffer
 */
public class TargetTrackingCommunication {

    public static final String TARGET_TABLE_NAME = "TargetTracking";
    public static final String TARGET_TABLE_STATE_KEY = "goal_hot";
    public static final String TARGET_TABLE_AUTONOMOUS_VISION_RUNNING_KEY = "auto_vision";
    public static final String TARGET_TABLE_ENABLE_CAMERA_KEY = "enable_camera";

    public static final NetworkTable targetTable = NetworkTable.getTable(TARGET_TABLE_NAME);

    static {
        init();
    }

    public static void init() {
        setState(State.UNKNOWN);
    }

    /**
     * A fake enum to store the state of the target. I didn't use a real enum
     * because it isn't supported in Java ME on the robot side and Java doesn't
     * support custom enum ordinals like C++;
     */
    public static class State {

        public static final int HOT_VALUE = 1;
        public static final int NOT_HOT_VALUE = 2;
        public static final int UNKNOWN_VALUE = 3;

        public final int value;

        public State(int value) {
            this.value = value;
        }

        public static final State HOT = new State(HOT_VALUE);
        public static final State NOT_HOT = new State(NOT_HOT_VALUE);
        public static final State UNKNOWN = new State(UNKNOWN_VALUE);

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final State other = (State) obj;
            return this.value == other.value;
        }

        @Override
        public int hashCode() {
            int hash = 3;
            hash = 29 * hash + this.value;
            return hash;
        }

        @Override
        public String toString() {
            switch (value) {
                case HOT_VALUE:
                    return "HOT";
                case NOT_HOT_VALUE:
                    return "NOT HOT";
                case UNKNOWN_VALUE:
                default:
                    return "UNKNOWN";
            }
        }

    }

    public static void setState(State state) {
        targetTable.putNumber(TARGET_TABLE_STATE_KEY, state.value);
    }

    public static State getState() {
        return new State((int) targetTable.getNumber(TARGET_TABLE_STATE_KEY, State.UNKNOWN_VALUE));
    }

    public static boolean isAutonomousVisionRunning() {
        return targetTable.getBoolean(TARGET_TABLE_AUTONOMOUS_VISION_RUNNING_KEY, false);
    }

    public static void setAutonomousVisionRunning(boolean started) {
        targetTable.putBoolean(TARGET_TABLE_AUTONOMOUS_VISION_RUNNING_KEY, started);
    }

    public static void setCameraEnabled(boolean enabled) {
        targetTable.putBoolean(TARGET_TABLE_ENABLE_CAMERA_KEY, enabled);
    }

    public static boolean isCameraEnabled() {
        return targetTable.getBoolean(TARGET_TABLE_ENABLE_CAMERA_KEY, true);
    }
}