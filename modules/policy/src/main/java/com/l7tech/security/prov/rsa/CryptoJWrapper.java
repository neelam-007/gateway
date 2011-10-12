package com.l7tech.security.prov.rsa;

import com.l7tech.util.ConfigFactory;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.Pair;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.Provider;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

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
    public static final String PROP_CERTJ_PATH = "com.l7tech.security.prov.rsa.libpath.certj";
    public static final String PROP_SSLJ_PATH = "com.l7tech.security.prov.rsa.libpath.sslj";
    public static final String PROP_DISABLE_BLACKLISTED_SERVICES = "com.l7tech.security.prov.rsa.disableServices";

    static final String FIPS_LIB_PATH = ConfigFactory.getProperty( PROP_FIPS_LIB_PATH, null );
    static final String NON_FIPS_LIB_PATH = ConfigFactory.getProperty( PROP_NON_FIPS_LIB_PATH, null );
    static final String SSLJ_LIB_PATH = ConfigFactory.getProperty( PROP_SSLJ_PATH, replaceFilename( NON_FIPS_LIB_PATH, "sslj-5.1.1.2.jar" ) );
    static final String CERTJ_LIB_PATH = ConfigFactory.getProperty( PROP_CERTJ_PATH, replaceFilename( NON_FIPS_LIB_PATH, "certj-3.1.jar" ) );
    static final boolean DISABLE_BLACKLISTED_SERVICES = ConfigFactory.getBooleanProperty( PROP_DISABLE_BLACKLISTED_SERVICES, true );

    static final String CLASSNAME_CRYPTOJ = "com.rsa.jsafe.crypto.CryptoJ";
    static final String CLASSNAME_PROVIDER = "com.rsa.jsafe.provider.JsafeJCE";

    @SuppressWarnings({"unchecked"})
    private static final Collection<Pair<String,String>> SERVICE_BLACKLIST = Collections.unmodifiableCollection(Arrays.asList(
            new Pair<String,String>( "CertificateFactory", "X.509" ),
            new Pair<String,String>( "KeyStore", "PKCS12" ),
            new Pair<String,String>( "CertPathBuilder", "PKIX" ),
            new Pair<String,String>( "CertPathValidator", "PKIX" ),
            new Pair<String,String>( "CertStore", "Collection" )
    ));

    final ClassLoader cl;
    final Provider provider;
    final Class cryptoj;

    final int FIPS140_SSL_ECC_MODE;
    final int NON_FIPS140_MODE;

    CryptoJWrapper(boolean fips) throws ClassNotFoundException, IllegalAccessException, InstantiationException, NoSuchFieldException, MalformedURLException {
        String libPath = fips ? FIPS_LIB_PATH : NON_FIPS_LIB_PATH;
        cl = libPath == null || "USECLASSPATH".equals(libPath)
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

    // Replace the filename at the end of a path with a new filename, ie changing "/foo/bar/jsafe.jar" into "/foo/bar/sslj.jar".
    private static String replaceFilename(String path, String filename) {
        if (path == null) return null;
        File parentFile = new File(path).getParentFile();
        return parentFile == null ? filename : new File(parentFile, filename).getPath();
    }

    URLClassLoader makeJarClassLoader(String libPath) throws MalformedURLException {
        final File file = new File(libPath);
        if (!file.exists() || !file.canRead())
            throw new IllegalArgumentException("Crypto library path not found: " + file.getAbsolutePath());
        List<URL> jarUrls = new ArrayList<URL>();
        jarUrls.add(file.toURI().toURL());
        if (SSLJ_LIB_PATH != null) jarUrls.add(new File(SSLJ_LIB_PATH).toURI().toURL());
        if (CERTJ_LIB_PATH != null) jarUrls.add(new File(CERTJ_LIB_PATH).toURI().toURL());
        return new URLClassLoader(jarUrls.toArray(new URL[jarUrls.size()]));
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

    Object getVersion() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException, NoSuchFieldException {
        return cryptoj.getField("CRYPTO_J_VERSION").get(null);
    }

    Provider getPkcs12Provider() throws  IllegalAccessException, InstantiationException {
        try {
            return (Provider) cryptoj.getClassLoader().loadClass("com.rsa.jcp.RSAJCP").newInstance();
        } catch (ClassNotFoundException e) {
            logger.log(Level.FINE, "Unable to find RSAJCP for PKCS#12; attempting to do without: " + ExceptionUtils.getMessage(e), e);
            return null;
        }
    }

    Provider getJsseProvider() throws IllegalAccessException, InstantiationException {
        try {
            return (Provider) cryptoj.getClassLoader().loadClass("com.rsa.jsse.JsseProvider").newInstance();
        } catch (ClassNotFoundException e) {
            logger.log(Level.FINE, "Unable to find SSL-J for TLS 1.2; attempting to do without: " + ExceptionUtils.getMessage(e), e);
            return null;
        }
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
