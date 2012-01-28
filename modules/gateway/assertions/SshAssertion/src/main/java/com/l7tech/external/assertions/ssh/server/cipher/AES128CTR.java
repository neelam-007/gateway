package com.l7tech.external.assertions.ssh.server.cipher;

import org.apache.sshd.common.Cipher;
import org.apache.sshd.common.NamedFactory;
import org.apache.sshd.common.cipher.BaseCipher;

/**
 * AES128CTR cipher
 */
public class AES128CTR extends BaseCipher {

    /**
     * Named factory for AES128CTR Cipher
     */
    public static class Factory implements NamedFactory<Cipher> {
        public String getName() {
            return "aes128-ctr";
        }
        public Cipher create() {
            return new AES128CTR();
        }
    }

    public AES128CTR() {
        super(16, 16, "AES", "AES/CTR/NoPadding");
    }
}
