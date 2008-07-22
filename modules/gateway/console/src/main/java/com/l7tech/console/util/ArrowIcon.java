package com.l7tech.console.util;

import javax.swing.*;
import java.awt.*;

/*
 * This class creates an arrow icon.
 *
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

public class ArrowIcon implements Icon {
    public static final int UP = 0;         // direction
    public static final int DOWN = 1;

    public static final int DEFAULT_SIZE = 11;

    private Color edge1;
    private Color edge2;
    private Color fill;
    private int size;
    private int direction;

    /**
     * Constructor
     *
     * @param direction  The direction of the arrow.
     */
    public ArrowIcon(int direction) {
        init(UIManager.getColor("controlHighlight"),
                UIManager.getColor("controlShadow"),
                UIManager.getColor("control"),
                DEFAULT_SIZE, direction);
    }

    /**
     * Paint the icon object.
     *
     * @param c  The component object.
     * @param g  The graphic object.
     * @param x  The x-coordinate of the arrow's origin.
     * @param y  The y-coordinate of the arrow's origin.
     */
    public void paintIcon(Component c, Graphics g, int x, int y) {
        switch (direction) {
            case DOWN:
                drawDownArrow(g, x, y);
                break;
            case UP:
                drawUpArrow(g, x, y);
                break;
        }
    }

    /**
     * Return the width of the icon.
     *
     * @return int  The width of the icon.
     */
    public int getIconWidth() {
        return size;
    }

    /**
     * return the height of the icon.
     *
     * @return int  The height of the icon.
     */
    public int getIconHeight() {
        return size;
    }

    /**
     * Initialize the variables.
     *
     * @param edge1  The color of the highlight.
     * @param edge2  The color of the shadow.
     * @param fill  The fill color.
     * @param size  The size of the arrow.
     * @param direction  The direction of the arrow.
     */
    private void init(Color edge1, Color edge2, Color fill,
                      int size, int direction) {
        this.edge1 = edge1;
        this.edge2 = edge2;
        this.fill = fill;
        this.size = size;
        this.direction = direction;
    }

    /**
     * Draw the arrow icon in downward direction.
     *
     * @param g  The graphic object reference.
     * @param xo  The x-coordinate of the arrow's origin.
     * @param yo  The y-coordinate of the arrow's origin.
     */
    private void drawDownArrow(Graphics g, int xo, int yo) {
        g.setColor(edge1);
        g.drawLine(xo, yo, xo + size - 1, yo);
        g.drawLine(xo, yo + 1, xo + size - 3, yo + 1);
        g.setColor(edge2);
        g.drawLine(xo + size - 2, yo + 1, xo + size - 1, yo + 1);
        int x = xo + 1;
        int y = yo + 2;
        int dx = size - 6;
        while (y + 1 < yo + size) {
            g.setColor(edge1);
            g.drawLine(x, y, x + 1, y);
            g.drawLine(x, y + 1, x + 1, y + 1);
            if (0 < dx) {
                g.setColor(fill);
                g.drawLine(x + 2, y, x + 1 + dx, y);
                g.drawLine(x + 2, y + 1, x + 1 + dx, y + 1);
            }
            g.setColor(edge2);
            g.drawLine(x + dx + 2, y, x + dx + 3, y);
            g.drawLine(x + dx + 2, y + 1, x + dx + 3, y + 1);
            x += 1;
            y += 2;
            dx -= 2;
        }
        g.setColor(edge1);
        g.drawLine(xo + (size / 2), yo + size - 1, xo + (size / 2), yo + size - 1);
    }

    /**
     * Draw the arrow icon in upward direction.
     *
     * @param g  The graphic object reference.
     * @param xo  The x-coordinate of the arrow origin.
     * @param yo  The y-coordinate of the arrow origin.
     */
    private void drawUpArrow(Graphics g, int xo, int yo) {
        g.setColor(edge1);
        int x = xo + (size / 2);
        g.drawLine(x, yo, x, yo);
        x--;
        int y = yo + 1;
        int dx = 0;
        while (y + 3 < yo + size) {
            g.setColor(edge1);
            g.drawLine(x, y, x + 1, y);
            g.drawLine(x, y + 1, x + 1, y + 1);
            if (0 < dx) {
                g.setColor(fill);
                g.drawLine(x + 2, y, x + 1 + dx, y);
                g.drawLine(x + 2, y + 1, x + 1 + dx, y + 1);
            }
            g.setColor(edge2);
            g.drawLine(x + dx + 2, y, x + dx + 3, y);
            g.drawLine(x + dx + 2, y + 1, x + dx + 3, y + 1);
            x -= 1;
            y += 2;
            dx += 2;
        }
        g.setColor(edge1);
        g.drawLine(xo, yo + size - 3, xo + 1, yo + size - 3);
        g.setColor(edge2);
        g.drawLine(xo + 2, yo + size - 2, xo + size - 1, yo + size - 2);
        g.drawLine(xo, yo + size - 1, xo + size, yo + size - 1);
    }

}

