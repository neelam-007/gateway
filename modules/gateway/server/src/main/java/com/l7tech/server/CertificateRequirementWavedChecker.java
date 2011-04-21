/**
 * Copyright (C) 2008, Layer 7 Technologies Inc.
 * @author darmstrong
 */
package com.l7tech.server;

import com.l7tech.server.admin.AdminSessionManager;
import com.l7tech.util.Functions;
import org.springframework.beans.factory.InitializingBean;

import java.util.concurrent.Callable;

/**
 * Allow appropriate callers to signal that a user is not required to present their cert.
 * See {@link AdminSessionManager#authenticate(com.l7tech.policy.assertion.credential.LoginCredentials)}
 */
public class CertificateRequirementWavedChecker implements InitializingBean {
    @Override
    public void afterPropertiesSet() throws Exception {
        AdminSessionManager.setCertRequirementWavedChecker(new Functions.Nullary<Boolean>() {
            @Override
            public Boolean call() {
                return certRequirementWavedChecker.get();
            }
        });
    }

    public <T> T doWithCertRequirementWaved(Callable<T> callable) throws Exception {
        final boolean wasCertRequirementWaved = certRequirementWavedChecker.get();
        try {
            certRequirementWavedChecker.set(true);
            return callable.call();
        } finally {
            certRequirementWavedChecker.set(wasCertRequirementWaved);
        }
    }

    // - PRIVATE

    private static final ThreadLocal<Boolean> certRequirementWavedChecker = new ThreadLocal<Boolean>() {
        @Override
        protected Boolean initialValue() {
            return false;
        }
    };
}
