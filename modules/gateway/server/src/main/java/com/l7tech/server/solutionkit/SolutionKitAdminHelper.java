package com.l7tech.server.solutionkit;

import com.l7tech.gateway.api.Mapping;
import com.l7tech.gateway.common.LicenseManager;
import com.l7tech.gateway.common.solutionkit.*;
import com.l7tech.identity.IdentityProviderConfig;
import com.l7tech.identity.IdentityProviderConfigManager;
import com.l7tech.identity.IdentityProviderType;
import com.l7tech.objectmodel.*;
import com.l7tech.server.bundling.EntityMappingResult;
import com.l7tech.server.policy.bundle.ssgman.restman.RestmanMessage;
import com.l7tech.util.ExceptionUtils;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.Serializable;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.l7tech.server.policy.bundle.ssgman.restman.RestmanMessage.MAPPING_ACTION_TAKEN_ATTRIBUTE;
import static com.l7tech.server.policy.bundle.ssgman.restman.RestmanMessage.MAPPING_ERROR_TYPE_ATTRIBUTE;

/**
 * This helper holds common logic used in more than one place (i.e. SolutionKitAdminImpl, SolutionKitManagerResource).
 */
public class SolutionKitAdminHelper implements SolutionKitAdmin {
    private static final Logger logger = Logger.getLogger(SolutionKitAdminHelper.class.getName());

    private final SolutionKitManager solutionKitManager;
    private final LicenseManager licenseManager;
    private final IdentityProviderConfigManager identityProviderConfigManager;

    public SolutionKitAdminHelper(@NotNull final LicenseManager licenseManager, @NotNull final SolutionKitManager solutionKitManager, @NotNull final IdentityProviderConfigManager identityProviderConfigManager) {
        this.solutionKitManager = solutionKitManager;
        this.licenseManager = licenseManager;
        this.identityProviderConfigManager = identityProviderConfigManager;
    }

    @NotNull
    public List<SolutionKit> getSolutionKitsToUpgrade(@Nullable SolutionKit solutionKit) throws FindException {   // TODO (TL refactor) looks out of place?
        final List<SolutionKit> skList = new ArrayList<>();
        if (solutionKit == null) return skList;

        // Case 1:
        final Goid parentGoid = solutionKit.getParentGoid();
        if (parentGoid != null) {
            final SolutionKit parent = get(parentGoid);
            if (parent != null) {
                skList.add(parent);
            } else {
                throw new FindException("The parent of a child solution kit '" + solutionKit.getName() + "' does not exist.");
            }
        }

        // Case 1 + Case 2 + Case 3:
        skList.add(solutionKit);

        // Case 3:
        final Collection<SolutionKit> children = find(solutionKit.getGoid());
        for (final SolutionKit child: children) {
            skList.add(child);
        }

        return skList;
    }

    @NotNull
    public String testInstall(@NotNull final SolutionKit solutionKit, @NotNull final String bundle, final boolean isUpgrade) throws Exception {
        checkFeatureEnabled(solutionKit);
        validateSolutionKitForInstallOrUpgrade(solutionKit, isUpgrade);

        return solutionKitManager.importBundle(bundle, solutionKit, true);
    }

    private void checkFeatureEnabled(@NotNull final SolutionKit solutionKit) throws SolutionKitException {
        final String featureSet = solutionKit.getProperty(SolutionKit.SK_PROP_FEATURE_SET_KEY);
        if (!StringUtils.isEmpty(featureSet) && !licenseManager.isFeatureEnabled(featureSet)) {
            throw new ForbiddenException(solutionKit.getName() + " is unlicensed.  Required feature set " + featureSet);
        }
    }

    @NotNull
    @Override
    public JobId<String> testInstallAsync(@NotNull SolutionKit solutionKit, @NotNull String bundle, boolean isUpgrade) {
        throw new UnsupportedOperationException();
    }

    @NotNull
    public Goid install(@NotNull final SolutionKit solutionKit, @NotNull final String bundle, final boolean isUpgrade) throws Exception {
        checkFeatureEnabled(solutionKit);

        // Install bundle.
        String mappings = solutionKitManager.importBundle(bundle, solutionKit, false);

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

    @NotNull
    @Override
    public JobId<Goid> installAsync(@NotNull SolutionKit solutionKit, @NotNull String bundle, boolean isUpgrade) {
        throw new UnsupportedOperationException();
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

    @NotNull
    public String uninstall(@NotNull final Goid goid) throws Exception {
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
                String actionStr, entityTypeStr, subEntityTypeStr, srcId, targetId;
                final RestmanMessage uninstallMappingsMsg = new RestmanMessage(uninstallBundle);
                for (Element element: uninstallMappingsMsg.getMappings()) {
                    actionStr = element.getAttribute(RestmanMessage.MAPPING_ACTION_ATTRIBUTE);
                    if (Mapping.Action.Ignore == Mapping.Action.valueOf(actionStr)) continue;

                    entityTypeStr = element.getAttribute(RestmanMessage.MAPPING_TYPE_ATTRIBUTE);

                    // Retrieve a sub entity type if the entity type is identity provider.  If it is not identity provider, just simply return null.
                    if (EntityType.valueOf(entityTypeStr) == EntityType.ID_PROVIDER_CONFIG) {
                        targetId = element.getAttribute(RestmanMessage.MAPPING_TARGET_ID_ATTRIBUTE);
                        if (StringUtils.isBlank(targetId)) {
                            srcId = element.getAttribute(RestmanMessage.MAPPING_SRC_ID_ATTRIBUTE);
                            subEntityTypeStr = StringUtils.isBlank(srcId)? null : getIdentityProviderType(srcId);
                        } else {
                            subEntityTypeStr = getIdentityProviderType(targetId);
                        }
                    } else {
                        subEntityTypeStr = null;
                    }

                    if (!InstanceModifier.isModifiableType(entityTypeStr, subEntityTypeStr)) {
                        toBeIgnoredEntityTypes.add(entityTypeStr);
                    }
                }
                for (String toBeIgnored: toBeIgnoredEntityTypes) {
                    uninstallMappingsMsg.removeMappingByEntityType(toBeIgnored);
                }
                uninstallBundle = uninstallMappingsMsg.getAsString();
            }

            // Import the deletion bundle
            resultMappings = solutionKitManager.importBundle(uninstallBundle, solutionKit, isTest);
        }
        solutionKitManager.delete(solutionKit);
        return resultMappings;
    }

    private String getIdentityProviderType(final String identityProviderGoid) {
        try {
            final IdentityProviderConfig identityProviderConfig = identityProviderConfigManager.findByPrimaryKey(Goid.parseGoid(identityProviderGoid));
            if (identityProviderConfig == null) return null;

            return IdentityProviderType.fromVal(identityProviderConfig.getTypeVal()).description();
        } catch (FindException e) {
            logger.warning("Error on finding an identity provider with GOID = '" + identityProviderGoid + "'");
            return null;
        }
    }

    @NotNull
    @Override
    public JobId<String> uninstallAsync(@NotNull Goid goid) {
        throw new UnsupportedOperationException();
    }

    /**
     * Validate if a target solution kit is good for install or upgrade.
     *
     * Install: if an existing solution kit is found as same as the target solution kit, fail install.
     * This is the requirement from the note in the story SSG-10996, Upgrade Solution Kit.
     * - disable install if SK is already installed (upgrade only)
     *
     * Upgrade: if the source solution kit uses an instance modifier that other existing solution kit has been used, fail upgrade.
     *
     * @param sourceSK: the solution kit to be validated
     * @param isUpgrade: indicate if the solution kit is to upgraded or install.  True for upgrade and false for install.
     * @return true if validation is passed; Otherwise false returns.
     * @throws BadRequestException: any errors or rule violation will throw BadRequestException
     */
    public boolean validateSolutionKitForInstallOrUpgrade(@NotNull final SolutionKit sourceSK, final boolean isUpgrade) throws BadRequestException, SolutionKitConflictException {  // TODO (TL refactor) can move?
        final Goid sourceGoid = sourceSK.getGoid();
        final String sourceGuid = sourceSK.getSolutionKitGuid();

        String sourceIM = sourceSK.getProperty(SolutionKit.SK_PROP_INSTANCE_MODIFIER_KEY);
        if (StringUtils.isBlank(sourceIM)) sourceIM = "";

        final String sourceIMDisplayName = InstanceModifier.getDisplayName(sourceIM);

        // Check upgrade
        if (isUpgrade) {
            try {
                SolutionKit found = solutionKitManager.findBySolutionKitGuidAndIM(sourceGuid, sourceIM);
                if (found != null && sourceGoid != null && !sourceGoid.equals(found.getGoid())) {
                    //if the source solution kit uses an instance modifier that other existing solution kit has been used, fail upgrade.
                    throw new SolutionKitConflictException("Upgrade Failed: the solution kit '" + sourceSK.getName() + "' tired to use a same instance modifier '" + sourceIMDisplayName + "', which other existing solution kit already uses");
                }
            } catch (FindException e) {
                throw new BadRequestException(ExceptionUtils.getMessage(e));
            }

            return true;
        }

        // Check install
        final List<SolutionKit> solutionKitsOnDB;
        try {
            solutionKitsOnDB = solutionKitManager.findBySolutionKitGuid(sourceGuid);
        } catch (FindException e) {
            throw new BadRequestException(ExceptionUtils.getMessage(e));
        }

        String instanceModifier;
        for (SolutionKit solutionKit: solutionKitsOnDB) {
            instanceModifier = solutionKit.getProperty(SolutionKit.SK_PROP_INSTANCE_MODIFIER_KEY);
            if (StringUtils.isBlank(instanceModifier)) instanceModifier = "";

            if (sourceIM.equals(instanceModifier)) {
                throw new SolutionKitConflictException("Installation Failed: found one other existing solution kit matching '" + sourceSK.getName() + "' (GUID = " + sourceGuid + ", Instance Modifier = " + sourceIMDisplayName + ")");
            }
        }

        return true;
    }

    @Override
    public <OUT extends Serializable> String getJobStatus(JobId<OUT> jobId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <OUT extends Serializable> JobResult<OUT> getJobResult(JobId<OUT> jobId) throws UnknownJobException, JobStillActiveException {
        throw new UnsupportedOperationException();
    }

    @Override
    public <OUT extends Serializable> void cancelJob(JobId<OUT> jobId, boolean interruptIfRunning) {
        throw new UnsupportedOperationException();
    }

    /**
     * Create EntityOwnershipDescriptors for each newly created entity.
     */
    private void updateEntityOwnershipDescriptors(
            @NotNull final String resultMappings,
            @NotNull final SolutionKit solutionKit
    ) throws SAXException, IOException {
        // get solutionKit's current entity ownership descriptors
        // todo: consider locking the EntityOwnershipDescriptors set (or even this entire method) IF another thread is upgrading or updating the solution kit content
        // todo: shouldn't really happen though (we'd probably have bigger issues if same solution kit is installed from multiple threads)
        final Collection<EntityOwnershipDescriptor> entityOwnershipDescriptors = solutionKit.getEntityOwnershipDescriptors();
        // a bit of optimization, convert the ownership descriptors set into a map by entity-id
        final Map<String, EntityOwnershipDescriptor> ownershipDescriptorMap = new HashMap<>(entityOwnershipDescriptors != null ? entityOwnershipDescriptors.size() : 0);
        if (entityOwnershipDescriptors != null) {
            for (final EntityOwnershipDescriptor descriptor : entityOwnershipDescriptors) {
                final EntityOwnershipDescriptor oldDescriptor = ownershipDescriptorMap.put(descriptor.getEntityId(), descriptor);
                if (oldDescriptor != null) {
                    // this shouldn't happen so log for now
                    logger.log(Level.WARNING, "EntityOwnershipDescriptor: Duplicate entity id: " + descriptor.getEntityId());
                }
            }
        }

        // ArrayList is faster for add operations and List.addAll has the same performance for both ArrayList and HashSet
        final Collection<EntityOwnershipDescriptor> newOwnershipDescriptors = new ArrayList<>();
        // HashSet works faster with List.removeAll
        final Collection<EntityOwnershipDescriptor> deletedOwnershipDescriptors = new HashSet<>();
        // a hash-set of entities (entity-ids) to update readonly-ness (by default all updated and created entities are candidates)
        // this is done by decrementing version_stamp of all updated and created entities owned by other solution kits
        final Collection<String> entitiesToDecrementVersionStamp = new HashSet<>();

        // Find all matches of srdId and targetId in result mappings and save them in a map.
        final RestmanMessage mappingsMsg = new RestmanMessage(resultMappings);

        // loop through the result mappings from RESTMAN
        final Collection<Element> mappings = mappingsMsg.getMappings();
        for (final Element mapping : mappings) {
            if (mapping.hasAttribute(MAPPING_ERROR_TYPE_ATTRIBUTE)) {
                // skip if this mapping contains an error
                // highly unlikely that this would happen as any errors in the bundle would be caught before this method is even called
                // if it got this far it means that the bundle is already installed, therefore the only logical thing to do is skip this one log the error and process remaining entities
                logger.log(Level.WARNING, "Skipping mapping containing an error: errorType = '" + mapping.getAttribute(MAPPING_ERROR_TYPE_ATTRIBUTE) + "'");
                continue;
            }

            // get the resulting restman mapping action (actionTaken)
            final EntityMappingResult.MappingAction actionTaken;
            try {
                actionTaken = EntityMappingResult.MappingAction.valueOf(mapping.getAttribute(MAPPING_ACTION_TAKEN_ATTRIBUTE));
            } catch (final IllegalArgumentException e) {
                // skip if we cannot determine the actionTaken
                // highly unlikely that this would happen as any errors in the bundle would be caught before this method is even called
                // if it got this far it means that the bundle is already installed, therefore the only logical thing to do is skip this one log the error and process remaining entities
                logger.log(Level.WARNING, "Invalid Mapping actionTaken '" + mapping.getAttribute(MAPPING_ACTION_TAKEN_ATTRIBUTE) + "':" + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
                continue;
            }

            // skip the entity if its ignored by restman i.e. if actionTaken is Ignored then continue with the next one
            // RESTMAN result action is Ignored if either the author action was Ignore or the author action was Delete but the target entity could not be found to be deleted.
            if (EntityMappingResult.MappingAction.Ignored == actionTaken) {
                continue;
            }

            // get entity target-id and entity type
            final String entityId = mapping.getAttribute(RestmanMessage.MAPPING_TARGET_ID_ATTRIBUTE);
            final String entityType = mapping.getAttribute(RestmanMessage.MAPPING_TYPE_ATTRIBUTE);
            if (StringUtils.isNotBlank(entityId) && StringUtils.isNotBlank(entityType)) {
                if (EntityMappingResult.MappingAction.Deleted == actionTaken) {
                    // get the entity ownership descriptor for entityId
                    final EntityOwnershipDescriptor existing = ownershipDescriptorMap.get(entityId);
                    if (existing != null) {
                        // the solutionKit owns this entityId so add it to the deletedOwnershipDescriptors
                        deletedOwnershipDescriptors.add(existing);
                    }
                } else if (EntityMappingResult.MappingAction.CreatedNew == actionTaken) {
                    final boolean isReadOnly = RestmanMessage.isMarkedAsReadOnly(mapping);
                    // check if entity ownership descriptor already exists for this entityId
                    EntityOwnershipDescriptor descriptor = ownershipDescriptorMap.get(entityId);
                    if (descriptor != null) {
                        // edge case: found an orphan entity (update the existing one)
                        descriptor.setReadOnly(isReadOnly);
                        descriptor.setEntityType(EntityType.valueOf(entityType));
                    } else {
                        descriptor = new EntityOwnershipDescriptor(solutionKit, entityId, EntityType.valueOf(entityType), isReadOnly);
                        newOwnershipDescriptors.add(descriptor);
                    }
                    descriptor.markAsOwned();
                    // sanity check
                    assert entityId != null;
                    assert entityId.equals(descriptor.getEntityId());
                    entitiesToDecrementVersionStamp.add(descriptor.getEntityId());
                } else if (EntityMappingResult.MappingAction.UpdatedExisting == actionTaken || EntityMappingResult.MappingAction.UsedExisting == actionTaken) {
                    // this is an update
                    // check if the author is modifying entity read-only status
                    // this can only be done if the solution kit owns this entity
                    final EntityOwnershipDescriptor existing = ownershipDescriptorMap.get(entityId);
                    if (existing != null) {
                        final boolean isReadOnly = RestmanMessage.isMarkedAsReadOnly(mapping);
                        existing.setReadOnly(isReadOnly);
                        existing.markAsOwned();
                        // sanity check
                        assert entityId != null;
                        assert entityId.equals(existing.getEntityId());
                        entitiesToDecrementVersionStamp.add(existing.getEntityId());
                    }
                } else {
                    logger.log(Level.WARNING, "Unrecognized RESTMAN result action '" + actionTaken + "'. Entity (" + entityId + "," + entityType + ") ownership information cannot be processed.");
                }
            }
        }

        try {
            // edge case: decrementing version_stamp i.e. update readonly-ness for all updated and created entities
            // owned by other solution kits (as this skar now takes over readonly-ness)
            solutionKitManager.decrementEntitiesVersionStamp(Collections.unmodifiableCollection(entitiesToDecrementVersionStamp), solutionKit.getGoid());
        } catch (UpdateException e) {
            logger.log(Level.WARNING, "Failed to reset readonly-ness for created and updated entities not belonging to this skar: " + ExceptionUtils.getMessageWithCause(e), ExceptionUtils.getDebugException(e));
        }

        // update solutionKit EntityOwnershipDescriptors
        solutionKit.removeEntityOwnershipDescriptors(deletedOwnershipDescriptors);
        solutionKit.addEntityOwnershipDescriptors(newOwnershipDescriptors);
    }


    @NotNull
    public Collection<SolutionKitHeader> findHeaders() throws FindException {
        return solutionKitManager.findAllHeaders();
    }

    @NotNull
    public Collection<SolutionKitHeader> findHeaders(@NotNull final Goid parentGoid) throws FindException {
        return solutionKitManager.findAllChildrenHeadersByParentGoid(parentGoid);
    }

    @NotNull
    public Collection<SolutionKit> find(@NotNull final Goid parentGoid) throws FindException {
        return solutionKitManager.findAllChildrenByParentGoid(parentGoid);
    }

    @NotNull
    public Collection<SolutionKit> find(@NotNull String solutionKitGuid) throws FindException {
        return solutionKitManager.findBySolutionKitGuid(solutionKitGuid);
    }

    public SolutionKit get(@NotNull Goid goid) throws FindException {
        return solutionKitManager.findByPrimaryKey(goid);
    }

    @NotNull
    public Goid save(@NotNull SolutionKit solutionKit) throws SaveException {
        return solutionKitManager.save(solutionKit);
    }

    public void update(@NotNull SolutionKit solutionKit) throws UpdateException {
        solutionKitManager.update(solutionKit);
    }

    public void delete(@NotNull Goid goid) throws FindException, DeleteException {
        solutionKitManager.delete(goid);
    }
}
