package com.l7tech.console.util;

import java.rmi.server.RMIClassLoaderSpi;
import java.net.MalformedURLException;
import java.util.concurrent.atomic.AtomicReference;

import com.l7tech.policy.wsp.ClassLoaderUtil;

/**
 * RMIClassLoaderSpi that is backed by a CustomAssertionClassLoader.
 *
 * <p>This class also initializes the SSM classloaders.</p>
 *
 * @author Steve Jones, $Author$
 * @version $Revision$
 * @see ClassLoaderUtil#setClassloader
 */
public class CustomAssertionRMIClassLoaderSpi extends RMIClassLoaderSpi {

    //- PUBLIC

    public CustomAssertionRMIClassLoaderSpi() {
        lastInstance = this;
        resetRemoteClassLoader();
    }

    public String getClassAnnotation(Class<?> cl) {
        return null;
    }

    public ClassLoader getClassLoader(String codebase) throws MalformedURLException {
        return null;
    }

    public Class<?> loadClass(String codebase, String name, ClassLoader defaultLoader) throws MalformedURLException, ClassNotFoundException {
        return classLoader.get().loadClass(name);
    }

    public Class<?> loadProxyClass(String codebase, String[] interfaces, ClassLoader defaultLoader) throws MalformedURLException, ClassNotFoundException {
        throw new ClassNotFoundException("loading of proxies not supported.");
    }

    public static void resetRemoteClassLoader() {
        CustomAssertionRMIClassLoaderSpi that = lastInstance;
        if (that != null) {
            that.classLoader.set(new CustomAssertionClassLoader());
            ClassLoaderUtil.setClassloader(that.classLoader.get());
        }
    }

    //- PRIVATE

    private static CustomAssertionRMIClassLoaderSpi lastInstance = null;
    private final AtomicReference<ClassLoader> classLoader = new AtomicReference<ClassLoader>();
}
