package com.l7tech.server;

import com.l7tech.gateway.common.security.keystore.SsgKeyEntry;
import com.l7tech.objectmodel.FindException;

import javax.net.ssl.KeyManager;
import java.io.IOException;
import java.security.KeyStoreException;

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

    /**
     * Similar to {@link com.l7tech.server.security.keystore.SsgKeyStoreManager#lookupKeyByKeyAlias(String, long)} but
     * recognizes a null keyAlias as a request for the default SSL key.  In other respects, this just chains
     * to the other method; see its JavaDoc for more information.
     *
     * @param keyAlias  the alias of the key to find, or null to obtain the default SSL key.
     * @param preferredKeystoreId  the ID of a keystore to look in first, or -1 to scan all key stores.
     *                             For compatibility with systems ugpraded from pre-5.0, an ID of zero is treated like -1.
     * @return a SignerInfo instance containing a private key and cert chain.  Never null.
     * @throws com.l7tech.objectmodel.ObjectNotFoundException if the requested alias could not be found in any keystore
     * @throws com.l7tech.objectmodel.FindException if there is a problem reading key data from the DB.
     * @throws java.security.KeyStoreException if there is a problem with the format of some keystore data
     * @throws java.io.IOException if there is a problem reading the certificate file or the private key,
     *                             or if no default SSL key is currently designated.
     */
    SsgKeyEntry lookupKeyByKeyAlias(String keyAlias, long preferredKeystoreId) throws FindException, KeyStoreException, IOException;
}
