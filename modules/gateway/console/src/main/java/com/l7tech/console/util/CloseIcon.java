/**
 * Copyright (C) 2008, Layer 7 Technologies Inc.
 *
 * Modified from javax.swing.plaf.metal.MetalIconFactory.InternalFrameCloseIcon
 * @author darmstrong
 */
package com.l7tech.console.util;

import javax.swing.*;
import javax.swing.plaf.UIResource;
import javax.swing.plaf.metal.MetalLookAndFeel;
import java.awt.*;
import java.io.Serializable;

public class CloseIcon implements Icon, UIResource, Serializable {
    int iconSize = 16;

    public CloseIcon() {
    }

    public CloseIcon(int size) {
        iconSize = size;
    }

    public void paintIcon(Component c, Graphics g, int x, int y) {
        Color backgroundColor = MetalLookAndFeel.getPrimaryControl();
        Color internalBackgroundColor =
                MetalLookAndFeel.getPrimaryControl();
        Color mainItemColor =
                MetalLookAndFeel.getPrimaryControlDarkShadow();
        Color darkHighlightColor = MetalLookAndFeel.getBlack();
        Color xLightHighlightColor = MetalLookAndFeel.getWhite();
        Color boxLightHighlightColor = MetalLookAndFeel.getWhite();

        // Some calculations that are needed more than once later on.
        int oneHalf = (int) (iconSize / 2); // 16 -> 8

        g.translate(x, y);

        // THE "X"
        // Dark highlight
        g.setColor(darkHighlightColor);
        g.drawLine(4, 5, 5, 4); // far up left
        g.drawLine(4, iconSize - 6, iconSize - 6, 4); // against body of "X"
        // Light highlight
        g.setColor(xLightHighlightColor);
        g.drawLine(6, iconSize - 5, iconSize - 5, 6); // against body of "X"
        // one pixel over from the body
        g.drawLine(oneHalf, oneHalf + 2, oneHalf + 2, oneHalf);
        // bottom right
        g.drawLine(iconSize - 5, iconSize - 5, iconSize - 4, iconSize - 5);
        g.drawLine(iconSize - 5, iconSize - 4, iconSize - 5, iconSize - 4);
        // Main color
        g.setColor(mainItemColor);
        // Upper left to lower right
        g.drawLine(5, 5, iconSize - 6, iconSize - 6); // g.drawLine(5,5, 10,10);
        g.drawLine(6, 5, iconSize - 5, iconSize - 6); // g.drawLine(6,5, 11,10);
        g.drawLine(5, 6, iconSize - 6, iconSize - 5); // g.drawLine(5,6, 10,11);
        // Lower left to upper right
        g.drawLine(5, iconSize - 5, iconSize - 5, 5); // g.drawLine(5,11, 11,5);
        g.drawLine(5, iconSize - 6, iconSize - 6, 5); // g.drawLine(5,10, 10,5);

        g.translate(-x, -y);
    }

    public int getIconWidth() {
        return iconSize;
    }

    public int getIconHeight() {
        return iconSize;
    }
}
