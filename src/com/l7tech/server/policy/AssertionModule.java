package com.l7tech.server.policy;

import com.l7tech.policy.assertion.Assertion;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Set;

/**
 * Represents a module jarfile that contains at least one assertion, loaded from /ssg/modules/assertions.
 */
public class AssertionModule {
    private final File jarfile;
    private final long jarfileModifiedTime;
    private final String jarfileSha1;
    private final AssertionModuleClassLoader classLoader;
    private final Set<? extends Assertion> assertionPrototypes;
    private final Set<String> packages;

    AssertionModule(File jarfile, long modifiedTime, String jarfileSha1, AssertionModuleClassLoader classLoader, Set<? extends Assertion> assertionPrototypes, Set<String> packages) {
        if (assertionPrototypes == null || assertionPrototypes.isEmpty()) throw new IllegalArgumentException("assertionPrototypes must contain at least one prototype instance");
        if (classLoader == null) throw new IllegalArgumentException("classLoader required");
        if (packages == null || packages.isEmpty()) throw new IllegalArgumentException("packages must be specified and contain at least one package name");
        this.jarfile = jarfile;
        this.jarfileModifiedTime = modifiedTime;
        this.jarfileSha1 = jarfileSha1;
        this.classLoader = classLoader;
        this.assertionPrototypes = Collections.unmodifiableSet(assertionPrototypes);
        this.packages = Collections.unmodifiableSet(packages);
    }

    /** @return the name of this assertion module, ie "RateLimitAssertion-3.7.0.jar". */
    public String getName() {
        return jarfile.getName();
    }

    /** @return the SHA-1 of this assertion module file, ie "deadbeefcafebabeface4d7721111be8b56c4d77". */
    public String getSha1() {
        return jarfileSha1;
    }

    /** @return prototype instances of each assertion added by this module.  Never null, and always contains at least one assertion. */
    public Set<? extends Assertion> getAssertionPrototypes() {
        return assertionPrototypes;
    }

    /**
     * @return an InputStream that produces the entire module jarfile bytes. Never null.  Caller is responsible for closing this.
     * @throws java.io.IOException if the jarfile can't be read.
     */
    public InputStream getJarfile() throws IOException {
        return new FileInputStream(jarfile);
    }

    /**
     * Get the bytes for the specified resource from this assertion module, without looking in any parent
     * classloaders.
     * <p/>
     * Keep in mind you can convert a class name into a resource path easily:
     * <pre>
     * String resourcepath = classname.replace('.', '/').concat(".class");
     * </pre>
     *
     * @param resourcepath  the path to look for, ie "com/l7tech/console/panels/resources/RateLimitAssertionPropertiesDialog.form".  Required.
     * @return the bytes of the specified resource, or null if no matching resource was found.
     * @throws IOException if there was an error reading the resource
     */
    public byte[] getResourceBytes(String resourcepath) throws IOException {
        return classLoader.getResourceBytes(resourcepath);
    }

    /**
     * Check if this module includes any classes in the specified package.
     *
     * @param packageName the package to check
     * @return true if at least one class or resource in this exact package (not any subpackages) is present
     *              in this module.
     */
    public boolean offersPackage(String packageName) {
        return packages.contains(packageName);
    }

    /** @return the modification time of the file when this module was read. */
    long getJarfileModifiedTime() {
        return jarfileModifiedTime;
    }

    /**
     * Check if this module added the specified classname.
     *
     * @param assertionClassname  the class name to check
     * @return true if this module added the specified assertion concrete classname.
     */
    boolean offersClass(String assertionClassname) {
        for (Assertion prototype : assertionPrototypes)
            if (prototype.getClass().getName().equals(assertionClassname))
                return true;
        return false;
    }

    /**
     * @return the ClassLoader providing classes for this jarfile
     */
    AssertionModuleClassLoader getModuleClassLoader() {
        return classLoader;
    }

    /**
     * Notify interested classes that this module is being unloaded.
     */
    void onModuleUnloaded() {
        classLoader.onModuleUnloaded();
    }
}
