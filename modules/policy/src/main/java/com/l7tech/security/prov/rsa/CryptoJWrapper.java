package com.l7tech.security.prov.rsa;

import com.l7tech.util.SyspropUtil;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.net.URLClassLoader;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.Provider;

/**
 * Wrapper for Crypto-J classes that uses reflection to load them to avoid compile-time dependencies on a particular
 * RSA BSAFE Crypto-J version (since we are shipping two versions in conflicting jars: FIPS and non-FIPS).
 * <p/>
 * If the system property {@link #PROP_FIPS_LIB_PATH} is set and FIPS mode is requested (or if {@link #PROP_NON_FIPS_LIB_PATH} is set
 * and FIPS mode is not requested) we will expect to find Crypto-J in the specified
 * jarfile; otherwise, we expect to find it in the context classloader.
 */
class CryptoJWrapper {
    public static final String PROP_FIPS_LIB_PATH = "com.l7tech.security.prov.rsa.libpath.fips";
    public static final String PROP_NON_FIPS_LIB_PATH = "com.l7tech.security.prov.rsa.libpath.nonfips";

    static final String FIPS_LIB_PATH = SyspropUtil.getString(PROP_FIPS_LIB_PATH, null);
    static final String NON_FIPS_LIB_PATH = SyspropUtil.getString(PROP_NON_FIPS_LIB_PATH, null);

    static final String CLASSNAME_CRYPTOJ = "com.rsa.jsafe.crypto.CryptoJ";
    static final String CLASSNAME_PROVIDER = "com.rsa.jsafe.provider.JsafeJCE";

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
}
