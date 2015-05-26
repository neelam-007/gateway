package com.l7tech.server.module;

import com.l7tech.server.policy.module.BaseAssertionModule;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

/**
 * Custom or Modular Assertion Modules Finder.
 */
public interface AssertionModuleFinder<T extends BaseAssertionModule> {

    /**
     * Find the assertion module, if any, that owns the assertion specified with the {@code className}.
     *
     * @param className    the Assertion class name.  Required and cannot be {@code null}.
     * @return The module that registered the Assertion with the specified {@code className}, or {@code null} if
     * there are no modules registering Assertion with the specified {@code className}.
     */
    @Nullable
    T getModuleForAssertion(@NotNull String className);

    /**
     * Find the assertion module, if any, that owns the specified class loader.
     *
     * @param classLoader    the class loader to check.
     * @return The module that provides this {@code classLoader}, or {@code null} if no currently registered
     * assertion modules owns the specified {@code ClassLoader}.
     */
    @Nullable
    T getModuleForClassLoader(ClassLoader classLoader);

    /**
     * Find the most-recently-loaded loaded assertion module that contains at least one class or resource in the specified package.
     *
     * @param packageName    a package name.  Required and cannot be {@code null}.
     * @return The most-recently-loaded loaded assertion module that offers at least one file in this package, or {@code null} if there isn't one.
     */
    @Nullable
    T getModuleForPackage(@NotNull String packageName);

    /**
     * Gather all Modular or Custom Assertion modules which are currently loaded.
     *
     * @return a read-only view of all assertion modules which are currently loaded.  May be empty but never {@code null}.
     */
    @NotNull
    Set<T> getLoadedModules();
}
