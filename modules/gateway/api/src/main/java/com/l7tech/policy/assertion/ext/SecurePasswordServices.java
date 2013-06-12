package com.l7tech.policy.assertion.ext;

/**
 * Use this service to decrypt password or PEM private key selected in the CustomSecurePasswordPanel.
 */
public interface SecurePasswordServices {

    /**
     * Decrypts the specified password.
     *
     * @param oid the OID of the password to decrypt
     * @return the decrypted password. Never null but may be empty.
     * @throws ServiceException if the specified password with give OID cannot be decrypted
     */
    String decryptPassword (long oid) throws ServiceException;
}