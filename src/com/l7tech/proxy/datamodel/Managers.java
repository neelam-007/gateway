/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.proxy.datamodel;

/**
 * Used to obtain datamodel classes.
 *
 * User: mike
 * Date: Jun 3, 2003
 * Time: 2:45:08 PM
 */
public class Managers {
    private static CredentialManager credentialManager = null;

    /**
     * Get the PolicyManager.
     * @return the PolicyManager instance.
     */
    public static PolicyManager getPolicyManager() {
        return PolicyManagerImpl.getInstance();
    }

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
}
