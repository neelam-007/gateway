/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.common.xml;

import java.lang.reflect.Method;

/**
 * @author alex
 * @version $Revision$
 */
public class TarariUtil {
    public static final String ENABLE_PROPERTY = "com.l7tech.common.xml.tarari.enable";
    public static final String XPATH_COMPILER_CLASSNAME = "com.tarari.xml.xpath.XPathCompiler";
    private static Boolean present = null;
    public static boolean isTarariPresent() {
        if (present == null) {
            if (Boolean.getBoolean(ENABLE_PROPERTY)) {
                try {
                    Class xpathCompilerClass = Class.forName(XPATH_COMPILER_CLASSNAME, false, TarariUtil.class.getClassLoader());
                    Method resetMethod = xpathCompilerClass.getMethod("reset", new Class[0]);
                    resetMethod.invoke(null, new Object[0]);
                    present = Boolean.TRUE;
                } catch (UnsatisfiedLinkError e) {
                    present = Boolean.FALSE;
                } catch (Throwable t) {
                    present = Boolean.FALSE;
                }
            } else {
                // Disabled -- skip the probe
                present = Boolean.FALSE;
            }
        }
        return present.booleanValue();
    }
}
