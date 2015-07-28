package com.l7tech.server.solutionkit;

import com.l7tech.gateway.common.solutionkit.SolutionKit;
import com.l7tech.gateway.common.solutionkit.SolutionKitException;
import com.l7tech.gateway.common.solutionkit.SolutionKitHeader;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.SaveException;
import com.l7tech.server.EntityManagerStub;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
    public String importBundle(@NotNull String migrationBundle, @Nullable String instanceModifier, boolean isTest) throws SaveException, SolutionKitException {
        return "";
    }

    @Override
    public List<SolutionKit> findBySolutionKitGuid(@NotNull String solutionKitGuid) throws FindException {
        return null;
    }

    @Override
    public void updateProtectedEntityTracking() throws FindException {}
}