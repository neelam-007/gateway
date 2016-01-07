package com.l7tech.gateway.common.solutionkit;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.gateway.api.Bundle;
import com.l7tech.gateway.api.Item;
import com.l7tech.gateway.api.Mapping;
import com.l7tech.gateway.api.Mappings;
import com.l7tech.gateway.api.impl.MarshallingUtils;
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
    private Map<String, Map<String, Mapping>> installMappingsMap = new HashMap<>();

    // using SolutionKit.sk_guid as the map key prevents changes to solution kit from losing reference to the original map value (e.g. SolutionKit.hashcode() changes)
    private Map<String, Pair<SolutionKit, Map<String, String>>> resolvedEntityIds = new HashMap<>();
    private Map<String, Pair<SolutionKit, Map<String, String>>> previouslyResolvedEntityIds = new HashMap<>();

    private List<SolutionKit> solutionKitsToUpgrade = new ArrayList<>();

    // using SolutionKit.sk_guid as the map key prevents changes to solution kit from losing reference to the original map value (e.g. SolutionKit.hashcode() changes)
    private Map<String, Pair<SolutionKit, SolutionKitCustomization>> customizations = new HashMap<>();

    private Map<SolutionKit, Boolean> upgradeInfoProvided = new HashMap<>();
    private Map<String, Pair<String, String>> selectedGuidAndImForHeadlessUpgrade = new HashMap<>(); // The map of guid and instance modifier of selected solution kits for headless upgrade.

    public static final String MAPPING_PROPERTY_NAME_SK_ALLOW_MAPPING_OVERRIDE = "SK_AllowMappingOverride";

    @Nullable
    private SolutionKit parentSolutionKitLoaded;

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
    public Map<String, Pair<SolutionKit, Map<String, String>>> getPreviouslyResolvedEntityIds() {
        return previouslyResolvedEntityIds;
    }

    @NotNull
    public List<SolutionKit> getSolutionKitsToUpgrade() {
        return solutionKitsToUpgrade;
    }

    public void setSolutionKitsToUpgrade(@NotNull final List<SolutionKit> solutionKitsToUpgrade) {
        this.solutionKitsToUpgrade = solutionKitsToUpgrade;
    }

    public boolean isUpgrade() {
        return solutionKitsToUpgrade.size() > 0;
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

    @NotNull
    public Map<String, Mapping> getInstallMappings(@NotNull String solutionKitGuid) {
        Map<String, Mapping> installMappings = installMappingsMap.get(solutionKitGuid);
        if (installMappings == null) {
            installMappings = new HashMap<>();
            installMappingsMap.put(solutionKitGuid, installMappings);
        }
        return installMappings;
    }

    @Nullable
    public SolutionKit getParentSolutionKitLoaded() {
        return parentSolutionKitLoaded;
    }

    public void setParentSolutionKitLoaded(@Nullable SolutionKit parentSolutionKitLoaded) {
        this.parentSolutionKitLoaded = parentSolutionKitLoaded;
    }

    public Map<String, Pair<String, String>> getSelectedGuidAndImForHeadlessUpgrade() {
        return selectedGuidAndImForHeadlessUpgrade;
    }

    public void clear(boolean nullSolutionKitToUpgrade) {
        loaded.clear();
        selected.clear();
        testMappings.clear();
        installMappingsMap.clear();
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
     * Set previously resolved IDs, based on the actual installed entity IDs (which was saved in the mappings).
     * This implies an upgrade, so the method only selects solution kits marked for upgrade.
     * Method then finds and sets last installed mappings where srcId differs from targetId (e.g. user resolved).
     */
    public void setPreviouslyResolvedIds() {
        // get all kits to upgrade
        final List<SolutionKit> solutionKitsToUpgrade = getSolutionKitsToUpgrade();

        // Note that if it is a collection of solution kits for upgrade, then the first element in solutionKitsToUpgrade is a parent solution kit, which should not be upgraded.
        final int startIdx = solutionKitsToUpgrade.size() > 1 ? 1 : 0;

        for (int i = startIdx; i < solutionKitsToUpgrade.size(); i++) {
            final SolutionKit solutionKitToUpgrade = solutionKitsToUpgrade.get(i);
            if (! SolutionKitUtils.isParentSolutionKit(solutionKitToUpgrade)) {
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
                        getPreviouslyResolvedEntityIds().put(solutionKitToUpgrade.getSolutionKitGuid(), new Pair<>(solutionKitToUpgrade, previouslyResolvedIds));
                    }
                } catch (IOException e) {
                    throw new IllegalArgumentException(ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
                }
            }
        }
    }

    /**
     * Set mapping targetId if mapping srcId matches resolved ID.
     * For security, we check if the mapping is allowed to be overridden.
     * We resolve an entity ID when processing User Configurable Entity (e.g. map JDBC connection ID in bundle to JDBC connection already in user's environment).
     *
     * @param solutionKit the SolutionKit holding the resolved entities.  Required and cannot be {@code null}.
     * @throws ForbiddenException if {@link #MAPPING_PROPERTY_NAME_SK_ALLOW_MAPPING_OVERRIDE} is not set by the SKAR author.
     */
    public void setMappingTargetIdsFromResolvedIds(@NotNull final SolutionKit solutionKit) throws ForbiddenException {
        final Pair<SolutionKit, Map<String, String>> resolvedEntityIds = getResolvedEntityIds(solutionKit.getSolutionKitGuid());
        final Bundle bundle = getBundle(solutionKit);
        if (bundle != null) {
            for (final Mapping mapping : bundle.getMappings()) {
                final String resolvedId = resolvedEntityIds.right == null ? null : resolvedEntityIds.right.get(mapping.getSrcId());
                if (resolvedId != null) {
                    if (allowOverride(mapping)) {
                        mapping.setTargetId(resolvedId);
                    } else {
                        throw new ForbiddenException("Unable to process entity ID replace for mapping with scrId=" + mapping.getSrcId() +
                                ".  Replacement id=" + resolvedId + " requires the .skar file author to set mapping property '" + MAPPING_PROPERTY_NAME_SK_ALLOW_MAPPING_OVERRIDE + "' to true.");
                    }
                }
            }
        }
    }

    /**
     * Set mapping targetId if mapping srcId matches *previously* resolved ID.
     * Previously resolved ID is set by {@link #setPreviouslyResolvedIds()}.  We resolve a previous entity ID when upgrading, we map the ID in bundle to an actual installed ID.
     * We ignore if there's already an existing mapping targetId.  This is needed to preserve instance modified entity which uses a *deterministic* modified ID (see {@link com.l7tech.gateway.common.solutionkit.InstanceModifier#apply()}).
     *
     * @param solutionKit the SolutionKit holding the resolved entities.  Required and cannot be {@code null}.
     * @param bundle the bundle XML to install or upgrade
     */
    public void setMappingTargetIdsFromPreviouslyResolvedIds(@NotNull final SolutionKit solutionKit, @NotNull final Bundle bundle) {
        Pair<SolutionKit, Map<String, String>> previouslyResolvedIds = getPreviouslyResolvedEntityIds().get(solutionKit.getSolutionKitGuid());
        for (Mapping mapping : bundle.getMappings()) {
            if (previouslyResolvedIds != null) {
                String resolvedId = previouslyResolvedIds.right.get(mapping.getSrcId());
                if (resolvedId != null) {
                    mapping.setTargetId(resolvedId);
                }
            }
        }
    }

    public static boolean allowOverride(@NotNull final Mapping mapping) {
        final Boolean allowOverride = mapping.getProperty(SolutionKitsConfig.MAPPING_PROPERTY_NAME_SK_ALLOW_MAPPING_OVERRIDE);
        return allowOverride != null && allowOverride;
    }
}