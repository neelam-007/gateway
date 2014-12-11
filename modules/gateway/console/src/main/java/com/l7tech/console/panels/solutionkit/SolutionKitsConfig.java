package com.l7tech.console.panels.solutionkit;

import com.l7tech.gateway.common.solutionkit.SolutionKit;
import org.jetbrains.annotations.NotNull;


import java.util.*;

/**
 *  POJO to store user inputs for InstallSolutionKitWizard.
 */
public class SolutionKitsConfig {
    private Map<SolutionKit, String> loadedSolutionKits = new HashMap<>();
    private Set<SolutionKit> selectedSolutionKits = new HashSet<>();
    private Map<SolutionKit, String> testMappingResults = new HashMap<>();

    public SolutionKitsConfig() {
    }

    @NotNull
    public Set<SolutionKit> getLoadedSolutionKits() {
        return loadedSolutionKits.keySet();
    }

    @NotNull
    public String getMigrationBundle(@NotNull SolutionKit solutionKit) {
        return loadedSolutionKits.get(solutionKit);
    }

    public void setLoadedSolutionKits(@NotNull Map<SolutionKit, String> loadedSolutionKits) {
        this.loadedSolutionKits = loadedSolutionKits;
    }

    @NotNull
    public Set<SolutionKit> getSelectedSolutionKits() {
        return selectedSolutionKits;
    }

    public void setSelectedSolutionKits(@NotNull Set<SolutionKit> selectedSolutionKits) {
        this.selectedSolutionKits = selectedSolutionKits;
    }

    @NotNull
    public Map<SolutionKit, String> getTestMappingResults() {
        return testMappingResults;
    }

    public void setTetMappingResults(@NotNull Map<SolutionKit, String> testMappingResults) {
        this.testMappingResults = testMappingResults;
    }
}