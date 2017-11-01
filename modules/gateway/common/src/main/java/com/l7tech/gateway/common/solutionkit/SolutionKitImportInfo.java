package com.l7tech.gateway.common.solutionkit;

import com.l7tech.util.DeserializeSafe;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * A SolutionKitImportInfo class to hold information needed for importing multiple bundles at a time in Restman.
 * Created by chaoy01 on 2017-10-20.
 */
@DeserializeSafe
public class SolutionKitImportInfo implements Serializable {
    private final List<SolutionKit> solutionKitsToDelete;
    private final Map<SolutionKit, String> solutionKitsToInstall;
    private final SolutionKit parentSolutionKit;

    public SolutionKitImportInfo(@NotNull final List<SolutionKit> solutionKitsToDelete,
                                 @NotNull final Map<SolutionKit, String> solutionKitsToInstall,
                                 @Nullable final SolutionKit parentSolutionKit) {
        this.solutionKitsToDelete = solutionKitsToDelete;
        this.solutionKitsToInstall = solutionKitsToInstall;
        this.parentSolutionKit = parentSolutionKit;
    }

    @NotNull
    public List<SolutionKit> getSolutionKitsToDelete() {
        return solutionKitsToDelete;
    }

    @NotNull
    public Map<SolutionKit, String> getSolutionKitsToInstall() {
        return solutionKitsToInstall;
    }

    @Nullable
    public SolutionKit getParentSolutionKit() {
        return parentSolutionKit;
    }
}
