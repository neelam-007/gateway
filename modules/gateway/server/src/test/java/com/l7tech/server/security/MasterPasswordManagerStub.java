package com.l7tech.server.security;

import com.l7tech.util.Charsets;
import com.l7tech.util.MasterPasswordManager;

/**
 * A version of MasterPasswordManager that uses a static master password.
 */
public class MasterPasswordManagerStub extends MasterPasswordManager {
    public MasterPasswordManagerStub(final String masterPassword) {
        super(new MasterPasswordFinder() {
            public byte[] findMasterPasswordBytes() {
                return masterPassword.getBytes(Charsets.UTF8);
            }
        });
    }
}
