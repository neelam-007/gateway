/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.proxy.datamodel;

import com.l7tech.common.security.CertificateRequest;
import com.l7tech.common.security.JceProvider;
import com.l7tech.common.security.RsaSignerEngine;
import com.l7tech.common.util.CertUtils;
import com.l7tech.skunkworks.xss4j.Xss4jWrapper;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.security.KeyPair;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.util.logging.Logger;

/**
 * @author mike
 */
public class SsgKeyStoreManagerTest extends TestCase {
    private static Logger log = Logger.getLogger(SsgKeyStoreManagerTest.class.getName());

    public SsgKeyStoreManagerTest(String name) {
        super(name);
    }

    public static Test suite() {
        return new TestSuite(SsgKeyStoreManagerTest.class);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }

    public void testKeyStore() throws Exception {
        final Xss4jWrapper x = new Xss4jWrapper();

        KeyPair kp = JceProvider.generateRsaKeyPair();
        CertificateRequest csr = JceProvider.makeCsr("mike", kp);
        RsaSignerEngine rsaSigner = JceProvider.createRsaSignerEngine("/tomcat4.1/kstores/ca.ks", "tralala", "ssgroot", "tralala", KeyStore.getDefaultType());
        X509Certificate cert = (X509Certificate)rsaSigner.createCertificate(csr.getEncoded(), null);

        System.out.println("Using client cert:" + CertUtils.toString(cert));

        Ssg testSsg = new Ssg(1, "foo.bar.blortch.l7tech.com");
        testSsg.setUsername(CertUtils.extractCommonNameFromClientCertificate(cert));
        testSsg.getRuntime().setCachedPassword("tralala".toCharArray());


        testSsg.getRuntime().getSsgKeyStoreManager().deleteStores();

        testSsg.getRuntime().getSsgKeyStoreManager().saveClientCertificate(kp.getPrivate(), cert, testSsg.getRuntime().getCachedPassword());

        X509Certificate haveCert = testSsg.getClientCertificate();
        assertTrue(haveCert.equals(cert));

        //SsgKeyStoreManager.deleteStores(testSsg);
    }
}
