package com.l7tech.server.policy.module;

import com.sun.istack.Nullable;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.URL;
import java.util.*;

/**
 * A ClassLoader for all custom assertion modules loaded in the system.
 * <p/>
 * This is a simple wrapper for locating all custom assertion class names and resources.
 *
 * @author Steve Jones, $Author$
 * @version $Revision$
 */
public class AllCustomAssertionClassLoader extends ClassLoader {

    //- PUBLIC

    /**
     * Create a module class loader for all scanned assertions.
     * 
     * @param parent         the parent <code>ClassLoaders</code> for all modules.
     * @param modules        a concurrent read-only view of all scanned modules.
     * @param finalLoader    this <code>ClassLoader</code> will be treated as the <i>"final"</i> module in the chain (may be null).
     */
    public AllCustomAssertionClassLoader(@NotNull final ClassLoader parent,
                                         @NotNull final Collection<CustomAssertionModule> modules,
                                         @Nullable final ClassLoader finalLoader) {
        super(parent);
        this.modules = modules;
        this.finalLoader = finalLoader;
    }

    //- PROTECTED

    /**
     * Find the class in one of the modules.
     *
     * @param name The class name
     * @return The class
     * @throws ClassNotFoundException if not in any module
     */
    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        Class<?> clazz = null;

        // try first from scanned modules
        for (final CustomAssertionModule module : modules) {
            clazz = doFindClass(module.getModuleClassLoader(), name); // getModuleClassLoader cannot be null
            if (clazz != null) break;
        }

        // if nothing found try the final ClassLoader
        if (clazz == null && finalLoader != null) {
            clazz = doFindClass(finalLoader, name);
        }

        if (clazz == null) throw new ClassNotFoundException(name);

        return clazz;
    }

    /**
     * A helper function for locating the specified class name in the specified <code>ClassLoader</code>.
     *
     * @param delegateClassLoader    the <code>ClassLoader</code> from where to find the specified class name.
     * @param name                   the class name to find.
     * @return a if the class name if found in the ClassLoader then <code>Class</code> object, <code>null</code> otherwise
     */
    private Class<?> doFindClass(@NotNull final ClassLoader delegateClassLoader, final String name) {
        final ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            if (contextClassLoader != null) Thread.currentThread().setContextClassLoader(null);
            return delegateClassLoader.loadClass(name);
        }
        catch(ClassNotFoundException ignore) {
            // ignore
        } finally {
            if (contextClassLoader != null) Thread.currentThread().setContextClassLoader(contextClassLoader);
        }
        return null;
    }

    /**
     * Find the resource in a module (in listed order)
     *
     * @param name The resource name
     * @return the URL or null
     */
    @Override
    protected URL findResource(String name) {
        URL url = null;

        for (final CustomAssertionModule module : modules) {
            final ClassLoader delegateClassLoader = module.getModuleClassLoader(); // getModuleClassLoader cannot be null
            url = delegateClassLoader.getResource(name);
            if (url != null) break;
        }

        // if nothing found try the final ClassLoader
        if (url == null && finalLoader != null) {
            url = finalLoader.getResource(name);
        }

        return url;
    }

    /**
     * Find all the resource URLs (in listed order)
     *
     * @param name The resource name
     * @return the Enumeration (never null)
     * @throws IOException if an underlying loader throws IOException
     */
    @Override
    protected Enumeration<URL> findResources(String name) throws IOException {
        Set<URL> urlList = new LinkedHashSet<>();

        // pre-build the delegates list, since we need to access it again after the initial loop,
        // therefore the second access is not guarantied to be concurrent i.e. the modules view might be changed
        final List<ClassLoader> delegateLoaders = new ArrayList<>();
        for (final CustomAssertionModule module : modules) {
            delegateLoaders.add(module.getModuleClassLoader());
        }
        if (finalLoader != null) {
            delegateLoaders.add(finalLoader);
        }

        // initial loop
        for (ClassLoader delegateClassLoader : delegateLoaders) {
            urlList.addAll(Collections.list(delegateClassLoader.getResources(name)));
        }

        // second access
        //
        // since all the delegate loaders share a parent with this loader we must remove the parents
        // items from this list else there will be duplicates
        if (!delegateLoaders.isEmpty()) {
            ClassLoader parentLoader = delegateLoaders.get(0).getParent();
            if (parentLoader != null) urlList.removeAll(Collections.list(parentLoader.getResources(name)));
        }

        return Collections.enumeration(urlList);
    }

    //- PRIVATE

    /**
     * Concurrent and read-only view of scanned modules.
     */
    private final Collection<CustomAssertionModule> modules;

    /**
     * the final <code>ClassLoader</code> in the chain (may be null).
     */
    private final ClassLoader finalLoader;
}
