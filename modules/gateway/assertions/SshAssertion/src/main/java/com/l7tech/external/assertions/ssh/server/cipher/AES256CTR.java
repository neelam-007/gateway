package com.l7tech.external.assertions.ssh.server.cipher;

import org.apache.sshd.common.Cipher;
import org.apache.sshd.common.NamedFactory;
import org.apache.sshd.common.cipher.BaseCipher;

/**
 * AES256CTR cipher
 */
public class AES256CTR extends BaseCipher {

    /**
     * Named factory for AES256CTR Cipher
     */
    public static class Factory implements NamedFactory<Cipher> {
        public String getName() {
            return "aes256-ctr";
        }
        public Cipher create() {
            return new AES256CTR();
        }
    }

    public AES256CTR() {
        super(16, 32, "AES", "AES/CTR/NoPadding");
    }
}
