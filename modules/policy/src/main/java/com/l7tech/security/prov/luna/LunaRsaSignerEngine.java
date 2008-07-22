/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.security.prov.luna;

import com.l7tech.security.prov.RsaSignerEngine;

import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.security.SecureRandom;
import java.util.Calendar;
import java.util.Random;

/**
 * @author mike
 */
public class LunaRsaSignerEngine implements RsaSignerEngine {
    private static final Random random = new SecureRandom();

    private final LunaCmu cmu;
    private final LunaCmu.CmuObject caCert;

    public LunaRsaSignerEngine(String privateKeyAlias)
    {
        try {
            cmu = new LunaCmu();
            caCert = cmu.findCertificateByHandle(privateKeyAlias);
        } catch (LunaCmu.LunaCmuException e) {
            throw new RuntimeException("Unable to process Certificate Signing Request: " + e.getMessage(), e);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Unable to process Certificate Signing Request: " + e.getMessage(), e);
        } catch (LunaCmu.LunaTokenNotLoggedOnException e) {
            throw new RuntimeException("Unable to process Certificate Signing Request: " + e.getMessage(), e);
        }
    }

    public Certificate createCertificate(byte[] pkcs10req, String subject) throws Exception {
        Calendar exp = Calendar.getInstance();
        exp.add(Calendar.DATE, CERT_DAYS_VALID);
        return createCertificate(pkcs10req, subject, exp.getTime().getTime());
    }

    public Certificate createCertificate(byte[] pkcs10req, String subject, long expiration) throws Exception {
        if (subject == null || subject.length() < 1) throw new IllegalArgumentException("Must specify a subject for the new cert");
        long daysValid = CERT_DAYS_VALID;
        if (expiration > 0) {
            long now = System.currentTimeMillis();
            long days = (expiration - now) / (86400 * 1000);
            if (days > 0)
                daysValid = days;
        }

        X509Certificate cert =  cmu.certify(pkcs10req, caCert, (int)daysValid, random.nextLong(), null);
        String gotSubj = cert.getSubjectDN().getName();
        if (!(subject.equalsIgnoreCase(gotSubj)))
            throw new IllegalArgumentException("The CSR requested the subject \"" + gotSubj + "\", but only the subject \"" + subject + "\" is permitted");

        return cert;
    }
}
