package com.l7tech.server.solutionkit;

import com.l7tech.gateway.common.LicenseManager;
import com.l7tech.gateway.common.solutionkit.*;
import com.l7tech.objectmodel.*;
import com.l7tech.server.admin.AsyncAdminMethodsImpl;
import com.l7tech.server.bundling.EntityMappingInstructions;
import com.l7tech.server.bundling.EntityMappingResult;
import com.l7tech.server.policy.bundle.ssgman.restman.RestmanMessage;
import com.l7tech.server.policy.bundle.ssgman.restman.VersionModifier;
import com.l7tech.server.security.signer.SignatureVerifier;
import com.l7tech.util.Background;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.IOException;
import java.security.SignatureException;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;

import static com.l7tech.server.bundling.EntityMappingInstructions.MappingAction.Ignore;
import static com.l7tech.server.bundling.EntityMappingResult.MappingAction.CreatedNew;
import static com.l7tech.server.bundling.EntityMappingResult.MappingAction.Deleted;
import static com.l7tech.server.event.AdminInfo.find;
import static com.l7tech.server.policy.bundle.ssgman.restman.RestmanMessage.MAPPING_ACTION_ATTRIBUTE;
import static com.l7tech.server.policy.bundle.ssgman.restman.RestmanMessage.MAPPING_ACTION_TAKEN_ATTRIBUTE;

public class SolutionKitAdminImpl extends AsyncAdminMethodsImpl implements SolutionKitAdmin {
    @Inject
    private SolutionKitManager solutionKitManager;
    @Inject
    private LicenseManager licenseManager;

    @Inject
    @Named( "signatureVerifier" )
    final void setSignatureVerifier(final SignatureVerifier signatureVerifier) {
        this.signatureVerifier = signatureVerifier;
    }
    private SignatureVerifier signatureVerifier;


    @SuppressWarnings("unused")   // used for spring configuration
    public SolutionKitAdminImpl() {}

    public SolutionKitAdminImpl(LicenseManager licenseManager, SolutionKitManager solutionKitManager, SignatureVerifier signatureVerifier) {
        this.licenseManager = licenseManager;
        this.solutionKitManager = solutionKitManager;
        this.signatureVerifier = signatureVerifier;
    }

    @NotNull
    @Override
    public Collection<SolutionKitHeader> findSolutionKits() throws FindException {
        return solutionKitManager.findAllHeaders();
    }

    @NotNull
    @Override
    public Collection<SolutionKit> findBySolutionKitGuid(@NotNull String solutionKitGuid) throws FindException {
        return solutionKitManager.findBySolutionKitGuid(solutionKitGuid);
    }

    @NotNull
    @Override
    public Collection<SolutionKitHeader> findAllChildrenByParentGoid(Goid parentGoid) throws FindException {
        return solutionKitManager.findAllChildrenByParentGoid(parentGoid);
    }

    @NotNull
    @Override
    public Collection<SolutionKitHeader> findAllExcludingChildren() throws FindException {
        return solutionKitManager.findAllExcludingChildren();
    }

    @NotNull
    @Override
    public Collection<SolutionKitHeader> findParentSolutionKits() throws FindException {
        return solutionKitManager.findParentSolutionKits();
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
                        updateEntityOwnershipDescriptors(mappings, solutionKit);
                        solutionKitManager.update(solutionKit);
                        return solutionKit.getGoid();
                    } else {
                        Goid solutionKitGoid = solutionKitManager.save(solutionKit);
                        updateEntityOwnershipDescriptors(mappings, solutionKit);
                        solutionKitManager.update(solutionKit);
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
                    solutionKitManager.delete(solutionKit);
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

    @NotNull
    @Override
    public Goid saveSolutionKit(@NotNull SolutionKit solutionKit) throws SaveException {
        return solutionKitManager.save(solutionKit);
    }

    @Override
    public void updateSolutionKit(@NotNull SolutionKit solutionKit) throws UpdateException {
        solutionKitManager.update(solutionKit);
    }

    @Override
    public void deleteSolutionKit(@NotNull Goid goid) throws FindException, DeleteException {
        solutionKitManager.delete(goid);
    }

    @Override
    public void verifySkarSignature(@NotNull final byte[] digest, @Nullable final String signatureProperties) throws SignatureException {
        signatureVerifier.verify(digest, signatureProperties);
    }

    private void checkFeatureEnabled(@NotNull final SolutionKit solutionKit) throws SolutionKitException {
        final String featureSet = solutionKit.getProperty(SolutionKit.SK_PROP_FEATURE_SET_KEY);
        if (!StringUtils.isEmpty(featureSet) && !licenseManager.isFeatureEnabled(featureSet)) {
            throw new ForbiddenException(solutionKit.getName() + " is unlicensed.  Required feature set " + featureSet);
        }
    }

    /**
     * Create EntityOwnershipDescriptors for each newly created entity.
     */
    private void updateEntityOwnershipDescriptors(@NotNull final String resultMappings,
                                                  @NotNull final SolutionKit solutionKit) throws SAXException, IOException {
        final HashMap<String, EntityType> deletedEntities = new HashMap<>();

        final Set<EntityOwnershipDescriptor> newOwnershipDescriptors = new HashSet<>();
        final Set<EntityOwnershipDescriptor> obsoleteOwnershipDescriptors = new HashSet<>();

        String entityId, entityType;

        // Find all matches of srdId and targetId in result mappings and save them in a map.
        final RestmanMessage mappingsMsg = new RestmanMessage(resultMappings);

        for (Element mapping : mappingsMsg.getMappings()) {
            if (Ignore == EntityMappingInstructions.MappingAction.valueOf(mapping.getAttribute(MAPPING_ACTION_ATTRIBUTE)))
                continue;

            entityId = mapping.getAttribute(RestmanMessage.MAPPING_TARGET_ID_ATTRIBUTE);
            entityType = mapping.getAttribute(RestmanMessage.MAPPING_TYPE_ATTRIBUTE);

            if (!StringUtils.isEmpty(entityId) && !StringUtils.isEmpty(entityType)) {
                EntityMappingResult.MappingAction mappingResultAction =
                        EntityMappingResult.MappingAction.valueOf(mapping.getAttribute(MAPPING_ACTION_TAKEN_ATTRIBUTE));

                if (Deleted == mappingResultAction) {
                    deletedEntities.put(entityId, EntityType.valueOf(entityType));
                } else if (CreatedNew == mappingResultAction) {
                    EntityOwnershipDescriptor d = new EntityOwnershipDescriptor(solutionKit, entityId, EntityType.valueOf(entityType), true);
                    newOwnershipDescriptors.add(d);
                }
            }
        }

        /**
         * Look at each existing EntityOwnershipDescriptor to see if it is obsolete - check for the owned entity in
         * the map of deleted ones. We'll remove each deleted entity from the deletedEntities map, and when it is
         * empty we can stop iterating over the EntityOwnershipDescriptors; the Solution Kit is likely to have more
         * entities than would be deleted in a upgrade scenario.
         */
        if (null != solutionKit.getEntityOwnershipDescriptors() && !deletedEntities.isEmpty()) {
            for (EntityOwnershipDescriptor descriptor : solutionKit.getEntityOwnershipDescriptors()) {
                if (0 == deletedEntities.size())
                    break;

                EntityType deletedEntityType = deletedEntities.get(descriptor.getEntityId());

                if (null != deletedEntityType) {
                    obsoleteOwnershipDescriptors.add(descriptor);
                    deletedEntities.remove(descriptor.getEntityId());
                }
            }

            solutionKit.removeEntityOwnershipDescriptors(obsoleteOwnershipDescriptors);
        }

        solutionKit.addEntityOwnershipDescriptors(newOwnershipDescriptors);
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