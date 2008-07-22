/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.security.prov.luna;

import com.l7tech.common.io.CertUtils;
import com.l7tech.util.HexUtils;
import com.l7tech.security.prov.luna.LunaCmu;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;
import java.util.logging.Logger;

/**
 * @author mike
 */
public class LunaCmuTest extends TestCase {
    private static Logger log = Logger.getLogger(LunaCmuTest.class.getName());

    public LunaCmuTest(String name) {
        super(name);
    }

    public static Test suite() {
        return new TestSuite(LunaCmuTest.class);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }

    public void testFindCmu() throws Exception {

        final LunaCmu cmu = new LunaCmu();

        LunaCmu.CmuObject[] things;
        things = cmu.list();
        log.info("Luna CMU probed successfully.  Our partition contains " + things.length + " objects.");


        for (int i = 0; i < things.length; i++) {
            LunaCmu.CmuObject thing = things[i];
            if (thing.getLabel().startsWith("tomcat") || thing.getLabel().startsWith("ssgroot")) {
                System.out.println("Deleting object: " + thing);
                cmu.delete(thing);
            }
        }

        LunaCmu.CmuObject caKey = cmu.generateRsaKeyPair("ssgroot");
        System.out.println("Generated new CA key pair: " + caKey);

        LunaCmu.CmuObject caCert = cmu.generateCaCert(caKey, null, "root.data.l7tech.com");
        System.out.println("Generated new CA cert: " + caCert);

        LunaCmu.CmuObject sslKey = cmu.generateRsaKeyPair("tomcat");
        System.out.println("Generated new SSL key pair: " + sslKey);

        byte[] csr = cmu.requestCertificate(sslKey, "data.l7tech.com");
        X509Certificate sslX509 = cmu.certify(csr, caCert, 365 * 5, 1002, "tomcat");
        LunaCmu.CmuObject sslCert = cmu.findCertificateByHandle("tomcat");
        System.out.println("Generated new SSL cert: "  + sslCert);

        X509Certificate caX509 = cmu.exportCertificate(caCert);
        X509Certificate sslX509_2 = cmu.exportCertificate(sslCert);

        assertTrue(CertUtils.certsAreEqual(sslX509, sslX509_2));

        System.out.println("\nCA cert:\n" + new String(CertUtils.encodeAsPEM(caX509)) + "\n");
        System.out.println("\nSSL cert:\n" + new String(CertUtils.encodeAsPEM(sslX509)) + "\n");

        csr = HexUtils.unHexDump(CSR_BYTES);
        X509Certificate ccert = cmu.certify(csr, caCert, 365 * 5, new Random().nextLong(), null);
        System.out.println("Successfully produced a signed client cert: " + ccert.getSubjectDN().toString());
        ArrayList props = CertUtils.getCertProperties(ccert);
        for (Iterator i = props.iterator(); i.hasNext();) {
            String[] prop = (String[])i.next();
            String key = prop[0];
            String val = prop[1];
            System.out.println(key + ": " + val);
        }


        System.out.println("Deleting new key pair");
        //cmu.delete(caKey);
        //cmu.delete(caKey); // it should succeed but do nothing if run a second time

    }

    public static final String CSR_BYTES = "3082014c3081b6020100300f310d300b060355040313046d696b6530819f300d06092" +
            "a864886f70d010101050003818d0030818902818100a5ca439ea3dc3659d4314bcc6f2901338c98c8d0c05a63d5af3c65ab8" +
            "258f6815f53c5fb8c2c564a45412bc645ee91ca7dbc4fab415b7c16008a9992b3acd81257060f49682623620a90669d16571" +
            "1fe1b05c4d770ede285c6597ca21691d0fbfd15c4e28dc328b2f6383676e0659960f1dde83b830f181ccf88a73e827560350" +
            "203010001300d06092a864886f70d01010505000381810020214b066ce75d0f7ed12a46cd8887f9dea50b369b946ef896276" +
            "bf382aec7f6dbb27cf6d462873c74ab6387e8511c56caee8314fe3b93f709b913e599819821297c73933e933b8ff46d9381d" +
            "e0ea45a769dc4ab45fb3471e0605216e10c9a76d2d42a379033dac545da03dba64e75d6350f358b100dad2527cd0a74e35bf159";
}
