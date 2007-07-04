/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.common.gui.util;

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
     * @param c  the component whose font to make boldface.  Required.
     * @param scale  the scaling factor to apply to its current font.  Required.
     */
    public static void resizeFont(Component c, double scale) {
        if (c == null) throw new IllegalArgumentException("Component required");
        Font font = c.getFont();
        if (font == null) return;
        Map<TextAttribute, Object> fontAttributes = new HashMap<TextAttribute, Object>(font.getAttributes());
        fontAttributes.put(TextAttribute.SIZE, scale * getFontSize(fontAttributes));
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

    private static float getFontSize(Map<TextAttribute,?> attributes) {
        Float size = (Float)attributes.get(TextAttribute.SIZE);
        return size == null ? 12.0f : size;
    }
}
