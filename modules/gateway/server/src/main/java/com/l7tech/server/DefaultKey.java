package com.l7tech.server;

import com.l7tech.gateway.common.security.keystore.SsgKeyEntry;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.util.Pair;

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
     * Get Gateway default CA private key and cert chain, if one is set, otherwise null.
     * <p/>
     * <b>Note:</b> This differs from {@link #getSslInfo()} in that this method may return null.
     *
     * @return SignerInfo for the default CA private key, or null if no default CA key is currently designated.
     */
    SsgKeyEntry getCaInfo();

    /**
     * Get the Gateway default audit signing key and cert chain, if one is set, otherwise null.
     *
     * @return SignerInfo for the designated audit signing key, or null if one is not currently configured.
     */
    SsgKeyEntry getAuditSigningInfo();

    /**
     * Get the Gateway audit viewer decryption key and cert chain, if one is set, otherwise null.
     * <p/>
     * <b>Note:</b> This differs from {@link #getSslInfo()} in that this method may return null.
     *
     * @return SignerInfo for the designated audit viewer decryption key, or null if one is not currently configured.
     */
    SsgKeyEntry getAuditViewerInfo();

    /**
     * Get just the keystore ID and alias of the audit viewer decryption key, if one is set, otherwise null.
     *
     * @return the audit viewer keystore goid and alias, or null if not set.  The keystore goid may be returned as default
     * if it is configured as a wildcard match the actual matching key entry has not been loaded yet.
     */
    Pair<Goid, String> getAuditViewerAlias();

    /**
     * Get an array containing a single KeyManager implementation which will always present the current
     * default SSL cert as the server cert.
     *
     * @return an array containing a single KeyManager implementation.  Never null or empty.
     */
    KeyManager[] getSslKeyManagers();

    /**
     * Similar to {@link com.l7tech.server.security.keystore.SsgKeyStoreManager#lookupKeyByKeyAlias(String, Goid)} but
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
    SsgKeyEntry lookupKeyByKeyAlias(String keyAlias, Goid preferredKeystoreId) throws FindException, KeyStoreException, IOException;
}
