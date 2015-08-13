package com.l7tech.gateway.common.api.solutionkit;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.gateway.api.Bundle;
import com.l7tech.gateway.api.Mappings;
import com.l7tech.gateway.api.impl.MarshallingUtils;
import com.l7tech.gateway.common.solutionkit.SolutionKit;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.ResourceUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Document;

import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
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
    private Set<SolutionKit> selected = new TreeSet<>();
    private Map<SolutionKit, Mappings> testMappings = new HashMap<>();
    private Map<SolutionKit, Map<String, String>> resolvedEntityIds = new HashMap<>();
    private List<SolutionKit> solutionKitsToUpgrade = new ArrayList<>();
    private Map<SolutionKit, SolutionKitCustomization> customizations = new HashMap<>();
    private Map<String, List<String>> instanceModifiers = new HashMap<>();
    private Map<SolutionKit, Boolean> upgradeInfoProvided = new HashMap<>();

    @Nullable
    private SolutionKit parentSolutionKit;

    public SolutionKitsConfig() {
    }

    @NotNull
    public Map<SolutionKit, Bundle> getLoadedSolutionKits() {
        return loaded;
    }

    @Nullable
    public Bundle getBundle(@NotNull SolutionKit solutionKit) {
        return loaded.get(solutionKit);
    }

    @Nullable
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

    @Nullable
    public Document getBundleAsDocument(@NotNull SolutionKit solutionKit) {
        Bundle bundle = getBundle(solutionKit);
        if (bundle != null) {
            DOMResult result = new DOMResult();
            try {
                MarshallingUtils.marshal(bundle, result, false);
                return (Document) result.getNode();
            } catch (IOException e) {
                logger.log(Level.WARNING, ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
                return null;
            }
        } else {
            return null;
        }
    }

    public void setBundle(@NotNull SolutionKit solutionKit, Document bundleDocument) throws IOException {
        final DOMSource bundleSource = new DOMSource();
        bundleSource.setNode(bundleDocument.getDocumentElement());

        // TODO fix duplicate HashMap bug where put(...) does not replace previous value
        loaded.put(solutionKit, MarshallingUtils.unmarshal(Bundle.class, bundleSource, true));
    }

    @NotNull
    public Set<SolutionKit> getSelectedSolutionKits() {
        return selected;
    }

    public void setSelectedSolutionKits(@NotNull Set<SolutionKit> selectedSolutionKits) {
        this.selected = selectedSolutionKits;
    }

    @Nullable
    public Mappings getTestMappings(@NotNull SolutionKit solutionKit) {
        return testMappings.get(solutionKit);
    }

    public void setTestMappings(@NotNull Map<SolutionKit, Mappings> testMappings) {
        this.testMappings = testMappings;
    }

    @NotNull
    public Map<SolutionKit, Map<String, String>> getResolvedEntityIds() {
        return resolvedEntityIds;
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

    @NotNull
    public List<SolutionKit> getSolutionKitsToUpgrade() {
        return solutionKitsToUpgrade;
    }

    public void setSolutionKitsToUpgrade(@Nullable List<SolutionKit> solutionKitsToUpgrade) {
        this.solutionKitsToUpgrade = solutionKitsToUpgrade;
    }

    @NotNull
    public Map<SolutionKit, SolutionKitCustomization> getCustomizations() {
        return customizations;
    }

    public boolean isUpgradeInfoProvided(SolutionKit solutionKit) {
        return upgradeInfoProvided.get(solutionKit);
    }

    public void setUpgradeInfoProvided(SolutionKit solutionKit, boolean upgradeInfo) {
        upgradeInfoProvided.put(solutionKit, upgradeInfo);
    }

    public Map<String, List<String>> getInstanceModifiers() {
        return instanceModifiers;
    }

    public void setInstanceModifiers(Map<String, List<String>> instanceModifiers) {
        this.instanceModifiers = instanceModifiers;
    }

    @Nullable
    public SolutionKit getParentSolutionKit() {
        return parentSolutionKit;
    }

    public void setParentSolutionKit(@Nullable SolutionKit parentSolutionKit) {
        this.parentSolutionKit = parentSolutionKit;
    }

    public void clear(boolean nullSolutionKitToUpgrade) {
        loaded.clear();
        selected.clear();
        testMappings.clear();
        resolvedEntityIds.clear();
        if (nullSolutionKitToUpgrade) {
            solutionKitsToUpgrade.clear();
        }
        clearCustomizations();
    }

    public void clear() {
        clear(true);
    }

    private void clearCustomizations() {
        for (final SolutionKitCustomization customization : customizations.values()) {
            ResourceUtils.closeQuietly(customization.getClassLoader());
        }
        customizations.clear();
    }
}