package com.l7tech.security.prov.rsa;

import com.l7tech.security.prov.ProviderUtil;
import com.l7tech.util.ConfigFactory;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.Functions.Unary;
import com.l7tech.util.Functions.UnaryVoid;
import com.l7tech.util.Option;
import com.l7tech.util.Pair;
import com.rsa.jsafe.crypto.CryptoJ;
import com.rsa.jsafe.crypto.JSAFE_InvalidUseException;
import com.rsa.jsafe.provider.JsafeJCE;
import com.rsa.jsse.JsseProvider;
import com.rsa.jsse.SSLSessionCache;
import org.jetbrains.annotations.NotNull;

import javax.net.ssl.SSLContext;
import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.NoSuchAlgorithmException;
import java.security.Provider;
import java.security.spec.AlgorithmParameterSpec;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.l7tech.util.Option.none;

/**
 * Wrapper that takes care of configuring Crypto-J.
 * <p/>
 * We will always expect to find the FIPS version of the Crypto-J JCM in our context classloader.
 */
class CryptoJWrapper {
    private static final Logger logger = Logger.getLogger(CryptoJWrapper.class.getName());

    public static final String PROP_DISABLE_BLACKLISTED_SERVICES = "com.l7tech.security.prov.rsa.disableServices";

    static final boolean DISABLE_BLACKLISTED_SERVICES = ConfigFactory.getBooleanProperty( PROP_DISABLE_BLACKLISTED_SERVICES, true );

    @SuppressWarnings({"unchecked"})
    private static final Collection<Pair<String,String>> SERVICE_BLACKLIST = Collections.unmodifiableCollection(Arrays.asList(
            new Pair<String,String>( "CertificateFactory", "X.509" ),
            new Pair<String,String>( "KeyStore", "PKCS12" ),
            new Pair<String,String>( "CertPathBuilder", "PKIX" ),
            new Pair<String,String>( "CertPathValidator", "PKIX" ),
            new Pair<String,String>( "CertStore", "Collection" )
    ));

    final Provider provider;
    final NonLeakingSsljSessionCache ssljSessionCache;
    final byte[] ssljSessionCacheContextId = { 1 };

    CryptoJWrapper(boolean fips) throws ClassNotFoundException, IllegalAccessException, InstantiationException, NoSuchFieldException, MalformedURLException {
        this.provider = new JsafeJCE();
        if ( DISABLE_BLACKLISTED_SERVICES ) {
            ProviderUtil.configureProvider(SERVICE_BLACKLIST, provider);
        }
        this.ssljSessionCache = new NonLeakingSsljSessionCache();
    }

    boolean isFIPS140Compliant() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        return CryptoJ.isFIPS140Compliant();
    }

    boolean isInFIPS140Mode() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        return CryptoJ.isInFIPS140Mode();
    }

    void setMode(int mode) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        try {
            CryptoJ.setMode( mode );
        } catch ( JSAFE_InvalidUseException e ) {
            throw new InvocationTargetException( e );
        }
    }

    Object getVersion() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException, NoSuchFieldException {
        return CryptoJ.CRYPTO_J_VERSION;
    }

    Provider getJsseProvider() throws IllegalAccessException, InstantiationException {
        return new JsseProvider();
    }

    void resetDebug() {
        com.rsa.jsse.engine.util.Debug.debugInitialize();
    }

    AlgorithmParameterSpec createGcmParameterSpec(int authTagLenBytes, long authenticatedDataLenBytes, byte[] iv) throws NoSuchAlgorithmException {
        return new com.rsa.jsafe.provider.GCMParameterSpec( authTagLenBytes, authenticatedDataLenBytes, iv );
    }

    // Configure the specified SSLContext to use our custom global SSL-J session cache.
    // The specified SSL context must be one created using the RsaJsse provider.
    void attachSessionCache( @NotNull SSLContext sslContext ) {
        SSLSessionCache.setExternalSessionCache( sslContext, ssljSessionCacheContextId, ssljSessionCache );
    }
}
