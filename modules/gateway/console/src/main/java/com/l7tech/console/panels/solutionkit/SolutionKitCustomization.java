package com.l7tech.console.panels.solutionkit;

import com.l7tech.policy.solutionkit.SolutionKitManagerCallback;
import com.l7tech.policy.solutionkit.SolutionKitManagerUi;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Holder classes required for customizations.
 */
public class SolutionKitCustomization {
    @NotNull
    private final SolutionKitCustomizationClassLoader classLoader;
    @Nullable
    private final SolutionKitManagerCallback customCallback;
    @Nullable
    private final SolutionKitManagerUi customUi;

    public SolutionKitCustomization(@NotNull final SolutionKitCustomizationClassLoader classLoader,
                                    @Nullable final SolutionKitManagerUi customUi,
                                    @Nullable final SolutionKitManagerCallback customCallback) {
        this.classLoader = classLoader;
        this.customUi = customUi;
        this.customCallback = customCallback;
    }

    @NotNull
    public SolutionKitCustomizationClassLoader getClassLoader() {
        return classLoader;
    }

    @Nullable
    public SolutionKitManagerCallback getCustomCallback() {
        return customCallback;
    }

    @Nullable
    public SolutionKitManagerUi getCustomUi() {
        return customUi;
    }
}
