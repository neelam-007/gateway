package com.l7tech.server.policy.module;

import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.AssertionMetadata;
import com.l7tech.util.ClassUtils;
import com.l7tech.util.ExceptionUtils;

import java.io.Closeable;
import java.io.IOException;
import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.jar.JarFile;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Represents a module jarfile that contains at least one assertion, loaded from /ssg/modules/assertions.
 * <p/>
 * This takes ownership of the jar file and keeps it open -- call {@link #close()} to close it.
 */
public class ModularAssertionModule extends BaseAssertionModule<ModularAssertionClassLoader> implements Closeable {
    private static final Logger logger = Logger.getLogger(ModularAssertionModule.class.getName());

    private final JarFile jarfile;
    private final Set<? extends Assertion> assertionPrototypes;
    private final Set<String> packages;

    ModularAssertionModule(String moduleName, JarFile jarfile, long modifiedTime, String moduleDigest, ModularAssertionClassLoader classLoader, Set<? extends Assertion> assertionPrototypes, Set<String> packages) {
        super(moduleName, modifiedTime, moduleDigest, classLoader);

        if (jarfile == null) throw new IllegalArgumentException("jarfile required");
        if (assertionPrototypes == null || assertionPrototypes.isEmpty()) throw new IllegalArgumentException("assertionPrototypes must contain at least one prototype instance");
        if (packages == null || packages.isEmpty()) throw new IllegalArgumentException("packages must be specified and contain at least one package name");

        this.jarfile = jarfile;
        this.assertionPrototypes = Collections.unmodifiableSet(assertionPrototypes);
        this.packages = Collections.unmodifiableSet(packages);
    }

    /** @return prototype instances of each assertion added by this module.  Never null, and always contains at least one assertion. */
    public Set<? extends Assertion> getAssertionPrototypes() {
        return assertionPrototypes;
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
     * @param hidePrivateLibraries  true if the resource bytes will be sent back to a remote client, and so any classes from nested jarfiles
     *                              matching patterns listed in the .AAR manifest's "Private-Libraries:" header should not be loadable.
     *                              <p/>
     *                              false if all resource bytes should be loadable, even those from private nested jarfiles.
     * @return the bytes of the specified resource, or null if no matching resource was found.
     * @throws IOException if there was an error reading the resource
     */
    public byte[] getResourceBytes(String resourcepath, boolean hidePrivateLibraries) throws IOException {
        return getModuleClassLoader().getResourceBytes(resourcepath, hidePrivateLibraries);
    }

    /**
     * Get the list of resources that are likely necessary for the console.
     *
     * @return The list of resource paths.
     */
    public String[] getClientResources() {
        Set<String> resources = new LinkedHashSet<>();

        for ( Assertion assertion : getAssertionPrototypes() ) {
            AssertionMetadata meta = assertion.meta();
            try {
                Collection<URL> urls = ClassUtils.listResources( meta.getAssertionClass(), "/AAR-INF/assertion.index");
                for ( URL url : urls ) {
                    String path = url.getFile();
                    int index = path.lastIndexOf( "AAR-INF/" );
                    if ( index > 0 ) {
                        path = path.substring( index + 8 );
                    }
                    resources.add( path );
                }
            } catch (IOException e) {
                logger.log(Level.WARNING, "Unable to read assertion index for module " + getName() + ": " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
            }

            try {
                Collection<URL> urls = ClassUtils.listResources( meta.getAssertionClass(), "/AAR-INF/console.index");
                for ( URL url : urls ) {
                    String path = url.getFile();
                    int index = path.lastIndexOf( "AAR-INF/" );
                    if ( index > 0 ) {
                        path = path.substring( index + 8 );
                    }
                    resources.add( path );
                }
            } catch (IOException e) {
                logger.log(Level.WARNING, "Unable to read console index for module " + getName() + ": " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
            }
        }

        return resources.toArray( new String[resources.size()] );
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

    /**
     * Check if this module added the specified classname.
     *
     * @param assertionClassname  the class name to check
     * @return true if this module added the specified assertion concrete classname.
     */
    public boolean offersClass(final String assertionClassname) {
        for (final Assertion prototype : assertionPrototypes)
            if (prototype.getClass().getName().equals(assertionClassname))
                return true;
        return false;
    }

    /**
     * Release any resources used by this module and close the jarfile.
     *
     * @throws IOException if there is an error closing the jarfile or the class loader
     */
    @Override
    public void close() throws IOException {
        try {
            getModuleClassLoader().close();
        } finally {
            jarfile.close();
        }
    }
}
