package com.l7tech.external.assertions.ssh.server.keyprovider;

import com.l7tech.util.ExceptionUtils;
import org.apache.sshd.common.keyprovider.AbstractKeyPairProvider;

import java.security.KeyPair;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * L7 customized abstract SSH host key provider class for use by Apache SSHD.
 * Heavily borrowed from org.apache.sshd.server.keyprovider.AbstractGeneratorHostKeyProvider
 */
public abstract class AbstractSshHostKeyProvider extends AbstractKeyPairProvider {

    private static final Logger LOG = Logger.getLogger(AbstractSshHostKeyProvider.class.getName());

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
            LOG.log(Level.INFO, "Unable to read key " + privateKey + ": ", ExceptionUtils.getDebugException(e));
        }
        return null;
    }
}