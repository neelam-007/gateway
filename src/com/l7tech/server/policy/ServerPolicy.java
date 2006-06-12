/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.server.policy;

import com.l7tech.server.policy.assertion.ServerAssertion;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.common.util.Closeable;

import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.io.IOException;

/**
 * Ensures that {@link ServerAssertion#close} can be called safely when no more traffic will arrive by
 * acquiring the read lock when messages are processed, and the write lock when closing this policy. 
 */
public class ServerPolicy implements Closeable {
    private final ServerAssertion rootAssertion;
    private final ReadWriteLock lock = new ReentrantReadWriteLock(false);
    private boolean closed = false;

    public ServerPolicy(ServerAssertion rootAssertion) {
        this.rootAssertion = rootAssertion;
    }

    public AssertionStatus checkRequest(PolicyEnforcementContext context) throws PolicyAssertionException, IOException {
        try {
            lock.readLock().lock();
            if (closed) throw new ServerPolicyException(rootAssertion.getAssertion(), "Request arrived after policy closed");
            return rootAssertion.checkRequest(context);
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Closes this policy.  May block until all message traffic passing through this policy has concluded.
     */
    public void close() {
        try {
            lock.writeLock().lock();
            closed = true;
            rootAssertion.close();
        } finally {
            lock.writeLock().unlock();
        }
    }
}
