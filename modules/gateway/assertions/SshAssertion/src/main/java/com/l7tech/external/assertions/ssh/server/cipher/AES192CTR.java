package com.l7tech.external.assertions.ssh.server.cipher;

import org.apache.sshd.common.Cipher;
import org.apache.sshd.common.NamedFactory;
import org.apache.sshd.common.cipher.BaseCipher;

/**
 * AES192CTR cipher
 */
public class AES192CTR extends BaseCipher {

    /**
     * Named factory for AES192CTR Cipher
     */
    public static class Factory implements NamedFactory<Cipher> {
        public String getName() {
            return "aes192-ctr";
        }
        public Cipher create() {
            return new AES192CTR();
        }
    }

    public AES192CTR() {
        super(16, 24, "AES", "AES/CTR/NoPadding");
    }
}
