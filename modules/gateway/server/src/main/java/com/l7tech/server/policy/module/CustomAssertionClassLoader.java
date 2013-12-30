package com.l7tech.server.policy.module;

import java.net.URL;
import java.net.URLClassLoader;

/**
 * Represents a class loader for certain custom assertion jar.<br/>
 * For now it only wraps around {@link URLClassLoader}, which is the actual custom assertions class loader.<br/>
 * Currently the class is used to identify which <code>ClassLoader</code> belongs to the custom assertions,
 * from the {@link AllCustomAssertionClassLoader} delegate class loaders list.
 * <p/>
 * At some point we can add logic here
 */
public class CustomAssertionClassLoader extends URLClassLoader {
    public CustomAssertionClassLoader(URL[] urls) {
        super(urls);
    }

    public CustomAssertionClassLoader(URL[] urls, ClassLoader parent) {
        super(urls, parent);
    }
}
