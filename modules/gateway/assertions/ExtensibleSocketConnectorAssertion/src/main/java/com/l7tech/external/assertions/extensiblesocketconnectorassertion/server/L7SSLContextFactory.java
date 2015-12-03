package com.l7tech.external.assertions.extensiblesocketconnectorassertion.server;

import com.l7tech.common.io.SingleCertX509KeyManager;
import com.l7tech.gateway.common.security.keystore.SsgKeyEntry;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.server.security.keystore.SsgKeyStoreManager;
import org.springframework.beans.factory.config.MethodInvokingFactoryBean;

import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

/**
 * Created by IntelliJ IDEA.
 * User: njordan
 * Date: 01/12/11
 * Time: 12:51 PM
 * To change this template use File | Settings | File Templates.
 */
public class L7SSLContextFactory {
    private SsgKeyStoreManager keyStoreManager;
    private TrustManager trustManager;
    private MethodInvokingFactoryBean secureRandomFactory;

    public L7SSLContextFactory(SsgKeyStoreManager keyStoreManager, TrustManager trustManager,
                               MethodInvokingFactoryBean secureRandomFactory) {
        this.keyStoreManager = keyStoreManager;
        this.trustManager = trustManager;
        this.secureRandomFactory = secureRandomFactory;
    }

    public SSLContext getSSLContext(String keyId) throws FindException {
        try {
            String[] parts = keyId.split(":");
            SsgKeyEntry keyEntry = keyStoreManager.lookupKeyByKeyAlias(parts[1], Goid.parseGoid(parts[0]));

            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(new KeyManager[]{new SingleCertX509KeyManager(keyEntry.getCertificateChain(), keyEntry.getPrivate())},
                    new TrustManager[]{trustManager},
                    (SecureRandom) secureRandomFactory.getObject());

            return sslContext;
        } catch (KeyStoreException e) {
            throw new FindException();
        } catch (NoSuchAlgorithmException e) {
            throw new FindException();
        } catch (Exception e) {
            throw new FindException();
        }
    }
}
