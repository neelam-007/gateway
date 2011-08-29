package com.l7tech.external.assertions.ssh.keyprovider;

import java.security.KeyPair;

/**
 * L7 customized PEM SSH host key provider class for use in Apache SSHD.
 * Heavily borrowed from org.apache.sshd.server.keyprovider.PEMGeneratorHostKeyProvider
 */
public class PemSshHostKeyProvider extends AbstractSshHostKeyProvider {
    public PemSshHostKeyProvider() {
    }

    public PemSshHostKeyProvider(String privateKey) {
        super(privateKey);
        String algorithm = SshKeyUtil.getPemPrivateKeyAlgorithm(privateKey);
        if (algorithm != null) {
            setAlgorithm(algorithm);
        }
    }

    public PemSshHostKeyProvider(String privateKey, String algorithm) {
        super(privateKey, algorithm);
    }

    public PemSshHostKeyProvider(String privateKey, String algorithm, int keySize) {
        super(privateKey, algorithm, keySize);
    }

    protected KeyPair doReadKeyPair(String privateKey) throws Exception {
        return SshKeyUtil.doReadKeyPair(privateKey);
    }
}
