/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.proxy.datamodel;

import com.l7tech.common.mime.HybridStashManager;
import com.l7tech.common.mime.StashManager;
import com.l7tech.policy.AssertionRegistry;
import com.l7tech.policy.wsp.WspConstants;

/**
 * Used to obtain datamodel classes.
 */
public class Managers {
    private static CredentialManager credentialManager = null;
    private static BridgeStashManagerFactory stashManagerFactory = new DefaultBridgeStashManagerFactory();
    private static int stashFileUnique = 1; // used to generate unique filenames for stashing large attachments
    private static AssertionRegistry assertionRegistry = null;

    /**
     * Get the CredentialManager.
     * @return the current CredentialManager instance.
     */
    public static CredentialManager getCredentialManager() {
        if (credentialManager == null) {
            credentialManager = CredentialManagerImpl.getInstance();
        }
        return credentialManager;
    }

    /**
     * Change the CredentialManager.
     * @param credentialManager the new CredentialManager to use.
     */
    public static void setCredentialManager(CredentialManager credentialManager) {
        Managers.credentialManager = credentialManager;
    }

    private static synchronized int getStashFileUnique() {
        return stashFileUnique++;
    }

    public static interface BridgeStashManagerFactory {
        StashManager createStashManager();
    }
    
    public static class DefaultBridgeStashManagerFactory implements BridgeStashManagerFactory {
        public StashManager createStashManager() {
            return new HybridStashManager(Ssg.ATTACHMENT_DISK_THRESHOLD, Ssg.ATTACHMENT_DIR,
                                          "att" + getStashFileUnique());
        }
    }

    /** Change the stash manager factory that createStashManager will use from now on. */
    public static void setBridgeStashManagerFactory(BridgeStashManagerFactory factory) {
        if (factory == null) throw new NullPointerException();
        stashManagerFactory = factory;
    }

    /**
     * Obtain a stash manager.  Currently always creates a new {@link HybridStashManager}.
     *
     * @return a new StashManager reader to stash input stream to RAM or disk according to their size.
     */
    public static StashManager createStashManager() {
        return stashManagerFactory.createStashManager();
    }

    public synchronized static AssertionRegistry getAssertionRegistry() {
        if (assertionRegistry == null) {
            assertionRegistry = new AssertionRegistry();
            if (WspConstants.getTypeMappingFinder() == null) {
                // Must be standalone SSB.   Attach default WspReader to assertion registry now
                WspConstants.setTypeMappingFinder(assertionRegistry);
            }
        }
        return assertionRegistry;
    }

    public synchronized static void setAssertionRegistry(AssertionRegistry reg) {
        assertionRegistry = reg;
    }
}
