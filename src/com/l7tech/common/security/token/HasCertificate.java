package com.l7tech.common.security.token;

import java.security.cert.X509Certificate;

public interface HasCertificate {
    X509Certificate getCertificate();
}
