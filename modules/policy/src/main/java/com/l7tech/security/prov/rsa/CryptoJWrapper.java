package com.l7tech.security.prov.rsa;

import com.l7tech.security.prov.ProviderUtil;
import com.l7tech.util.ConfigFactory;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.Functions.Unary;
import com.l7tech.util.Functions.UnaryVoid;
import com.l7tech.util.Option;
import com.l7tech.util.Pair;

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
    public static final String PROP_CRYPTOJCE_PATH = "com.l7tech.security.prov.rsa.libpath.cryptojce";
    public static final String PROP_CRYPTOJCOMMON_PATH = "com.l7tech.security.prov.rsa.libpath.cryptojcommon";
    public static final String PROP_DISABLE_BLACKLISTED_SERVICES = "com.l7tech.security.prov.rsa.disableServices";

    static final String FIPS_LIB_PATH = ConfigFactory.getProperty( PROP_FIPS_LIB_PATH, null );
    static final String NON_FIPS_LIB_PATH = ConfigFactory.getProperty( PROP_NON_FIPS_LIB_PATH, null );
    static final String SSLJ_LIB_PATH = ConfigFactory.getProperty( PROP_SSLJ_PATH, replaceFilename( NON_FIPS_LIB_PATH, "sslj-6.1.2.jar" ) );
    static final String CERTJ_LIB_PATH = ConfigFactory.getProperty( PROP_CERTJ_PATH, replaceFilename( NON_FIPS_LIB_PATH, "certj-6.1.1.jar" ) );
    static final String CRYPTOJCE_LIB_PATH = ConfigFactory.getProperty( PROP_CRYPTOJCE_PATH, replaceFilename( NON_FIPS_LIB_PATH, "cryptojce-6.1.2.jar" ) );
    static final String CRYPTOJCOMMON_LIB_PATH = ConfigFactory.getProperty( PROP_CRYPTOJCOMMON_PATH, replaceFilename( NON_FIPS_LIB_PATH, "cryptojcommon-6.1.2.jar" ) );
    static final boolean DISABLE_BLACKLISTED_SERVICES = ConfigFactory.getBooleanProperty( PROP_DISABLE_BLACKLISTED_SERVICES, true );

    static final String CLASSNAME_CRYPTOJ = "com.rsa.jsafe.crypto.CryptoJ";
    static final String CLASSNAME_PROVIDER = "com.rsa.jsafe.provider.JsafeJCE";
    static final String CLASSNAME_DEBUG = "com.rsa.jsse.engine.util.Debug";
    static final String CLASSNAME_GCM_PARAMETER_SPEC = "com.rsa.jsafe.provider.GCMParameterSpec";

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
    final Option<Method> debugReset;
    final Option<Constructor> gcmParameterSpecCtor;

    final int FIPS140_SSL_ECC_MODE;
    final int NON_FIPS140_MODE;

    CryptoJWrapper(boolean fips) throws ClassNotFoundException, IllegalAccessException, InstantiationException, NoSuchFieldException, MalformedURLException {
        String libPath = fips ? FIPS_LIB_PATH : NON_FIPS_LIB_PATH;
        this.cl = libPath == null || "USECLASSPATH".equals(libPath)
                ? Thread.currentThread().getContextClassLoader()
                : makeJarClassLoader(libPath);
        this.provider = (Provider)cl.loadClass(CLASSNAME_PROVIDER).newInstance();
        if ( DISABLE_BLACKLISTED_SERVICES ) {
            ProviderUtil.configureProvider(SERVICE_BLACKLIST, provider);
        }
        this.cryptoj = cl.loadClass(CLASSNAME_CRYPTOJ);
        this.debugReset = findDebugResetMethod();
        this.FIPS140_SSL_ECC_MODE = cryptoj.getField("FIPS140_SSL_ECC_MODE").getInt(null);
        this.NON_FIPS140_MODE = cryptoj.getField("NON_FIPS140_MODE").getInt(null);
        this.gcmParameterSpecCtor = findGcmParameterSpecCtor();
    }

    // Replace the filename at the end of a path with a new filename, ie changing "/foo/bar/jsafe.jar" into "/foo/bar/sslj.jar".
    private static String replaceFilename(String path, String filename) {
        if (path == null) return null;
        File parentFile = new File(path).getParentFile();
        return parentFile == null ? filename : new File(parentFile, filename).getPath();
    }

    private URLClassLoader makeJarClassLoader(String libPath) throws MalformedURLException {
        final File file = new File(libPath);
        if (!file.exists() || !file.canRead())
            throw new IllegalArgumentException("Crypto library path not found: " + file.getAbsolutePath());
        List<URL> jarUrls = new ArrayList<URL>();
        jarUrls.add(file.toURI().toURL());
        addRequiredLib( jarUrls, CRYPTOJCOMMON_LIB_PATH );
        addRequiredLib( jarUrls, CRYPTOJCE_LIB_PATH );
        addRequiredLib( jarUrls, SSLJ_LIB_PATH );
        addRequiredLib( jarUrls, CERTJ_LIB_PATH );
        return new URLClassLoader(jarUrls.toArray(new URL[jarUrls.size()]));
    }

    private void addRequiredLib( final List<URL> jarUrls, final String libPath ) throws MalformedURLException {
        if ( libPath != null ) {
            final File libFile = new File(libPath);
            if ( !libFile.exists() ) {
                logger.severe( "Missing required library for RSA: '" + libPath + "'");
            }
            jarUrls.add(libFile.toURI().toURL());
        }
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

    void resetDebug() {
        debugReset.foreach( new UnaryVoid<Method>() {
            @Override
            public void call( final Method debugReset ) {
                try {
                    debugReset.invoke( null );
                } catch ( InvocationTargetException e ) {
                    logger.log( Level.WARNING, "Error resetting SSL/TLS debug configuration", e.getCause() );
                } catch ( Exception e ) {
                    logger.log( Level.WARNING, "Error resetting SSL/TLS debug configuration", e );
                }
            }
        } );
    }

    AlgorithmParameterSpec createGcmParameterSpec(int authTagLenBytes, long authenticatedDataLenBytes, byte[] iv) throws NoSuchAlgorithmException {
        if (!gcmParameterSpecCtor.isSome())
            throw new NoSuchAlgorithmException("Galous/Counter Mode is not available");

        try {
            return (AlgorithmParameterSpec) gcmParameterSpecCtor.some().newInstance(authTagLenBytes, authenticatedDataLenBytes, iv);
        } catch (InstantiationException e) {
            throw new NoSuchAlgorithmException("Galous/Counter Mode is not available: " + ExceptionUtils.getMessage(e), e);
        } catch (IllegalAccessException e) {
            throw new NoSuchAlgorithmException("Galous/Counter Mode is not available: " + ExceptionUtils.getMessage(e), e);
        } catch (InvocationTargetException e) {
            throw new NoSuchAlgorithmException("Galous/Counter Mode is not available: " + ExceptionUtils.getMessage(e), e);
        }
    }

    private Option<Method> findDebugResetMethod() {
        Option<Class<?>> debug = none();
        try {
            debug = Option.<Class<?>>some( cl.loadClass( CLASSNAME_DEBUG ) );
        } catch ( ClassNotFoundException e ) {
            logger.log(
                    Level.WARNING,
                    "Unable to load class for debug output control.",
                    ExceptionUtils.getDebugException( e ) );
        }
        return debug.map( new Unary<Method,Class<?>>(){
            @Override
            public Method call( final Class<?> debug ) {
                try {
                    return debug.getMethod( "debugInitialize" );
                } catch ( Exception e ) {
                    logger.log(
                            Level.WARNING,
                            "Unable to access method for debug output control: " + ExceptionUtils.getMessage( e ),
                            ExceptionUtils.getDebugException( e ) );
                }
                return null;
            }
        } );
    }

    private Option<Constructor> findGcmParameterSpecCtor() {
        Option<Class<?>> spec = none();
        try {
            spec = Option.<Class<?>>some(cl.loadClass(CLASSNAME_GCM_PARAMETER_SPEC));
        } catch (ClassNotFoundException e) {
            logger.log(
                    Level.WARNING,
                    "Unable to load class for Crypto-J Galois/Counter Mode parameters.  AES-GCM will be unavailable.",
                    ExceptionUtils.getDebugException( e ) );
        }
        return spec.map(new Unary<Constructor, Class<?>>() {
            @Override
            public Constructor call(Class<?> spec) {
                if (!AlgorithmParameterSpec.class.isAssignableFrom(spec)) {
                    logger.log(
                            Level.WARNING,
                            "Unable to load class for Crypto-J Galois/Counter Mode parameters.  AES-GCM will be unavailable: " + CLASSNAME_GCM_PARAMETER_SPEC + " is not assignable to AlgorithmParameterSpec");
                    return null;
                }
                try {
                    return spec.getConstructor(int.class, long.class, byte[].class);
                } catch (Exception e) {
                    logger.log(
                            Level.WARNING,
                            "Unable to find constructor for Crypto-J Galois/Counter Mode parameters.  AES-GCM will be unavailable: " + ExceptionUtils.getMessage(e),
                            ExceptionUtils.getDebugException(e));
                }
                return null;
            }
        });
    }
}
