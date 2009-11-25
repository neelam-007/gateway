package com.l7tech.server.processcontroller.patching;

import java.security.cert.X509Certificate;
import java.util.Set;

public interface PatchTrustStore {
    Set<X509Certificate> getTrustedCerts();
}
