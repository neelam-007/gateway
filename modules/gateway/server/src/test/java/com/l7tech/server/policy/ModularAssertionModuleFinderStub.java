package com.l7tech.server.policy;

import com.l7tech.server.policy.module.ModularAssertionModule;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.Set;

/**
 * Stab for {@link AssertionModuleFinder}
 */
public class ModularAssertionModuleFinderStub implements AssertionModuleFinder<ModularAssertionModule>{

    @Override
    public ModularAssertionModule getModuleForClassLoader(final ClassLoader classLoader) {
        return null;
    }

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
