/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.common.util;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.util.logging.Logger;
import java.net.URL;

/**
 * Test certificate downloading.  The SSG must be running on localhost:8080, and must contain
 * a user "testuser" with the password "testpassword".
 *
 * User: mike
 * Date: Jul 15, 2003
 * Time: 3:07:31 PM
 */
public class CertificateDownloaderTest extends TestCase {
    private static Logger log = Logger.getLogger(CertificateDownloaderTest.class.getName());

    public CertificateDownloaderTest(String name) {
        super(name);
    }

    public static Test suite() {
        return new TestSuite(CertificateDownloaderTest.class);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }

    public void testUserUnknown() throws Exception {
        CertificateDownloader cd = new CertificateDownloader(new URL("http://sybok:8080"),
                                                             "tesasdfasdftuser",
                                                             "testpagergassword".toCharArray());
        assertFalse(cd.downloadCertificate());
        assertTrue(cd.getCertificate() != null);
        assertTrue(cd.isUserUnknown());
  }

    public void testSuccess() throws Exception {
        CertificateDownloader cd = new CertificateDownloader(new URL("http://sybok:8080"),
                                                             "mike",
                                                             "asdfasdf".toCharArray());
        assertTrue(cd.downloadCertificate());
        assertTrue(cd.getCertificate() != null);
        assertFalse(cd.isUserUnknown());
        assertTrue(cd.isValidCert());
    }
}
