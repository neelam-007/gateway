package com.l7tech.gateway.common.solutionkit;

import com.l7tech.util.DeserializeSafe;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.util.Map;

/**
 * A SolutionKitInfo class to hold information needed for importing multiple bundles at a time in Restman.
 * Created by chaoy01 on 2017-10-20.
 */
@DeserializeSafe
public class SolutionKitInfo implements Serializable {
    private final Map<SolutionKit, String> solutionKitDelete;
    private final Map<SolutionKit, String> solutionKitInstall;
    private final SolutionKit parentSolutionKit;

    public SolutionKitInfo(@NotNull final Map<SolutionKit, String> solutionKitDelete,
                           @NotNull final Map<SolutionKit, String> solutionKitInstall,
                           @Nullable final SolutionKit parentSolutionKit) {
        this.solutionKitDelete = solutionKitDelete;
        this.solutionKitInstall = solutionKitInstall;
        this.parentSolutionKit = parentSolutionKit;
    }

    @NotNull
    public Map<SolutionKit, String> getSolutionKitDelete() {
        return solutionKitDelete;
    }

    @NotNull
    public Map<SolutionKit, String> getSolutionKitInstall() {
        return solutionKitInstall;
    }

    @Nullable
    public SolutionKit getParentSolutionKit() {
        return parentSolutionKit;
    }
}
