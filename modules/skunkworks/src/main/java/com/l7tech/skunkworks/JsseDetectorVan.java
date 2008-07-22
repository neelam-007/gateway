/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.skunkworks;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

/**
 * @author alex
 * @version $Revision$
 */
public class JsseDetectorVan {
    public static void main(String[] args) throws Exception {
        String certFactoryName = null;
        String jsseName = null;
        String jceName = null;
        try {
            Class.forName("com.ibm.jsse.JSSEProvider");
            certFactoryName = "IbmX509";
            jsseName = "IBMJSSEFIPS";
            jceName = "IBMJCEFIPS";
            System.out.println("Using IBM JSSE");
            return;
        } catch (ClassNotFoundException e) {
            System.err.println("IBM JSSE not available");
        }

        System.out.println("Using Sun JSSE");
        certFactoryName = "SunX509";
        jsseName = "SunJSSE";
        jceName = "SunJCE";

        System.out.println("Certificate factory algorithm: " + certFactoryName);
        TrustManagerFactory.getInstance(certFactoryName);
        System.out.println("JSSE Name: " + jsseName);
        SSLContext.getInstance("SSL", jsseName);

        System.out.println("JCE Name: " + jceName);
    }
}
