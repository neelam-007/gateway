package com.l7tech.common.security;

/**
 * A version of MasterPasswordManager that uses a static master password.
 */
public class MasterPasswordManagerStub extends MasterPasswordManager {
    public MasterPasswordManagerStub(final String masterPassword) {
        super(new MasterPasswordFinder() {
            public char[] findMasterPassword() {
                return masterPassword.toCharArray();
            }
        });
    }
}
