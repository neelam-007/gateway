package com.l7tech.server.solutionkit;

import com.l7tech.gateway.common.LicenseManager;
import com.l7tech.gateway.common.solutionkit.*;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.server.admin.AsyncAdminMethodsImpl;
import com.l7tech.server.bundling.EntityMappingInstructions;
import com.l7tech.server.bundling.EntityMappingResult;
import com.l7tech.server.policy.bundle.ssgman.restman.RestmanMessage;
import com.l7tech.server.policy.bundle.ssgman.restman.VersionModifier;
import com.l7tech.util.Background;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import javax.inject.Inject;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;

import static com.l7tech.server.bundling.EntityMappingInstructions.MappingAction.AlwaysCreateNew;
import static com.l7tech.server.bundling.EntityMappingResult.MappingAction.CreatedNew;
import static com.l7tech.server.event.AdminInfo.find;
import static com.l7tech.server.policy.bundle.ssgman.restman.RestmanMessage.MAPPING_ACTION_ATTRIBUTE;
import static com.l7tech.server.policy.bundle.ssgman.restman.RestmanMessage.MAPPING_ACTION_TAKEN_ATTRIBUTE;

public class SolutionKitAdminImpl extends AsyncAdminMethodsImpl implements SolutionKitAdmin {
    @Inject
    private SolutionKitManager solutionKitManager;
    @Inject
    private LicenseManager licenseManager;


    @SuppressWarnings("unused")   // used for spring configuration
    public SolutionKitAdminImpl() {}

    public SolutionKitAdminImpl(LicenseManager licenseManager, SolutionKitManager solutionKitManager) {
        this.licenseManager = licenseManager;
        this.solutionKitManager = solutionKitManager;
    }

    @NotNull
    @Override
    public Collection<SolutionKitHeader> findSolutionKits() throws FindException {
        return solutionKitManager.findAllHeaders();
    }

    @Override
    public SolutionKit get(@NotNull Goid goid) throws FindException {
        return solutionKitManager.findByPrimaryKey(goid);
    }

    @NotNull
    @Override
    public JobId<String> testInstall(@NotNull final SolutionKit solutionKit, @NotNull final String bundle) {
        final FutureTask<String> task =
            new FutureTask<>(find(false).wrapCallable(new Callable<String>() {
                @Override
                public String call() throws Exception {
                    checkFeatureEnabled(solutionKit);
                    final boolean isTest = true;
                    return solutionKitManager.importBundle(bundle, solutionKit.getProperty(SolutionKit.SK_PROP_INSTANCE_MODIFIER_KEY), isTest);
                }
            }));

        Background.scheduleOneShot(new TimerTask() {
            @Override
            public void run() {
                task.run();
            }
        }, 0L);

        return registerJob(task, String.class);
    }

    @NotNull
    @Override
    public JobId<Goid> install(@NotNull final SolutionKit solutionKit, @NotNull final String bundle, final boolean isUpgrade) {
        final FutureTask<Goid> task =
            new FutureTask<>(find(false).wrapCallable(new Callable<Goid>() {
                @Override
                public Goid call() throws Exception {
                    checkFeatureEnabled(solutionKit);

                    // Install bundle.
                    final boolean isTest = false;
                    String mappings = solutionKitManager.importBundle(bundle, solutionKit.getProperty(SolutionKit.SK_PROP_INSTANCE_MODIFIER_KEY), isTest);

                    // Save solution kit entity.
                    solutionKit.setMappings(mappings);

                    // Update the delete mapping probably due to new entities created or an instance modifier specified.
                    final String uninstallBundle = solutionKit.getUninstallBundle();
                    if (! StringUtils.isEmpty(uninstallBundle)) {
                        solutionKit.setUninstallBundle(
                            // Set 'targetId' in the uninstall bundle.
                            updateUninstallBundleBySettingTargetIds(uninstallBundle, mappings)
                        );
                    }

                    if (isUpgrade) {
                        solutionKitManager.update(solutionKit);
                        // TODO jwilliams: update ownership descriptors
                        return solutionKit.getGoid();
                    } else {
                        Goid solutionKitGoid = solutionKitManager.save(solutionKit);

                        createEntityOwnershipDescriptors(mappings, solutionKit);
                        solutionKitManager.update(solutionKit);
                        solutionKitManager.updateProtectedEntityTracking();

                        return solutionKitGoid;
                    }
                }
            }));

        Background.scheduleOneShot(new TimerTask() {
            @Override
            public void run() {
                task.run();
            }
        }, 0L);

        return registerJob(task, Goid.class);
    }

    @NotNull
    @Override
    public JobId<String> uninstall(@NotNull final Goid goid) {
        final FutureTask<String> task =
            new FutureTask<>(find(false).wrapCallable(new Callable<String>() {
                @Override
                public String call() throws Exception {
                    final boolean isTest = false;
                    final SolutionKit solutionKit = get(goid);
                    String resultMappings = "";
                    String uninstallBundle = solutionKit.getUninstallBundle();
                    if (uninstallBundle != null) {
                        final int numOfInstances = solutionKitManager.findBySolutionKitGuid(solutionKit.getSolutionKitGuid()).size();

                        // If there is more than one solution kit instance installed, then any instance uninstall should not
                        // delete those entities shared by other solution kit instances.  So need to remove the shared entities'
                        // mappings from the deletion mappings.  Otherwise, use the full deletion mappings without any changes.
                        if (numOfInstances > 1) {
                            final List<String> toBeIgnoredEntityTypes = new ArrayList<>();
                            String entityTypeStr;
                            final RestmanMessage uninstallMappingsMsg = new RestmanMessage(uninstallBundle);
                            for (Element element: uninstallMappingsMsg.getMappings()) {
                                entityTypeStr = element.getAttribute(RestmanMessage.MAPPING_TYPE_ATTRIBUTE);
                                if (! VersionModifier.isModifiableType(entityTypeStr)) {
                                    toBeIgnoredEntityTypes.add(entityTypeStr);
                                }
                            }
                            for (String toBeIgnored: toBeIgnoredEntityTypes) {
                                uninstallMappingsMsg.removeMappingByEntityType(toBeIgnored);
                            }
                            uninstallBundle = uninstallMappingsMsg.getAsString();
                        }

                        // Import the deletion bundle
                        resultMappings = solutionKitManager.importBundle(uninstallBundle, solutionKit.getProperty(SolutionKit.SK_PROP_INSTANCE_MODIFIER_KEY), isTest);
                    }
                    solutionKitManager.delete(goid);
                    solutionKitManager.updateProtectedEntityTracking();
                    return resultMappings;
                }
            }));

        Background.scheduleOneShot(new TimerTask() {
            @Override
            public void run() {
                task.run();
            }
        }, 0L);

        return registerJob(task, String.class);
    }

    private void checkFeatureEnabled(@NotNull final SolutionKit solutionKit) throws SolutionKitException {
        final String featureSet = solutionKit.getProperty(SolutionKit.SK_PROP_FEATURE_SET_KEY);
        if (!StringUtils.isEmpty(featureSet) && !licenseManager.isFeatureEnabled(featureSet)) {
            throw new SolutionKitException(solutionKit.getName() + " is unlicensed.  Required feature set " + featureSet);
        }
    }

    /**
     * Create EntityOwnershipDescriptors for each newly created entity.
     */
    private void createEntityOwnershipDescriptors(@NotNull final String resultMappings,
                                                  @NotNull final SolutionKit solutionKit) throws SAXException, IOException {
        Set<EntityOwnershipDescriptor> ownershipDescriptors = new HashSet<>();

        String entityId, entityType;

        // Find all matches of srdId and targetId in result mappings and save them in a map.
        final RestmanMessage mappingsMsg = new RestmanMessage(resultMappings);

        for (Element mapping : mappingsMsg.getMappings()) {
            if (AlwaysCreateNew != EntityMappingInstructions.MappingAction.valueOf(mapping.getAttribute(MAPPING_ACTION_ATTRIBUTE)) ||
                    CreatedNew != EntityMappingResult.MappingAction.valueOf(mapping.getAttribute(MAPPING_ACTION_TAKEN_ATTRIBUTE))) {
                continue;
            }

            entityId = mapping.getAttribute(RestmanMessage.MAPPING_TARGET_ID_ATTRIBUTE);
            entityType = mapping.getAttribute(RestmanMessage.MAPPING_TYPE_ATTRIBUTE);

            if (!StringUtils.isEmpty(entityId) && !StringUtils.isEmpty(entityType)) {
                EntityOwnershipDescriptor d = new EntityOwnershipDescriptor(solutionKit,
                        Goid.parseGoid(entityId), EntityType.valueOf(entityType), true);
                ownershipDescriptors.add(d);
            }
        }

        solutionKit.setEntityOwnershipDescriptors(ownershipDescriptors);
    }

    /**
     * After a new instance of a solution kit is installed and an instance modifier might be specified, the entity mappings
     * will contains targetId to replace srcId. In this case, the original uninstall mappings should be updated based on
     * the given entity mappings.
     */
    private String updateUninstallBundleBySettingTargetIds(@NotNull final String uninstallBundle, @NotNull final String resultMappings) throws SAXException, IOException {
        final Map<String, String> idsMap = new HashMap<>();
        String srcId, targetId;

        // Find all matches of srdId and targetId in result mappings and save them in a map.
        final RestmanMessage mappingsMsg = new RestmanMessage(resultMappings);
        for (Element element: mappingsMsg.getMappings()) {
            srcId = element.getAttribute(RestmanMessage.MAPPING_SRC_ID_ATTRIBUTE);
            targetId = element.getAttribute(RestmanMessage.MAPPING_TARGET_ID_ATTRIBUTE);
            if (!StringUtils.isEmpty(srcId) && !StringUtils.isEmpty(targetId))
                idsMap.put(srcId, targetId);
        }

        // Add targetId in the uninstall mappings
        final RestmanMessage uninstallMappingsMsg = new RestmanMessage(uninstallBundle);
        for (Element element: uninstallMappingsMsg.getMappings()) {
            srcId = element.getAttribute(RestmanMessage.MAPPING_SRC_ID_ATTRIBUTE);
            if (idsMap.containsKey(srcId)) {
                element.setAttribute(RestmanMessage.MAPPING_TARGET_ID_ATTRIBUTE, idsMap.get(srcId));
            }
        }

        return uninstallMappingsMsg.getAsString();
    }
}