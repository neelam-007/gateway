/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.common.xml;

import com.tarari.xml.xpath.XPathCompiler;

/**
 * @author alex
 * @version $Revision$
 */
public class TarariUtil {
    private static Boolean present = null;
    public static boolean isTarariPresent() {
        if (present == null) {
            try {
                XPathCompiler.reset();
                present = Boolean.TRUE;
            } catch (Throwable t) {
                present = Boolean.FALSE;
            }
        }
        return present.booleanValue();
    }
}
