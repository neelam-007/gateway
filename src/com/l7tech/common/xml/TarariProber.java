/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.common.xml;

import com.l7tech.common.message.SoapInfo;
import com.l7tech.common.message.SoapInfoFactory;
import org.xml.sax.SAXException;

import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

/**
 * Probe presence of Tarari without introducing any static classloader dependencies on Tarari.
 */
public class TarariProber {
    public static final String ENABLE_PROPERTY = "com.l7tech.common.xml.tarari.enable";
    public static final String XPATH_COMPILER_CLASSNAME = "com.tarari.xml.xpath.XPathCompiler";
    static Boolean present = null;
    static SoapInfoFactory soapInfoFactory = null;

    public static boolean isTarariPresent() {
        if (present == null) {
            synchronized (TarariProber.class) {
                if (present == null) {
                    if (Boolean.getBoolean(ENABLE_PROPERTY)) {
                        try {
                            Class xpathCompilerClass = Class.forName(XPATH_COMPILER_CLASSNAME, false, TarariProber.class.getClassLoader());
                            Method resetMethod = xpathCompilerClass.getMethod("reset", new Class[0]);
                            resetMethod.invoke(null, new Object[0]);
                            Class tarariFactoryClass = Class.forName("com.l7tech.common.xml.tarari.TarariSoapInfoFactory");
                            Constructor c = tarariFactoryClass.getConstructor(new Class[0]);
                            soapInfoFactory = (SoapInfoFactory)c.newInstance(new Object[0]);
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
            }
        }
        return present.booleanValue();
    }

    public static SoapInfo getSoapInfoTarari(InputStream in) throws SAXException, SoftwareFallbackException {
        return soapInfoFactory.getSoapInfo(in);
    }
}
