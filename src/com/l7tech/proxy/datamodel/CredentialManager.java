/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.proxy.datamodel;

/**
 * Get usernames and passwords from somewhere.  The caller of the manager interface will then
 * typically stuff them into an Ssg object for safe keeping.
 *
 * In the GUI environment, the default CredentialManager will just pop up a login window.
 *
 * User: mike
 * Date: Jun 27, 2003
 * Time: 10:31:45 AM
 */
public interface CredentialManager {
    /**
     * Load credentials for this SSG.  If the SSG already contains credentials they will be
     * overwritten with new ones.  Where the credentials actually come from is up to the CredentialManager
     * implementation; in the GUI environment, it will pop up a login window.
     * @param ssg  the Ssg whose username and password are to be updated
     * @return true if the credentials were updated; false if new credentials couldn't be obtained.
     */
    boolean getCredentials(Ssg ssg);

    /**
     * Notify that the credentials for this SSG have been tried and found to be no good.
     */
    void notifyInvalidCredentials(Ssg ssg);

    /**
     * Unobtrusively notify that a lengthy operation is now in progress.
     * In the GUI environment, this will put up a "Please wait..." dialog.
     * Only one "Please wait..." dialog will be active for a given Ssg.
     */
    void notifyLengthyOperationStarting(Ssg ssg, String message);

    /**
     * Unobtrusively notify that a lengthy operation has completed.
     * Tears down any "Please wait..." dialog.
     */
    void notifyLengthyOperationFinished(Ssg ssg);
}
