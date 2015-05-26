package com.l7tech.server.module;

import com.l7tech.server.policy.module.ModularAssertionModule;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.Set;

/**
 * Stab for Modular {@link com.l7tech.server.module.AssertionModuleFinder}
 */
public class ModularAssertionModuleFinderStub implements AssertionModuleFinder<ModularAssertionModule> {

    @Nullable
    @Override
    public ModularAssertionModule getModuleForAssertion(@NotNull final String className) {
        return null;
    }

    @Nullable
    @Override
    public ModularAssertionModule getModuleForClassLoader(final ClassLoader classLoader) {
        return null;
    }

    @Nullable
    @Override
    public ModularAssertionModule getModuleForPackage(@NotNull final String packageName) {
        return null;
    }

    @NotNull
    @Override
    public Set<ModularAssertionModule> getLoadedModules() {
        return Collections.emptySet();
    }
}
