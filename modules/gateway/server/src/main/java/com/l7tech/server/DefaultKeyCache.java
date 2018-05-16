package com.l7tech.server;

import com.l7tech.gateway.common.security.keystore.SsgKeyEntry;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.util.Cacheable;
import com.l7tech.util.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.net.ssl.KeyManager;
import java.io.IOException;
import java.security.KeyStoreException;

/**
 * Cached interface for quickly looking up the Gateway's current default SSL key and cert chain.
 * Based off of com.l7tech.server.DefaultKey
 */
public interface DefaultKeyCache {
    /**
     * Returns the Ssl signer info containing the default SSL cert chain and private key.
     * <p/>
     * <b>Note:</b> This differs from {@link #getCaInfo()} in that this method will <em>never</em> return null.
     *
     * @return the <code>SignerInfo</code> instance. Never null.
     * @throws IOException if there is a problem reading the certificate file or the private key,
     *                             or if no default SSL key is currently designated.
     */
    @Cacheable(relevantArg=0,maxAge=5000)
    @NotNull
    SsgKeyEntry getSslInfo() throws IOException;

    /**
     * Get Gateway default CA private key and cert chain, if one is set, otherwise null.
     * <p/>
     * <b>Note:</b> This differs from {@link #getSslInfo()} in that this method may return null.
     *
     * @return SignerInfo for the default CA private key, or null if no default CA key is currently designated.
     */
    @Cacheable(relevantArg=0,maxAge=5000)
    @Nullable
    SsgKeyEntry getCaInfo();

    /**
     * Get the Gateway default audit signing key and cert chain, if one is set, otherwise null.
     *
     * @return SignerInfo for the designated audit signing key, or null if one is not currently configured.
     */
    @Cacheable(relevantArg=0,maxAge=5000)
    @Nullable
    SsgKeyEntry getAuditSigningInfo();

    /**
     * Get the Gateway audit viewer decryption key and cert chain, if one is set, otherwise null.
     * <p/>
     * <b>Note:</b> This differs from {@link #getSslInfo()} in that this method may return null.
     *
     * @return SignerInfo for the designated audit viewer decryption key, or null if one is not currently configured.
     */
    @Cacheable(relevantArg=0,maxAge=5000)
    @Nullable
    SsgKeyEntry getAuditViewerInfo();

    /**
     * Get just the keystore ID and alias of the audit viewer decryption key, if one is set, otherwise null.
     *
     * @return the audit viewer keystore goid and alias, or null if not set.  The keystore goid may be returned as default
     * if it is configured as a wildcard match the actual matching key entry has not been loaded yet.
     */
    @Cacheable(relevantArg=0,maxAge=5000)
    @Nullable
    Pair<Goid, String> getAuditViewerAlias();

    /**
     * Get an array containing a single KeyManager implementation which will always present the current
     * default SSL cert as the server cert.
     *
     * @return an array containing a single KeyManager implementation.  Never null or empty.
     */
    @Cacheable(relevantArg=0,maxAge=5000)
    @NotNull
    KeyManager[] getSslKeyManagers();

    /**
     * Similar to {@link com.l7tech.server.security.keystore.SsgKeyStoreManager#lookupKeyByKeyAlias(String, Goid)} but
     * recognizes a null keyAlias as a request for the default SSL key.  In other respects, this just chains
     * to the other method; see its JavaDoc for more information.
     *
     * @param key  a pair of the alias and the preferredKeystoreId. Use null alias to obtain the default SSL key.
     * preferredKeystoreId is the ID of a keystore to look in first, or DEFAULT_GOID to scan all key stores.
     * For compatibility with systems ugpraded from pre-5.0, an preferredKeystoreId of zero is treated like -1.
     * @return a SignerInfo instance containing a private key and cert chain.  Never null.
     * @throws com.l7tech.objectmodel.ObjectNotFoundException if the requested alias could not be found in any keystore
     * @throws FindException if there is a problem reading key data from the DB.
     * @throws KeyStoreException if there is a problem with the format of some keystore data
     * @throws IOException if there is a problem reading the certificate file or the private key,
     *                             or if no default SSL key is currently designated.
     */
    @Cacheable(relevantArg=0,maxAge=10000)  // 10s
    @NotNull
    SsgKeyEntry lookupKeyByKeyAlias( Pair<String,Goid> key) throws FindException, KeyStoreException, IOException;

}
