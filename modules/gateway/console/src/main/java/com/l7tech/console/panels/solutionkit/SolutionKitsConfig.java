package com.l7tech.console.panels.solutionkit;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.gateway.api.Bundle;
import com.l7tech.gateway.api.Mappings;
import com.l7tech.gateway.api.impl.MarshallingUtils;
import com.l7tech.gateway.common.solutionkit.SolutionKit;
import com.l7tech.util.ExceptionUtils;
import org.jetbrains.annotations.NotNull;


import javax.xml.transform.dom.DOMResult;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *  POJO to store user inputs for InstallSolutionKitWizard.
 */
public class SolutionKitsConfig {
    private static final Logger logger = Logger.getLogger(SolutionKitsConfig.class.getName());

    private Map<SolutionKit, Bundle> loaded = new HashMap<>();
    private Set<SolutionKit> selected = new HashSet<>();
    private Map<SolutionKit, Mappings> testMappings = new HashMap<>();
    private Map<SolutionKit, Map<String, String>> resolvedEntityIds = new HashMap<>();

    public SolutionKitsConfig() {
    }

    @NotNull
    public Set<SolutionKit> getLoadedSolutionKits() {
        return loaded.keySet();
    }

    public void setLoadedSolutionKits(@NotNull Map<SolutionKit, Bundle> loaded) {
        this.loaded = loaded;
    }

    public Bundle getBundle(@NotNull SolutionKit solutionKit) {
        return loaded.get(solutionKit);
    }

    public String getBundleAsString(@NotNull SolutionKit solutionKit) {
        Bundle bundle = getBundle(solutionKit);
        if (bundle != null) {
            DOMResult result = new DOMResult();
            try {
                MarshallingUtils.marshal(bundle, result, false);
                return XmlUtil.nodeToString(result.getNode());
            } catch (IOException e) {
                logger.log(Level.WARNING, ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
                return null;
            }
        } else {
            return null;
        }
    }

    @NotNull
    public Set<SolutionKit> getSelectedSolutionKits() {
        return selected;
    }

    public void setSelectedSolutionKits(@NotNull Set<SolutionKit> selectedSolutionKits) {
        this.selected = selectedSolutionKits;
    }

    public Mappings getTestMappings(@NotNull SolutionKit solutionKit) {
        return testMappings.get(solutionKit);
    }

    public void setTestMappings(@NotNull Map<SolutionKit, Mappings> testMappings) {
        this.testMappings = testMappings;
    }

    @NotNull
    public Map<String, String> getResolvedEntityIds(@NotNull SolutionKit solutionKit) {
        Map<String, String> result = resolvedEntityIds.get(solutionKit);
        if (result == null) {
            return Collections.emptyMap();
        }
        return result;
    }

    public void setResolvedEntityIds(@NotNull Map<SolutionKit, Map<String, String>> resolvedEntityIds) {
        this.resolvedEntityIds = resolvedEntityIds;
    }
}