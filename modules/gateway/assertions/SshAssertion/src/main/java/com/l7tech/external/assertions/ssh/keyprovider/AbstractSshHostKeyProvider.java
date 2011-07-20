package com.l7tech.external.assertions.ssh.keyprovider;

import org.apache.sshd.common.keyprovider.AbstractKeyPairProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.KeyPair;

/**
 * L7 customized abstract SSH host key provider class for use by Apache SSHD.
 * Heavily borrowed from org.apache.sshd.server.keyprovider.AbstractGeneratorHostKeyProvider
 */
public abstract class AbstractSshHostKeyProvider extends AbstractKeyPairProvider {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractSshHostKeyProvider.class);

    private String privateKey;
    private String algorithm = "DSA";
    private int keySize;
    private KeyPair keyPair;

    protected AbstractSshHostKeyProvider() {
    }

    protected AbstractSshHostKeyProvider(String privateKey) {
        this.privateKey = privateKey;
    }

    protected AbstractSshHostKeyProvider(String privateKey, String algorithm) {
        this.privateKey = privateKey;
        this.algorithm = algorithm;
    }

    protected AbstractSshHostKeyProvider(String privateKey, String algorithm, int keySize) {
        this.privateKey = privateKey;
        this.algorithm = algorithm;
        this.keySize = keySize;
    }

    public String getPrivateKey() {
        return privateKey;
    }

    public void setPrivateKey(String privateKey) {
        this.privateKey = privateKey;
    }

    public String getAlgorithm() {
        return algorithm;
    }

    public void setAlgorithm(String algorithm) {
        this.algorithm = algorithm;
    }

    public int getKeySize() {
        return keySize;
    }

    public void setKeySize(int keySize) {
        this.keySize = keySize;
    }

    protected abstract KeyPair doReadKeyPair(String privateKey) throws Exception;

    public synchronized KeyPair[] loadKeys() {
        if (keyPair == null) {
            if (privateKey != null) {
                keyPair = readKeyPair(privateKey);
            }
            if (keyPair == null) {
                return new KeyPair[0];
            }
        }
        return new KeyPair[] { keyPair };
    }

    private KeyPair readKeyPair(String privateKey) {
        try {
            return doReadKeyPair(privateKey);
        } catch (Exception e) {
            LOG.info("Unable to read key {}: {}", privateKey, e);
        }
        return null;
    }
}