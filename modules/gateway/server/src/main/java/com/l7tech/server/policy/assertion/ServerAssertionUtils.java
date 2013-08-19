package com.l7tech.server.policy.assertion;

import com.l7tech.gateway.common.security.keystore.SsgKeyEntry;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.ObjectNotFoundException;
import com.l7tech.policy.assertion.PrivateKeyable;
import com.l7tech.security.xml.SignerInfo;
import com.l7tech.server.DefaultKey;
import com.l7tech.server.security.keystore.SsgKeyStoreManager;
import com.l7tech.util.ExceptionUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.BeanFactory;

import java.io.IOException;
import java.security.KeyStoreException;
import java.security.PrivateKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.X509Certificate;

/**
 * Created by IntelliJ IDEA.
 * User: megery
 *
 * A Collection of Server Assertion related common methods.
 */
public class ServerAssertionUtils {
    /**
     * Get the SignerInfo (cert chain and private key) to use for the specified object.
     * If the object is an instance of PrivateKeyable that requests a specific private key, that private key will
     * be returned.  Otherwise the default private key will be returned.
     *
     * @param ctx  the Spring BeanFactory.  Required.
     * @param maybePrivateKeyable  an Object that might be an instance of PrivateKeyable.  Optional.
     * @return The SslSignerInfo to use for the specified object.  Never null.
     * @throws java.security.KeyStoreException if there is a problem loading the requested cert chain and private key.
     */
    @NotNull
    public static SignerInfo getSignerInfo(BeanFactory ctx, Object maybePrivateKeyable) throws KeyStoreException {
        try {
            if (maybePrivateKeyable instanceof PrivateKeyable) {
                PrivateKeyable keyable = (PrivateKeyable)maybePrivateKeyable;
                if (!keyable.isUsesDefaultKeyStore()) {
                    final Goid keystoreId = keyable.getNonDefaultKeystoreId();
                    final String keyAlias = keyable.getKeyAlias();
                    com.l7tech.server.security.keystore.SsgKeyStoreManager sksm =
                            ctx.getBean("ssgKeyStoreManager", SsgKeyStoreManager.class);
                    SsgKeyEntry keyEntry = sksm.lookupKeyByKeyAlias(keyAlias, keystoreId);
                    X509Certificate[] certChain = keyEntry.getCertificateChain();
                    PrivateKey privateKey = keyEntry.getPrivateKey();
                    return new SignerInfo(privateKey, certChain);
                }
            }

            // Default keystore
            DefaultKey ku = ctx.getBean("defaultKey", DefaultKey.class);
            return ku.getSslInfo();
        } catch (IOException e) {
            throw new KeyStoreException("Can't read the keystore for outbound message decoration: " + ExceptionUtils.getMessage(e), e);
        } catch (ObjectNotFoundException e) {
            throw new KeyStoreException("Can't find private key for outbound message decoration: " + ExceptionUtils.getMessage(e), e);
        } catch (FindException e) {
            throw new KeyStoreException("Can't read the keystore for outbound message decoration: " + ExceptionUtils.getMessage(e), e);
        } catch (UnrecoverableKeyException e) {
            throw new KeyStoreException("Can't read the keystore for outbound message decoration: " + ExceptionUtils.getMessage(e), e);
        }
    }

    public static boolean isPreferredSigner( final Object maybePrivateKeyable ) {
        boolean preferred = false;

        if (maybePrivateKeyable instanceof PrivateKeyable) {
            PrivateKeyable keyable = (PrivateKeyable)maybePrivateKeyable;
            preferred = !keyable.isUsesDefaultKeyStore();
        }

        return preferred;
    }
}
