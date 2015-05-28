package com.l7tech.server.module;

import com.l7tech.server.policy.module.CustomAssertionModule;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.Set;

/**
 * Stub for Custom {@link com.l7tech.server.module.AssertionModuleFinder}
 */
public class CustomAssertionModuleFinderStub implements AssertionModuleFinder<CustomAssertionModule> {

    @Nullable
    @Override
    public CustomAssertionModule getModuleForAssertion(@NotNull final String className) {
        return null;
    }

    @Nullable
    @Override
    public CustomAssertionModule getModuleForClassLoader(final ClassLoader classLoader) {
        return null;
    }

    @Nullable
    @Override
    public CustomAssertionModule getModuleForPackage(@NotNull final String packageName) {
        return null;
    }

    @NotNull
    @Override
    public Set<CustomAssertionModule> getLoadedModules() {
        return Collections.emptySet();
    }
}
