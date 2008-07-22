/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.gui.util;

import java.awt.*;
import java.awt.font.TextAttribute;
import java.util.Map;
import java.util.HashMap;

/**
 * Utilities for adjusting fonts on controls.
 *
 * User: mike
 * Date: Oct 1, 2003
 * Time: 12:33:05 PM
 */
public class FontUtil {

    /**
     * Resize the font used by the given component.  Example: resizeFont(myLabel, 2.0) makes
     * the label's text twice as big as it is now.
     *
     * @param c  the component whose font to resize.  Required.
     * @param scale  the scaling factor to apply to its current font.  Required.
     */
    public static void resizeFont(Component c, double scale) {
        if (c == null) throw new IllegalArgumentException("Component required");
        Font font = c.getFont();
        if (font == null) return;
        Map<TextAttribute, Object> fontAttributes = new HashMap<TextAttribute, Object>(font.getAttributes());
        // In Java 1.5 this must be a Float
        fontAttributes.put(TextAttribute.SIZE, new Float(scale * getFontSize(fontAttributes)));
        Font newFont = Font.getFont(fontAttributes);
        c.setFont(newFont);
    }

    /**
     * Change the font used by the given component into bold face.
     *
     * @param c the component whose font to make boldface.   Required.
     */
    public static void emboldenFont(Component c) {
        if (c == null) throw new IllegalArgumentException("Component required");
        Font font = c.getFont();
        if (font == null) return;
        Map<TextAttribute, Object> fontAttributes = new HashMap<TextAttribute, Object>(font.getAttributes());
        fontAttributes.put(TextAttribute.WEIGHT, TextAttribute.WEIGHT_BOLD);
        Font newFont = Font.getFont(fontAttributes);
        c.setFont(newFont);
    }

    /**
     * Change the font used by the given component into bold face.
     *
     * @param font the component whose font to make boldface.   Required.
     */
    public static Font emboldenFont(Font font) {
        if (font == null) throw new IllegalArgumentException("Font required");
        Map<TextAttribute, Object> fontAttributes = new HashMap<TextAttribute, Object>(font.getAttributes());
        fontAttributes.put(TextAttribute.WEIGHT, TextAttribute.WEIGHT_BOLD);
        return Font.getFont(fontAttributes);
    }

    private static double getFontSize(Map<TextAttribute,?> attributes) {
        Object obj = attributes.get(TextAttribute.SIZE);
        double size = 12.0;
        if (obj instanceof Number)
            size = ((Number)obj).doubleValue();
        return size;
    }
}
