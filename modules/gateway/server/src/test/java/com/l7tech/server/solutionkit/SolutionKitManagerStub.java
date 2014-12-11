package com.l7tech.server.solutionkit;

import com.l7tech.gateway.common.solutionkit.SolutionKit;
import com.l7tech.gateway.common.solutionkit.SolutionKitException;
import com.l7tech.gateway.common.solutionkit.SolutionKitHeader;
import com.l7tech.objectmodel.DeleteException;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.SaveException;
import com.l7tech.server.EntityManagerStub;
import org.jetbrains.annotations.NotNull;

public class SolutionKitManagerStub extends EntityManagerStub<SolutionKit, SolutionKitHeader> implements SolutionKitManager {

    public SolutionKitManagerStub() {
        super();
    }

    public SolutionKitManagerStub(SolutionKit... solutionKitsIn) {
        super(solutionKitsIn);
    }

    @NotNull
    @Override
    public String installBundle(@NotNull SolutionKit solutionKit, @NotNull String migrationBundle, boolean isTest) throws SaveException, SolutionKitException {
        return "";
    }

    @Override
    public void uninstallBundle(@NotNull Goid goid) throws FindException, DeleteException, SolutionKitException {
    }
}