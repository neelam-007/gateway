/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.proxy.datamodel;

import com.l7tech.proxy.datamodel.exceptions.PolicyLockedException;

import java.io.Serializable;
import java.util.*;
import java.util.logging.Logger;

/**
 * A {@link PolicyManager} that stores policies in memory.  This implementation is synchronized.
 */
public class LocalPolicyManager implements PolicyManager, Serializable {
    private static final Logger logger = Logger.getLogger(LocalPolicyManager.class.getName());
    private HashMap policyMap = new HashMap(); /* Policy cache */
    private Map wildcardMatches = new LinkedHashMap();  /* Begins-with policy matches */
    private transient Map wildcardSearchResults = new WeakHashMap(); /* Speeds up subsequent wildcard searches for the same key */
    private static final Object MISS = new Object(); /* Represents a wildcard search miss. */

    /**
     * Create a LocalPolicyManager with no delegate.
     */
    public LocalPolicyManager() {
    }

    /** Policy map accessor, for xml bean serializer.  Do not call this method. */
    protected synchronized HashMap getPolicyMap() {
        return policyMap;
    }

    /** Policy map mutator, for xml bean deserializer.  Do not call this method. */
    protected synchronized void setPolicyMap(HashMap policyMap) {
        this.policyMap = policyMap;
        wildcardSearchResults.clear();
    }

    /** Wildcard map accessor, for xml bean serializer.  Do not call this method. */
    protected Map getWildcardMatches() {
        return wildcardMatches;
    }

    /** Wildcard map mutator, for xml bean deserializer.  Do not call this method. */
    protected void setWildcardMatches(Map wildcardMatches) {
        this.wildcardMatches = wildcardMatches;
        wildcardSearchResults.clear();
    }

    public synchronized boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof LocalPolicyManager)) return false;

        final LocalPolicyManager that = (LocalPolicyManager)o;

        if (policyMap != null ? !policyMap.equals(that.policyMap) : that.policyMap != null) return false;

        return true;
    }

    public synchronized int hashCode() {
        return (policyMap != null ? policyMap.hashCode() : 0);
    }

    public synchronized void flushPolicy(PolicyAttachmentKey policyAttachmentKey) {
        policyMap.remove(policyAttachmentKey);
        wildcardMatches.remove(policyAttachmentKey);
        wildcardSearchResults.clear();
    }

    public synchronized Policy getPolicy(PolicyAttachmentKey policyAttachmentKey) {
        if (policyAttachmentKey == null) throw new NullPointerException();
        Policy policy = (Policy)policyMap.get(policyAttachmentKey);
        if (policy == null) policy = (Policy)wildcardMatches.get(policyAttachmentKey);
        return policy;
    }

    public synchronized Policy findMatchingPolicy(PolicyAttachmentKey pak) {
        if (pak == null) throw new NullPointerException();

        // Exact match trumps wildcard match
        Policy found = getPolicy(pak);
        if (found != null)
            return found;

        Object result = wildcardSearchResults.get(pak);
        if (result != null) {
            if (result == MISS)
                return null;
            return (Policy)result;
        }

        Set wildcardEntries = wildcardMatches.entrySet();
        for (Iterator i = wildcardEntries.iterator(); i.hasNext();) {
            Map.Entry wcEntry = (Map.Entry)i.next();
            PolicyAttachmentKey wcPak = (PolicyAttachmentKey)wcEntry.getKey();
            Policy policy = (Policy)wcEntry.getValue();

            if (wcPak == null) {
                found = policy; // null wildcard pak = matchall
                break;
            }

            // Currently the only wildcard match type we support is startsWith().

            found = policy; // assume match until ruled out
            final String wcPakUri = wcPak.getUri();
            if (wcPakUri != null && wcPakUri.length() > 0) {
                final String pakUri = pak.getUri();
                if (pakUri == null || pakUri.length() < 1 || !pakUri.startsWith(wcPakUri)) {
                    found = null; // ruled out
                    continue;
                }
            }

            final String wcPakProxyUri = wcPak.getProxyUri();
            if (wcPakProxyUri != null && wcPakProxyUri.length() > 0) {
                final String pakProxyUri = pak.getProxyUri();
                if (pakProxyUri == null || pakProxyUri.length() < 1 || !pakProxyUri.startsWith(wcPakProxyUri)) {
                    found = null; // ruled out
                    continue;
                }
            }

            final String wcPakSa = wcPak.getSoapAction();
            if (wcPakSa != null && wcPakSa.length() > 0) {
                final String pakSa = pak.getSoapAction();
                if (pakSa == null || pakSa.length() < 1 || !pakSa.startsWith(wcPakSa)) {
                    found = null; // ruled out
                    continue;
                }
            }

            if (found != null)
                break;
        }

        if (found == null)
            wildcardSearchResults.put(pak, MISS);
        else
            wildcardSearchResults.put(pak, found);
        return found;
    }

    public synchronized void setPolicy(PolicyAttachmentKey key, Policy policy) throws PolicyLockedException {
        if (key == null) throw new NullPointerException();
        if (policy == null) throw new NullPointerException();
        if (key.isBeginsWithMatch()) {
            policyMap.remove(key);
            wildcardMatches.put(key, policy);
        } else {
            wildcardMatches.remove(key);
            policyMap.put(key, policy);
        }
        wildcardSearchResults.clear();
    }

    public synchronized Set getPolicyAttachmentKeys() {
        Set setCopy = new TreeSet(wildcardMatches.keySet());
        setCopy.addAll(policyMap.keySet());
        return setCopy;
    }

    public synchronized void clearPolicies() {
        policyMap.clear();
        wildcardMatches.clear();
        wildcardSearchResults.clear();
    }
}
