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
     * @param c
     * @param scale
     */
    public static void resizeFont(Component c, double scale) {
        Font font = c.getFont();
        Map fontAttributes = new HashMap(font.getAttributes());
        fontAttributes.put(TextAttribute.SIZE, new Float(scale * ((Float)fontAttributes.get(TextAttribute.SIZE)).floatValue()));
        Font newFont = Font.getFont(fontAttributes);
        c.setFont(newFont);
    }
}
