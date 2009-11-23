package com.l7tech.security.prov.rsa;

import com.l7tech.util.SyspropUtil;
import com.l7tech.util.Pair;
import com.l7tech.util.ExceptionUtils;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URLClassLoader;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.Provider;
import java.util.Collection;
import java.util.Collections;
import java.util.Arrays;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * Wrapper for Crypto-J classes that uses reflection to load them to avoid compile-time dependencies on a particular
 * RSA BSAFE Crypto-J version (since we are shipping two versions in conflicting jars: FIPS and non-FIPS).
 * <p/>
 * If the system property {@link #PROP_FIPS_LIB_PATH} is set and FIPS mode is requested (or if {@link #PROP_NON_FIPS_LIB_PATH} is set
 * and FIPS mode is not requested) we will expect to find Crypto-J in the specified
 * jarfile; otherwise, we expect to find it in the context classloader.
 */
class CryptoJWrapper {
    private static final Logger logger = Logger.getLogger(CryptoJWrapper.class.getName());

    public static final String PROP_FIPS_LIB_PATH = "com.l7tech.security.prov.rsa.libpath.fips";
    public static final String PROP_NON_FIPS_LIB_PATH = "com.l7tech.security.prov.rsa.libpath.nonfips";
    public static final String PROP_DISABLE_BLACKLISTED_SERVICES = "com.l7tech.security.prov.rsa.disableServices";

    static final String FIPS_LIB_PATH = SyspropUtil.getString(PROP_FIPS_LIB_PATH, null);
    static final String NON_FIPS_LIB_PATH = SyspropUtil.getString(PROP_NON_FIPS_LIB_PATH, null);
    static final boolean DISABLE_BLACKLISTED_SERVICES = SyspropUtil.getBoolean(PROP_DISABLE_BLACKLISTED_SERVICES, true);

    static final String CLASSNAME_CRYPTOJ = "com.rsa.jsafe.crypto.CryptoJ";
    static final String CLASSNAME_PROVIDER = "com.rsa.jsafe.provider.JsafeJCE";

    private static final Collection<Pair<String,String>> SERVICE_BLACKLIST = Collections.unmodifiableCollection(Arrays.asList(
            new Pair<String,String>( "CertificateFactory", "X.509" ),
            new Pair<String,String>( "KeyStore", "PKCS12" )
    ));

    final ClassLoader cl;
    final Provider provider;
    final Class cryptoj;

    final int FIPS140_SSL_ECC_MODE;
    final int NON_FIPS140_MODE;

    CryptoJWrapper(boolean fips) throws ClassNotFoundException, IllegalAccessException, InstantiationException, NoSuchFieldException, MalformedURLException {
        String libPath = fips ? FIPS_LIB_PATH : NON_FIPS_LIB_PATH;
        cl = libPath == null
                ? Thread.currentThread().getContextClassLoader()
                : makeJarClassLoader(libPath);
        provider = (Provider)cl.loadClass(CLASSNAME_PROVIDER).newInstance();
        if ( DISABLE_BLACKLISTED_SERVICES ) {
            configureProvider(provider);
        }
        cryptoj = cl.loadClass(CLASSNAME_CRYPTOJ);
        FIPS140_SSL_ECC_MODE = cryptoj.getField("FIPS140_SSL_ECC_MODE").getInt(null);
        NON_FIPS140_MODE = cryptoj.getField("NON_FIPS140_MODE").getInt(null);
    }

    URLClassLoader makeJarClassLoader(String libPath) throws MalformedURLException {
        final File file = new File(libPath);
        if (!file.exists() || !file.canRead())
            throw new IllegalArgumentException("Crypto library path not found: " + file.getAbsolutePath());
        return new URLClassLoader(new URL[]{file.toURI().toURL()});
    }

    boolean isFIPS140Compliant() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        return (Boolean)cryptoj.getMethod("isFIPS140Compliant").invoke(null);
    }

    boolean isInFIPS140Mode() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        return (Boolean)cryptoj.getMethod("isInFIPS140Mode").invoke(null);
    }

    void setMode(int mode) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        cryptoj.getMethod("setMode", int.class).invoke(null, mode);
    }

    private void configureProvider( final Provider provider ) {
        try {
            final Method method = Provider.class.getDeclaredMethod( "removeService", Provider.Service.class );
            method.setAccessible( true );

            for ( Pair<String,String> serviceDesc : SERVICE_BLACKLIST ) {
                final String type = serviceDesc.left;
                final String algorithm = serviceDesc.right;
                final Provider.Service service = provider.getService( type, algorithm );
                if ( service != null ) { // may be null in some modes
                    logger.fine( "Removing service '"+type+"."+algorithm+"'." );
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
}
