/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.proxy.datamodel;

import com.l7tech.common.util.CausedIOException;
import com.l7tech.common.xml.InvalidDocumentFormatException;
import com.l7tech.common.xml.saml.SamlHolderOfKeyAssertion;
import com.l7tech.proxy.ConfigurationException;
import com.l7tech.proxy.datamodel.exceptions.*;
import com.l7tech.proxy.message.PolicyApplicationContext;
import com.l7tech.proxy.util.PolicyServiceClient;

import javax.net.ssl.SSLHandshakeException;
import java.io.IOException;
import java.io.Serializable;
import java.net.PasswordAuthentication;
import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.logging.Logger;
import java.util.HashMap;
import java.util.Set;
import java.util.TreeSet;

/**
 * A {@link PolicyManager} that stores policies in memory.  This implementation is synchronized.
 */
public class LocalPolicyManager implements PolicyManager, Serializable {
    private static final Logger log = Logger.getLogger(LocalPolicyManager.class.getName());
    public static final String PROPERTY_LOGPOLICIES    = "com.l7tech.proxy.datamodel.logPolicies";

    private PolicyManager delegate = null;
    private transient HashMap policyMap = new HashMap(); /* Policy cache */

    /**
     * Create a LocalPolicyManager with no delegate.
     */
    public LocalPolicyManager() {
    }

    /**
     * Create a LocalPolicyManager that will get policies from the specified delegate if there is a local cache miss.
     *
     * @param delegate PolicyManger to use if the current PolicyManager does not find a policy.
     */
    public LocalPolicyManager(PolicyManager delegate) {
        this.delegate = delegate;
    }

    public synchronized Set getPolicyAttachmentKeys() {
        Set setCopy = new TreeSet(policyMap.keySet());
        if (delegate != null) // mix in delegate's immediately available policies as immediately-available from us
            setCopy.addAll(delegate.getPolicyAttachmentKeys());
        return setCopy;
    }

    public synchronized void setPolicy(PolicyAttachmentKey key, Policy policy ) {
        policyMap.put(key, policy);
    }

    public synchronized Policy getPolicy(PolicyAttachmentKey policyAttachmentKey) throws IOException {
        Policy policy = (Policy) policyMap.get(policyAttachmentKey);
        if (policy == null && delegate != null)
            policy = delegate.getPolicy(policyAttachmentKey);
        if (policy != null) {
            if (LogFlags.logPolicies)
                log.info("Found a policy for this request: " + policy.getAssertion());
            else
                log.info("Found a policy for this request");
        } else
            log.info("No policy found for this request");
        return policy;
    }

    public synchronized void flushPolicy(PolicyAttachmentKey policyAttachmentKey) {
        policyMap.remove(policyAttachmentKey);
        if (delegate != null)
            delegate.flushPolicy(policyAttachmentKey);
    }

    /**
     * Clear policies in this LocalPolicyManager.  Does not affect the delegate.
     */
    public synchronized void clearPolicies() {
        policyMap.clear();
    }

    private static class LogFlags {
        private static final boolean logPolicies = Boolean.getBoolean(PROPERTY_LOGPOLICIES);
    }
}
