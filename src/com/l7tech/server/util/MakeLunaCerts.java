/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.server.util;

import com.chrysalisits.crypto.LunaCertificateX509;
import com.l7tech.common.util.ExceptionUtils;

import java.security.KeyStore;
import java.security.KeyPairGenerator;
import java.security.KeyPair;
import java.security.cert.X509Certificate;
import java.util.Calendar;
import java.util.Date;
import java.math.BigInteger;
import java.io.FileOutputStream;

/**
 * @author mike
 */
public class MakeLunaCerts {
    private static final String USAGE = "Usage: MakeLunaCerts [-f] ssghostname.company.com\n\n  -f    Force overwrite of existing certificate(s)";

    public static void main(String[] args) {
        try {
            realMain(args);
            System.exit(0);
        } catch (Exception e) {
            System.err.println("ERROR: " + ExceptionUtils.getMessage(e));
            System.exit(1);
        }
    }

    public static void realMain(String[] args) throws Exception {
        if (args.length < 1) throw new IllegalArgumentException(USAGE);

        String hostname = null;
        boolean force = false;
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if ("-f".equalsIgnoreCase(arg))
                force = true;
            else if (hostname != null)
                throw new IllegalArgumentException(USAGE);
            else
                hostname = arg;
        }

        if (hostname == null || hostname.trim().length() < 1) throw new IllegalArgumentException(USAGE);

        System.out.println("Connecting to Luna KeyStore... ");
        KeyStore ks = KeyStore.getInstance("Luna");
        ks.load(null, null);

        if (ks.getKey("tomcat", null) != null || ks.getKey("ssgroot", null) != null) {
            if (!force)
                throw new RuntimeException("SSG Certificates already present on this KeyStore.\n       Use -f switch to force them to be overwritten.");
            System.out.println("Deleting existing certificates...");
            ks.deleteEntry("tomcat");
            ks.deleteEntry("ssgroot");
        }

        final Calendar cal = Calendar.getInstance();
        Date start = cal.getTime();
        cal.add(Calendar.YEAR, 5);
        Date sslExpires = cal.getTime();
        cal.add(Calendar.YEAR, 20);
        Date rootExpires = cal.getTime();

        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");

        // Generate CA certificate
        {
            final String dn = "cn=root." + hostname;
            System.out.println("Generating new root certificate: " + dn);
            kpg.initialize(1024);
            KeyPair rootKp = kpg.generateKeyPair();
            LunaCertificateX509 rootCert = LunaCertificateX509.SelfSign(rootKp, dn, new BigInteger("1001"), start, rootExpires);
            ks.setKeyEntry("ssgroot", rootKp.getPrivate(), null, new X509Certificate[] { rootCert });

            System.out.println("Generated and saved a root certificate: " + rootCert.getSubjectDN().toString());

            new FileOutputStream("ca.cer").write(rootCert.getEncoded());
            System.out.println("Root cert exported to ca.cer in current directory");
        }

        // Generate SSL certificate
        {
            final String dn = "cn=" + hostname;
            System.out.println("Generating new SSL certificate: " + dn);
            kpg.initialize(1024);
            KeyPair sslKp = kpg.generateKeyPair();
            LunaCertificateX509 sslCert = LunaCertificateX509.SelfSign(sslKp, dn, new BigInteger("1002"), start, sslExpires);
            ks.setKeyEntry("tomcat", sslKp.getPrivate(), null, new X509Certificate[] { sslCert });

            System.out.println("Generated and saved an SSL certificate: " + sslCert.getSubjectDN().toString());

            new FileOutputStream("ssl.cer").write(sslCert.getEncoded());
            System.out.println("SSL cert exported to ssl.cer in current directory");
        }

        System.out.println("Success.");
    }
}
