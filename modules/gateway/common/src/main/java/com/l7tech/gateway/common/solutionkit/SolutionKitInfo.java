package com.l7tech.gateway.common.solutionkit;

import com.l7tech.util.DeserializeSafe;

import java.io.Serializable;
import java.util.Map;

/**
 * A SolutionKitInfo class to hold information needed for importing multiple bundles at a time in Restman.
 * Created by chaoy01 on 2017-10-20.
 */
@DeserializeSafe
public class SolutionKitInfo implements Serializable {
    private Map<SolutionKit, String> solutionKitDelete;
    private Map<SolutionKit, String> solutionKitInstall;
    private SolutionKit parentSolutionKit;
    private boolean isUpgrade;

    public SolutionKitInfo(Map<SolutionKit, String> solutionKitDelete, Map<SolutionKit, String> solutionKitInstall, SolutionKit parentSolutionKit, boolean isUpgrade) {
        this.solutionKitDelete = solutionKitDelete;
        this.solutionKitInstall = solutionKitInstall;
        this.parentSolutionKit = parentSolutionKit;
        this.isUpgrade = isUpgrade;
    }

    public Map<SolutionKit, String> getSolutionKitDelete() {
        return solutionKitDelete;
    }

    public Map<SolutionKit, String> getSolutionKitInstall() {
        return solutionKitInstall;
    }

    public boolean isUpgrade() {
        return isUpgrade;
    }

    public void setUpgrade(boolean upgrade) {
        isUpgrade = upgrade;
    }

    public SolutionKit getParentSolutionKit() {
        return parentSolutionKit;
    }
}
