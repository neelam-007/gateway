package com.l7tech.security.prov.rsa;

import java.security.Provider;
import java.lang.reflect.InvocationTargetException;

/**
 * Wrapper for Crypto-J classes that uses reflection to load them to avoid compile-time dependencies on a particular
 * RSA BSAFE Crypto-J version (since we are shipping two versions in conflicting jars: FIPS and non-FIPS).
 */
class CryptoJWrapper {
    static final String CLASSNAME_CRYPTOJ = "com.rsa.jsafe.crypto.CryptoJ";
    static final String CLASSNAME_PROVIDER = "com.rsa.jsafe.provider.JsafeJCE";

    final ClassLoader cl;
    final Provider provider;
    final Class cryptoj;

    final int FIPS140_SSL_ECC_MODE;
    final int NON_FIPS140_MODE;

    CryptoJWrapper(boolean fips) throws ClassNotFoundException, IllegalAccessException, InstantiationException, NoSuchFieldException {
        cl = getClass().getClassLoader();// TODO dynamically pick jarfile
        provider = (Provider)cl.loadClass(CLASSNAME_PROVIDER).newInstance();
        cryptoj = cl.loadClass(CLASSNAME_CRYPTOJ);
        FIPS140_SSL_ECC_MODE = cryptoj.getField("FIPS140_SSL_ECC_MODE").getInt(null);
        NON_FIPS140_MODE = cryptoj.getField("NON_FIPS140_MODE").getInt(null);
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
