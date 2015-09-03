package com.l7tech.gateway.common.api.solutionkit;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.gateway.api.Bundle;
import com.l7tech.gateway.api.Item;
import com.l7tech.gateway.api.Mapping;
import com.l7tech.gateway.api.Mappings;
import com.l7tech.gateway.api.impl.MarshallingUtils;
import com.l7tech.gateway.common.solutionkit.ForbiddenException;
import com.l7tech.gateway.common.solutionkit.SolutionKit;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.Pair;
import com.l7tech.util.ResourceUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Document;

import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamSource;
import java.io.IOException;
import java.io.StringReader;
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

    // using SolutionKit.sk_guid as the map key prevents changes to solution kit from losing reference to the original map value (e.g. SolutionKit.hashcode() changes)
    private Map<String, Pair<SolutionKit, Map<String, String>>> resolvedEntityIds = new HashMap<>();

    private List<SolutionKit> solutionKitsToUpgrade = new ArrayList<>();

    // using SolutionKit.sk_guid as the map key prevents changes to solution kit from losing reference to the original map value (e.g. SolutionKit.hashcode() changes)
    private Map<String, Pair<SolutionKit, SolutionKitCustomization>> customizations = new HashMap<>();

    private Map<String, List<String>> instanceModifiers = new HashMap<>();
    private Map<SolutionKit, Boolean> upgradeInfoProvided = new HashMap<>();

    public static final String MAPPING_PROPERTY_NAME_SK_ALLOW_MAPPING_OVERRIDE = "SK_AllowMappingOverride";

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
    public Map<String, Pair<SolutionKit, Map<String, String>>> getResolvedEntityIds() {
        return resolvedEntityIds;
    }

    @NotNull
    public Pair<SolutionKit, Map<String, String>> getResolvedEntityIds(@NotNull String solutionKitGuid) {
        Pair<SolutionKit, Map<String, String>> result = resolvedEntityIds.get(solutionKitGuid);
        if (result == null) {
            final Map<String, String> emptyMap = Collections.emptyMap();
            return new Pair<>(null, emptyMap);
        }
        return result;
    }

    public void setResolvedEntityIds(@NotNull Map<String, Pair<SolutionKit, Map<String, String>>> resolvedEntityIds) {
        this.resolvedEntityIds = resolvedEntityIds;
    }

    @NotNull
    public List<SolutionKit> getSolutionKitsToUpgrade() {
        return solutionKitsToUpgrade;
    }

    public void setSolutionKitsToUpgrade(@NotNull final List<SolutionKit> solutionKitsToUpgrade) {
        this.solutionKitsToUpgrade = solutionKitsToUpgrade;
    }

    @NotNull
    public Map<String, Pair<SolutionKit, SolutionKitCustomization>> getCustomizations() {
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
        for (final Pair<SolutionKit, SolutionKitCustomization> customization : customizations.values()) {
            if (customization.right != null) {
                ResourceUtils.closeQuietly(customization.right.getClassLoader());
            }
        }
        customizations.clear();
    }

    /**
     * Utility method, to be used only during upgrade, to set previously resolved user mapping.
     * Find and set previously installed mappings where srcId differs from targetId (e.g. user resolved).
     */
    public void onUpgradeResetPreviouslyInstalledMappings() {
        // get all kits to upgrade
        final List<SolutionKit> solutionKitsToUpgrade = getSolutionKitsToUpgrade();

        // Note that if it is a collection of solution kits for upgrade, then the first element in solutionKitsToUpgrade is a parent solution kit, which should not be upgraded.
        final int startIdx = solutionKitsToUpgrade.size() > 1 ? 1 : 0;

        for (int i = startIdx; i < solutionKitsToUpgrade.size(); i++) {
            final SolutionKit solutionKitToUpgrade = solutionKitsToUpgrade.get(i);
            if (!SolutionKit.PARENT_SOLUTION_KIT_DUMMY_MAPPINGS.equals(solutionKitToUpgrade.getMappings())) {
                try {
                    final Item item = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(solutionKitToUpgrade.getMappings())));
                    // TODO: ghuang; can this happen and if so throw appropriate error
                    assert item.getContent() != null && item.getContent() instanceof Mappings;
                    final Mappings mappings = (Mappings) item.getContent();
                    Map<String, String> previouslyResolvedIds = new HashMap<>();
                    for (Mapping mapping : mappings.getMappings()) {
                        if (!mapping.getSrcId().equals(mapping.getTargetId()) ) {
                            previouslyResolvedIds.put(mapping.getSrcId(), mapping.getTargetId());
                        }
                    }
                    if (!previouslyResolvedIds.isEmpty()) {
                        getResolvedEntityIds().put(solutionKitToUpgrade.getSolutionKitGuid(), new Pair<>(solutionKitToUpgrade, previouslyResolvedIds));
                    }
                } catch (IOException e) {
                    throw new IllegalArgumentException(ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
                }
            }
        }
    }

    /**
     * Utility method for updating resolved mapping target IDs into the bundle itself, before executing dry-run or install.
     *
     * @param solutionKit    the SolutionKit holding the resolved entities.  Required and cannot be {@code null}.
     * @throws ForbiddenException if {@link #MAPPING_PROPERTY_NAME_SK_ALLOW_MAPPING_OVERRIDE} is not set by the SKAR author.
     */
    public void updateResolvedMappingsIntoBundle(@NotNull final SolutionKit solutionKit) throws ForbiddenException {
        // TODO: this todo is valid only during headless install
        // TODO: the logic looks safe enough to be executed twice i.e. once per dry-run and afterwards (assuming no conflicts) on install (SkarProcessor.installOrUpgrade)
        // TODO: ghuang; would you double-check if this logic can be called twice in a row?
        final Pair<SolutionKit, Map<String, String>> resolvedEntityIds = getResolvedEntityIds(solutionKit.getSolutionKitGuid());
        final Bundle bundle = getBundle(solutionKit);
        if (bundle != null) {
            for (final Mapping mapping : bundle.getMappings()) {
                final String resolvedId = resolvedEntityIds.right == null ? null : resolvedEntityIds.right.get(mapping.getSrcId());
                if (resolvedId != null) {
                    final Boolean allowOverride = mapping.getProperty(MAPPING_PROPERTY_NAME_SK_ALLOW_MAPPING_OVERRIDE);
                    if (allowOverride != null && allowOverride) {
                        mapping.setTargetId(resolvedId);
                    } else {
                        throw new ForbiddenException("Unable to process entity ID replace for mapping with scrId=" + mapping.getSrcId() +
                                ".  Replacement id=" + resolvedId + " requires the .skar author to set mapping property '" + MAPPING_PROPERTY_NAME_SK_ALLOW_MAPPING_OVERRIDE + "' to true.");
                    }
                }
            }
        }
    }
}