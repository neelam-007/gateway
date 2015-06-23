package com.l7tech.server.service;

import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Utility class holding a shared lock for both {@link com.l7tech.server.service.ServiceCache} and {@link com.l7tech.server.policy.PolicyCache}.
 * <p/>
 * Introduced to resolve a deadlock cased when the {@code ServiceCache} is refreshed from one thread and, at the same time,
 * the {@code PolicyCache} is refreshed from another thread, causing both of them lock each other.
 * <p/>
 * For more details see <a href="https://jira.l7tech.com:8443/browse/SSG-11400">SSG-11400</a>
 */
public class ServiceAndPolicyCacheSharedLock {
    private static final ReadWriteLock sharedLock = new ReentrantReadWriteLock();

    /**
     * Get the shared lock.
     */
    public static ReadWriteLock getLock() {
        return sharedLock;
    }
}
