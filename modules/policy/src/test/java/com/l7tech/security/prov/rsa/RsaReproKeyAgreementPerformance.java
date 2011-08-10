package com.l7tech.security.prov.rsa;

import com.l7tech.test.BenchmarkRunner;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.Pair;
import com.l7tech.util.SyspropUtil;

import javax.crypto.KeyAgreement;
import javax.crypto.interfaces.DHPublicKey;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.*;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 */
public class RsaReproKeyAgreementPerformance {
    private static final Logger logger = Logger.getLogger(RsaReproKeyAgreementPerformance.class.getName());
    private static final String CLASSNAME_PROVIDER = "com.rsa.jsafe.provider.JsafeJCE";

    private static final Collection<Pair<String,String>> SERVICE_BLACKLIST = Collections.unmodifiableCollection(Arrays.asList(
            new Pair<String,String>( "CertificateFactory", "X.509" ),
            new Pair<String,String>( "KeyStore", "PKCS12" ),
            new Pair<String,String>( "KeyAgreement", "DH" ),
            new Pair<String,String>( "KeyPairGenerator", "DH" )
    ));

    public static void main( String[] args ) throws Exception {
        runTest();

        Provider provider = (Provider)RsaReproKeyAgreementPerformance.class.getClassLoader().loadClass(CLASSNAME_PROVIDER).newInstance();
        configureProvider(provider);
        Security.insertProviderAt(provider, 1);

        runTest();
    }

    private static void runTest() throws Exception {
        String providerName = KeyAgreement.getInstance("DH").getProvider().getName();
        new BenchmarkRunner(new Runnable() {
            @Override
            public void run() {
                try {
                    doKeyAgreement();

                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }, 100, 10, "KeyAgreement.DH with provider=" + providerName).run();
    }

    private static void configureProvider( final Provider provider ) {
        try {
            final Method method = Provider.class.getDeclaredMethod( "removeService", Provider.Service.class );
            method.setAccessible( true );

            for ( Pair<String,String> serviceDesc : SERVICE_BLACKLIST ) {
                final String type = serviceDesc.left;
                final String algorithm = serviceDesc.right;
                final Provider.Service service = provider.getService( type, algorithm );
                if ( SyspropUtil.getBoolean( "blacklistServices" ) && service != null ) { // may be null in some modes
                    logger.info( "Removing service '"+type+"."+algorithm+"'." );
                    method.invoke( provider, service );
                }
            }
        } catch (InvocationTargetException e) {
            logger.log( Level.WARNING, "Error configuring services '"+ ExceptionUtils.getMessage(e) +"'.", ExceptionUtils.getDebugException(e) );
        } catch (NoSuchMethodException e) {
            logger.log( Level.WARNING, "Error configuring services '"+ ExceptionUtils.getMessage(e) +"'.", ExceptionUtils.getDebugException(e) );
        } catch (IllegalAccessException e) {
            logger.log( Level.WARNING, "Error configuring services '"+ ExceptionUtils.getMessage(e) +"'.", ExceptionUtils.getDebugException(e) );
        }
    }

    private static void doKeyAgreement() throws NoSuchAlgorithmException, InvalidKeyException, InvalidAlgorithmParameterException {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("DH");

        // On server
        kpg.initialize(768);
        KeyPair kp1 = kpg.generateKeyPair();
        KeyAgreement ka1 = KeyAgreement.getInstance("DH");
        ka1.init(kp1.getPrivate());

        // On client
        kpg.initialize(((DHPublicKey) kp1.getPublic()).getParams());
        KeyPair kp2 = kpg.generateKeyPair();
        KeyAgreement ka2 = KeyAgreement.getInstance("DH");
        ka2.init(kp2.getPrivate());

        // On server
        ka1.doPhase(kp2.getPublic(), true);
        byte[] serverkeybytes = ka1.generateSecret();

        // On client
        ka2.doPhase(kp1.getPublic(), true);
        byte[] clientkeybytes = ka2.generateSecret();

        if (!Arrays.equals(serverkeybytes, clientkeybytes))
            throw new RuntimeException("Both sides failed to generate the same secret key");
    }
}
