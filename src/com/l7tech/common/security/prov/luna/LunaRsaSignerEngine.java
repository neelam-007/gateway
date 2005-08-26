/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.common.security.prov.luna;

import com.l7tech.common.security.RsaSignerEngine;

import java.security.cert.Certificate;
import java.util.Calendar;
import java.util.Random;

/**
 * @author mike
 */
public class LunaRsaSignerEngine implements RsaSignerEngine {
    private final LunaCmu cmu;
    private final LunaCmu.CmuObject caCert;

    public LunaRsaSignerEngine(String privateKeyAlias)
    {
        try {
            cmu = new LunaCmu();
            caCert = cmu.findCertificateByHandle(privateKeyAlias);
        } catch (LunaCmu.LunaCmuException e) {
            throw new RuntimeException("Unable to process Certificate Signing Request: " + e.getMessage(), e);
        }
    }

    public Certificate createCertificate(byte[] pkcs10req, String subject) throws Exception {
        Calendar exp = Calendar.getInstance();
        exp.add(Calendar.DATE, CERT_DAYS_VALID);
        return createCertificate(pkcs10req, subject, exp.getTime().getTime());
    }

    public Certificate createCertificate(byte[] pkcs10req, String subject, long expiration) throws Exception {
        long daysValid = CERT_DAYS_VALID;
        if (expiration > 0) {
            long now = System.currentTimeMillis();
            long days = (expiration - now) / (86400 * 1000);
            if (days > 0)
                daysValid = days;
        }

        return cmu.certify(pkcs10req, caCert, (int)daysValid, new Random().nextLong(), null);
    }
}
