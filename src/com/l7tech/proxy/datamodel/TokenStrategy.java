/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.proxy.datamodel;

import com.l7tech.common.security.token.SecurityToken;
import com.l7tech.common.security.token.SecurityTokenType;
import com.l7tech.proxy.datamodel.exceptions.*;

import java.io.IOException;
import java.security.GeneralSecurityException;

/**
 * Interface for classes that retrieve {@link SecurityToken}s.
 * <p>
 * This class uses the term "immediately available" to denote whether a {@link SecurityToken}
 * is present or easy to acquire:
 * <p>
 * Tokens should be considered to be Immediately Available if they are:
 * <ul>
 * <li>Cached in memory
 * <li>On disk, either unencrypted or with the required passphrase already known
 * <li>In a local database, even if the database can only be accessed across a network
 * </ul>
 *
 * <p>
 *
 * A token should <em>not</em> be considered to be immediately available if it might take an unpredictably long time
 * to acquire one, or if the acquisition might not succeed. For example:
 * <ul>
 * <li>The token is present but encrypted; would need to prompt user for passphrase
 * <li>The token can only be acquired after communication with an external service
 * </ul>
 */
public interface TokenStrategy {
    /**
     * Returns a SecurityToken of the appropriate type if one is available or can be obtained by any means.
     * If a SecurityToken cannot be obtained, an exception is thrown.
     *
     * <p>
     * @return a SecurityToken or throws an exception. Never null.
     */
    SecurityToken getOrCreate() throws OperationCanceledException, GeneralSecurityException, IOException, ClientCertificateException, KeyStoreCorruptException, PolicyRetryableException, BadCredentialsException;

    /**
     * Return a SecurityToken if it is immediately available, or null if it is not.
     * <p>
     * @return a SecurityToken if it is immediately available, or null if it is not.
     */
    SecurityToken getIfPresent();

    /**
     * @return the {@link SecurityTokenType} of the token that would be returned from {@link #getIfPresent} and {@link #getOrCreate()}.
     */
    SecurityTokenType getType();

    /**
     * Report that the security token was not accepted by the recipient.  The token strategy might wish to
     * forget any cached token and get a new one.
     */
    void onTokenRejected();
}
