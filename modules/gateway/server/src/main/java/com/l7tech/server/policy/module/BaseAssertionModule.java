package com.l7tech.server.policy.module;

import java.net.URLClassLoader;

/**
 * Base holder for assertion jar-files, both modular and custom.
 * <br/>
 * All shared entities between modular and custom assertion modules goes here.
 */
public class BaseAssertionModule<T extends URLClassLoader> {

    protected final String name;
    protected final long modifiedTime;
    protected final String digest;
    protected final T classLoader;

    /**
     * Create assertion module.
     *
     * @param moduleName      the module filename.
     * @param modifiedTime    the module last modified timestamp.
     * @param moduleDigest    the module content checksum (currently SHA256).
     * @param classLoader     the module class loader.
     */
    public BaseAssertionModule(final String moduleName,
                               final long modifiedTime,
                               final String moduleDigest,
                               final T classLoader)
    {
        if (moduleName == null || moduleName.length() < 1) {
            throw new IllegalArgumentException("non-empty moduleName required");
        }
        if (classLoader == null) {
            throw new IllegalArgumentException("classLoader required");
        }

        this.name = moduleName;
        this.modifiedTime = modifiedTime;
        this.digest = moduleDigest;
        this.classLoader = classLoader;
    }

    /**
     * @return the name of this assertion module, ie "RateLimitAssertion-3.7.0.jar".
     */
    public String getName() {
        return name;
    }

    /**
     * @return the modification time of the file when this module was read.
     */
    public long getModifiedTime() {
        return modifiedTime;
    }

    /**
     * @return the checksum of this assertion module file (currently SHA256).
     */
    public String getDigest() {
        return digest;
    }

    /**
     * @return the ClassLoader providing classes for this jar file.
     */
    public T getModuleClassLoader() {
        return classLoader;
    }
}
