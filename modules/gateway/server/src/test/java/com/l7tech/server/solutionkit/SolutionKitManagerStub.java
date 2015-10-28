package com.l7tech.server.solutionkit;

import com.l7tech.gateway.common.solutionkit.SolutionKit;
import com.l7tech.gateway.common.solutionkit.SolutionKitHeader;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.server.EntityManagerStub;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class SolutionKitManagerStub extends EntityManagerStub<SolutionKit, SolutionKitHeader> implements SolutionKitManager {

    public SolutionKitManagerStub() {
        super();
    }

    public SolutionKitManagerStub(SolutionKit... solutionKitsIn) {
        super(solutionKitsIn);
    }

    @NotNull
    @Override
    public String importBundle(@NotNull String bundle, @NotNull SolutionKit metadata, boolean isTest) throws Exception {
        return "";
    }

    @Override
    public List<SolutionKit> findBySolutionKitGuid(@NotNull String solutionKitGuid) throws FindException {
        return Collections.emptyList();
    }

    @Override
    public SolutionKit findBySolutionKitGuidAndIM(@NotNull String solutionKitGuid, @Nullable String instanceModifier) throws FindException {
        return null;
    }

    @Override
    public List<SolutionKitHeader> findAllChildrenHeadersByParentGoid(@NotNull Goid parentGoid) throws FindException {
        return Collections.emptyList();
    }

    @NotNull
    @Override
    public Collection<SolutionKit> findAllChildrenByParentGoid(@NotNull Goid parentGoid) throws FindException {
        return Collections.emptyList();
    }
}