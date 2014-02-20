package org.usfirst.frc2084.vision.properties;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;

import javax.swing.JSlider;
import javax.swing.plaf.basic.BasicSliderUI;

/**
 * UI delegate for a JSlider. It replaces the normal thumb with a circular gray
 * one.
 * 
 * @author Ben Wolsieffer
 */
class CircleSliderUI extends BasicSliderUI {

    public static final Color THUMB_OUTLINE_COLOR = Color.DARK_GRAY;
    public static final Color THUMB_FILL_COLOR = Color.GRAY;

    /**
     * Constructs a RangeSliderUI for the specified slider component.
     *
     * @param b RangeSlider
     */
    public CircleSliderUI(JSlider b) {
        super(b);
    }

    /**
     * Returns the size of a thumb.
     */
    @Override
    protected Dimension getThumbSize() {
        return new Dimension(12, 12);
    }

    /**
     * Paints the thumb for the lower value using the specified graphics object.
     */
    @Override
    public void paintThumb(Graphics g) {
        Rectangle knobBounds = thumbRect;
        int w = knobBounds.width;
        int h = knobBounds.height;

        // Create graphics copy.
        Graphics2D g2d = (Graphics2D) g.create();

        // Create default thumb shape.
        Shape thumbShape = createThumbShape(w - 1, h - 1);

        // Draw thumb.
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.translate(knobBounds.x, knobBounds.y);

        g2d.setColor(THUMB_FILL_COLOR);
        g2d.fill(thumbShape);

        g2d.setColor(THUMB_OUTLINE_COLOR);
        g2d.draw(thumbShape);

        // Dispose graphics.
        g2d.dispose();
    }

    /**
     * Returns a Shape representing a thumb.
     */
    private Shape createThumbShape(int width, int height) {
        // Use circular shape.
        Ellipse2D shape = new Ellipse2D.Double(0, 0, width, height);
        return shape;
    }

}
