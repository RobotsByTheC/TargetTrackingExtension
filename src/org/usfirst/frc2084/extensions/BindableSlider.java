/* 
 * Copyright (c) 2014 RobotsByTheC. All rights reserved.
 *
 * Open Source Software - may be modified and shared by FRC teams. The code must
 * be accompanied by the BSD license file in the root directory of the project.
 */
package org.usfirst.frc2084.extensions;

import edu.wpi.first.smartdashboard.gui.elements.bindings.NumberBindable;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

public class BindableSlider extends JSlider implements NumberBindable, ComponentListener, ChangeListener {

    private double min = 0;
    private double max = 100;
    private double value = 0;
    private double tickSpacing = 10;
    private int pixelWidth = 0;

    @SuppressWarnings("LeakingThisInConstructor")
    public BindableSlider(NumberBindable bindable) {
        calcBounds();
        addComponentListener(this);
        addChangeListener(this);
    }

    @Override
    public void setBindableValue(double value) {
        System.out.println("NumberSlider.setBindableValue(" + value + ")");
        this.value = value;
        setUnscaledValue(value);
    }

    private void setUnscaledValue(double value) {
        System.out.println("NumberSlider.setUnscaledValue");
        if (value < min) {
            value = min;
        }
        if (value > max) {
            value = max;
        }
        setValue(scaleValue(value));
    }

    private double getUnscaledValue() {
        return unscaleValue(getValue());
    }

    private int scaleValue(double unscaledValue) {
        return (int) (((unscaledValue - min) / (max - min)) * pixelWidth);
    }

    private double unscaleValue(int scaledValue) {
        if (pixelWidth != 0) {
            return (scaledValue / pixelWidth) * (max - min) + min;
        } else {
            return 0;
        }
    }

    @Override
    public void stateChanged(ChangeEvent e) {
        // Does not work at all!!! major bug!!!
//        if (e.getSource())//TODO implement better delay
//        {
//            bindable.setBindableValue(getUnscaledValue());
//        }
    }

    public void setMin(double min) {
        this.min = min;
        calcBounds();
    }

    public void setMax(double max) {
        this.max = max;
        calcBounds();
    }

    private void calcBounds() {
        System.out.println("NumberSlider.calcBounds");
        pixelWidth = getWidth();
        setMinimum(0);
        setMaximum(pixelWidth);
        setUnscaledValue(value);
        setTickSpacing(tickSpacing);
    }

    public void setTickSpacing(double spacing) {
        tickSpacing = spacing;
        setMajorTickSpacing(scaleValue(spacing));
    }

    @Override
    public void setOrientation(int orientation) {
        super.setOrientation(orientation);
        calcBounds();
    }

    @Override
    public void componentResized(ComponentEvent e) {
        calcBounds();
    }

    @Override
    public void componentMoved(ComponentEvent e) {
    }

    @Override
    public void componentShown(ComponentEvent e) {
    }

    @Override
    public void componentHidden(ComponentEvent e) {
    }
}
