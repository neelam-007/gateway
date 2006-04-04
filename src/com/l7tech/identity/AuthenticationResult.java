package com.l7tech.identity;

import org.apache.commons.collections.LRUMap;

import java.security.cert.X509Certificate;

/**
 * Created on successful authentication events.  Semi-threadsafe (mutators are synchronized).  Used in
 */
public final class AuthenticationResult {
    public static final AuthenticationResult AUTHENTICATED_UNKNOWN_USER = new AuthenticationResult();

    private AuthenticationResult() {
        user = null;
        certSignedByStaleCA = false;
    }

    public AuthenticationResult(User user, X509Certificate authenticatedCert, boolean certWasSignedByStaleCA) {
        this.user = user;
        if (user == null) throw new NullPointerException();
        this.certSignedByStaleCA = certWasSignedByStaleCA;
        this.authenticatedCert = authenticatedCert;
        if (certSignedByStaleCA && authenticatedCert == null) throw new IllegalArgumentException("Must include the stale cert if there is one");
    }

    public AuthenticationResult(User user) {
        this(user, null, false);
    }

    public User getUser() {
        if (user == null) throw new UnsupportedOperationException("Unknown authenticated user");
        return user;
    }

    public boolean isCertSignedByStaleCA() {
        return certSignedByStaleCA;
    }

    public synchronized X509Certificate getAuthenticatedCert() {
        return authenticatedCert;
    }

    public synchronized void setAuthenticatedCert(X509Certificate authenticatedCert) {
        this.authenticatedCert = authenticatedCert;
    }

    public synchronized void setCachedGroupMembership(Group group, Boolean isMember) {
        if (authorizedGroups == null) authorizedGroups = new LRUMap(Integer.getInteger(this.getClass().getName() + ".cacheSize", 50).intValue());
        authorizedGroups.put(group, isMember);
    }

    public synchronized Boolean getCachedGroupMembership(Group group) {
        if (authorizedGroups == null) return Boolean.FALSE;
        return (Boolean)authorizedGroups.get(group);
    }

    public long getTimestamp() {
        return timestamp;
    }

    private final User user;
    private final long timestamp = System.currentTimeMillis();
    private final boolean certSignedByStaleCA;

    private LRUMap authorizedGroups;
    private X509Certificate authenticatedCert;
}
