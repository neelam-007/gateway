package com.l7tech.policy.assertion.ext.password;

import com.l7tech.policy.assertion.ext.ServiceException;

/**
 * Use this service to decrypt password or PEM private key selected in the CustomSecurePasswordPanel.
 */
public interface SecurePasswordServices {

    /**
     * Decrypts the specified password.
     *
     * @param id the ID of the password to decrypt
     * @return the decrypted password. Never null but may be empty.
     * @throws ServiceException if the specified password with give ID cannot be decrypted
     */
    String decryptPassword (String id) throws ServiceException;
}