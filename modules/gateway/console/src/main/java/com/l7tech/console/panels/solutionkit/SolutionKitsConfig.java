package com.l7tech.console.panels.solutionkit;

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
    private Set<SolutionKit> selected = new HashSet<>();
    private Map<SolutionKit, Mappings> testMappings = new HashMap<>();
    private Map<SolutionKit, Map<String, String>> resolvedEntityIds = new HashMap<>();
    @Nullable
    private SolutionKit solutionKitToUpgrade;
    private Map<SolutionKit, SolutionKitCustomization> customizations = new HashMap<>();

    public SolutionKitsConfig() {
    }

    @NotNull
    public Map<SolutionKit, Bundle> getLoadedSolutionKits() {
        return loaded;
    }

    public void setLoadedSolutionKits(@NotNull Map<SolutionKit, Bundle> loaded) {
        this.loaded = loaded;
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

    @Nullable
    public SolutionKit getSingleSelectedSolutionKit() {
        if (!selected.isEmpty()) {
            // todo (kpak): Multiple solution kits not supported. Return the first item.
            // When multiple solution kits are supported, this method should be removed
            // and replaced with #getSelectedSolutionKits().
            return selected.iterator().next();
        }
        return null;
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

    @Nullable
    public SolutionKit getSolutionKitToUpgrade() {
        return solutionKitToUpgrade;
    }

    public void setSolutionKitToUpgrade(@Nullable SolutionKit solutionKitToUpgrade) {
        this.solutionKitToUpgrade = solutionKitToUpgrade;
    }

    @NotNull
    public Map<SolutionKit, SolutionKitCustomization> getCustomizations() {
        return customizations;
    }

    public void clear() {
        loaded.clear();
        selected.clear();
        testMappings.clear();
        resolvedEntityIds.clear();
        solutionKitToUpgrade = null;
        clearCustomizations();
    }

    private void clearCustomizations() {
        for (final SolutionKitCustomization customization : customizations.values()) {
            ResourceUtils.closeQuietly(customization.getClassLoader());
        }
        customizations.clear();
    }
}