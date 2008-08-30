package com.l7tech.server;

import com.l7tech.gateway.common.security.keystore.SsgKeyEntry;

import javax.net.ssl.KeyManager;
import java.io.IOException;

/**
 * Convenience interface for quickly looking up the Gateway's current default SSL key and cert chain.
 */
public interface DefaultKey {
    /**
     * Returns the Ssl signer info containing the default SSL cert chain and private key.
     * <p/>
     * <b>Note:</b> This differs from {@link #getCaInfo()} in that this method will <em>never</em> return null.
     *
     * @return the <code>SignerInfo</code> instance. Never null. 
     * @throws java.io.IOException if there is a problem reading the certificate file or the private key,
     *                             or if no default SSL key is currently designated.
     */
    SsgKeyEntry getSslInfo() throws IOException;

    /**
     * Get Gateway default CA private key and cert chain.
     * <p/>
     * <b>Note:</b> This differs from {@link #getSslInfo()} in that this method may return null.
     *
     * @return SignerInfo for the default CA private key, or null if no default CA key is currently designated.
     */
    SsgKeyEntry getCaInfo();

    /**
     * Get an array containing a single KeyManager implementation which will always present the current
     * default SSL cert as the server cert.
     *
     * @return an array containing a single KeyManager implementation.  Never null or empty.
     */
    KeyManager[] getSslKeyManagers();
}
