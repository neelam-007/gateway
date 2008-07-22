/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.gui.widgets;

import javax.swing.*;
import java.awt.*;

/**
 * A widget that displays a freely-resizable fast-forward indicator with a selectable speed.
 */
public class SpeedIndicator extends JComponent {
    public static final int SPEED_FAST = 1;
    public static final int SPEED_FASTER = 2;
    public static final int SPEED_FASTEST = 3;

    private int speed = 1;
    private Polygon triangle = null;
    private int halfTriWidth = 1;

    public SpeedIndicator() {
    }

    public SpeedIndicator(int speed) {
        this.speed = speed;
    }

    public void setSize(int width, int height) {
        super.setSize(width, height);
        triangle = null;
    }

    public void setSize(Dimension d) {
        super.setSize(d);
        triangle = null;
    }

    public void setBounds(int x, int y, int width, int height) {
        super.setBounds(x, y, width, height);
        triangle = null;
    }

    public void setBounds(Rectangle r) {
        super.setBounds(r);
        triangle = null;
    }

    /** @return a triangle positioned at the left of the component. */
    private Polygon getTriangle() {
        if (triangle == null) {
            final double h = getHeight();
            final double w = getWidth();
            triangle = new Polygon();
            triangle.addPoint((int)(w * 0.05), (int)(h * 0.05));
            triangle.addPoint((int)(w / 3), (int)(h * 0.50));
            triangle.addPoint((int)(w * 0.05), (int)(h * 0.95));
            halfTriWidth = (int)(triangle.getBounds().getMaxX() / 2);
        }
        return triangle;
    }

    /** Draw a triangle, offset from the horizontal center by the specified (positive or negative) amount. */
    private void drawTriangle(Graphics g, int offset) {
        Polygon tria = getTriangle();

        int center = getWidth() / 2;
        int totalOffset = center + offset - halfTriWidth;

        tria.translate(totalOffset, 0);

        g.setColor(new Color(0, 255, 0));
        g.setPaintMode();
        g.fillPolygon(triangle);
        g.setColor(new Color(0, 32, 0));
        g.drawPolygon(triangle);

        // Put it back, for next time
        tria.translate(-totalOffset, 0);
    }

    public void paintComponent( Graphics g ) {
        super.paintComponent(g);

        g.setColor(getBackground());
        final double h = getHeight();
        final double w = getWidth();
        g.fillRect(0, 0, (int)w, (int)h);

        // Draw this.speed triangles.
        getTriangle();
        double base = w < 20
                ? halfTriWidth + 2
                : halfTriWidth * 0.9;
        for (int i = 0; i < speed; ++i) {
            double p = (base * (speed - 1)) / 2;
            int offset = (int)(-(i * base) + p);
            drawTriangle(g, offset);
        }
    }

    /**
     * Set the speed to indicate.
     *
     * @param speed  the speed, from 0 (show no speed) to some small positive number (show that speed).  Must be between 0 and 20 inclusive.
     * @see #SPEED_FAST
     * @see #SPEED_FASTER
     * @see #SPEED_FASTEST
     */
    public void setSpeed(int speed) {
        if (speed < 0) speed = 0;
        if (speed > 20) speed = 20;
        this.speed = speed;
        repaint();
    }

    /**
     * @return the speed currently being indicated.
     * @see #setSpeed
     */
    public int getSpeed() {
        return speed;
    }
}
