/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.proxy.datamodel;

import com.l7tech.proxy.datamodel.exceptions.OperationCanceledException;

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
     */
    void getCredentials(Ssg ssg) throws OperationCanceledException;

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

    /**
     * Notify the user that the key store for the given Ssg has been damaged.
     * This should _not_ be used in cases where the user merely mistyped the password to decrypt his
     * private key.  The user should be given the option of either canceling, which will abort the operation,
     * or continuing, which will cause the invalid keystore to be deleted (which will require the SSG admin to revoke
     * the client cert, if the only copy of it's private key is now gone).
     *
     * Whatever decision the user makes should be remembered for the rest of this session.
     *
     * @param ssg
     * @throws OperationCanceledException if the user does not wish to delete the invalid keystore
     */
    void notifyKeyStoreCorrupt(Ssg ssg) throws OperationCanceledException;

    /**
     * Notify the user that a client certificate has already been issued for his account.
     * At this point there is nothing the user can do except try a different account, or contact
     * his Gateway administrator and beg to have the lost certificate revoked from the database.
     *
     * @param ssg
     */
    void notifyCertificateAlreadyIssued(Ssg ssg);

    /**
     * Notify the user that an SSL connection to the SSG could not be established because the hostname did not match
     * the one in the certificate.
     *
     * @param ssg
     * @param whatWeWanted  the expected hostname, equal to ssg.getSsgAddress()
     * @param whatWeGotInstead  the hostname in the peer's certificate
     */
    void notifySsgHostnameMismatch(Ssg ssg, String whatWeWanted, String whatWeGotInstead);
}
