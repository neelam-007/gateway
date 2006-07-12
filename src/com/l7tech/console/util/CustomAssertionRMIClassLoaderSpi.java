package com.l7tech.console.util;

import java.rmi.server.RMIClassLoaderSpi;
import java.net.MalformedURLException;

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
        classLoader = new CustomAssertionClassLoader();
        ClassLoaderUtil.setClassloader(classLoader);
    }

    public String getClassAnnotation(Class<?> cl) {
        return null;
    }

    public ClassLoader getClassLoader(String codebase) throws MalformedURLException {
        return null;
    }

    public Class<?> loadClass(String codebase, String name, ClassLoader defaultLoader) throws MalformedURLException, ClassNotFoundException {
        return classLoader.loadClass(name);
    }

    public Class<?> loadProxyClass(String codebase, String[] interfaces, ClassLoader defaultLoader) throws MalformedURLException, ClassNotFoundException {
        throw new ClassNotFoundException("loading of proxies not supported.");
    }

    //- PRIVATE

    private final ClassLoader classLoader;
}
