/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.common.util;

import com.l7tech.common.http.SimpleHttpClient;
import com.l7tech.common.http.prov.jdk.UrlConnectionHttpClient;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.net.URL;

/**
 * Test certificate downloading.  The SSG must be running on sybok:8080, and must contain
 * a user "testuser" with the password "testpassword".
 */
public class CertificateDownloaderTest extends TestCase {

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
        CertificateDownloader cd = new CertificateDownloader(new SimpleHttpClient(new UrlConnectionHttpClient()),
                                                             new URL("http://sybok:8080"),
                                                             "tesasdfasdftuser",
                                                             "testpagergassword".toCharArray());
        assertNotNull(cd.downloadCertificate());
        assertFalse(cd.isValidCert());
        assertTrue(cd.isUncheckablePassword());
  }

    public void testSuccess() throws Exception {
        CertificateDownloader cd = new CertificateDownloader(new SimpleHttpClient(new UrlConnectionHttpClient()),
                                                             new URL("http://sybok:8080"),
                                                             "mike",
                                                             "asdfasdf".toCharArray());
        assertNotNull(cd.downloadCertificate());
        assertTrue(cd.isValidCert());
        assertFalse(cd.isUncheckablePassword());
    }
}
