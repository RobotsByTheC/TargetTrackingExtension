/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.usfirst.frc2084.vision;

/**
 *
 * @author Ben Wolsieffer
 */
public final class ScoreUtils {

    private ScoreUtils() {
    }

    public static double ratioToScore(double ratio) {
        return (Math.max(0, Math.min(100 * (1 - Math.abs(1 - ratio)), 100)));
    }
}
