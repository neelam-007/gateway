package com.l7tech.server.policy;

import com.l7tech.util.ClassUtils;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;

/**
 * A ClassLoader that will find classes from the
 */
public class AllModulesClassLoader extends ClassLoader {
    private final @NotNull ServerAssertionRegistry serverAssertionRegistry;

    public AllModulesClassLoader(@NotNull ServerAssertionRegistry serverAssertionRegistry) {
        super(AllModulesClassLoader.class.getClassLoader());
        if (serverAssertionRegistry == null)
            throw new NullPointerException();
        this.serverAssertionRegistry = serverAssertionRegistry;
    }

    @Override
    protected Class<?> findClass(@NotNull String name) throws ClassNotFoundException {
        final String packageName = ClassUtils.getPackageName(name);
        final AssertionModule mod = serverAssertionRegistry.getModuleForPackage(packageName);
        if (mod == null)
            return super.findClass(name);
        final ClassLoader moduleClassLoader = mod.getModuleClassLoader();
        return moduleClassLoader.loadClass(name);
    }

    @Override
    protected URL findResource(@NotNull String name) {
        final String packageName = ClassUtils.getPackageName(name.replace('/', '.'));
        final AssertionModule mod = serverAssertionRegistry.getModuleForPackage(packageName);
        if (mod == null)
            return super.findResource(name);
        final ClassLoader cl = mod.getModuleClassLoader();
        return cl.getResource(name);
    }

    @Override
    protected Enumeration<URL> findResources(@NotNull String name) throws IOException {
        final String packageName = ClassUtils.getPackageName(name.replace('/', '.'));
        final AssertionModule mod = serverAssertionRegistry.getModuleForPackage(packageName);
        if (mod == null)
            return super.findResources(name);
        final ClassLoader cl = mod.getModuleClassLoader();
        return cl.getResources(name);
    }
}
