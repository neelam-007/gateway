package com.l7tech.server.solutionkit;

import com.l7tech.gateway.common.LicenseManager;
import com.l7tech.gateway.common.solutionkit.*;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.server.bundling.EntityMappingInstructions;
import com.l7tech.server.bundling.EntityMappingResult;
import com.l7tech.server.policy.bundle.ssgman.restman.RestmanMessage;
import com.l7tech.server.security.signer.SignatureVerifier;
import com.l7tech.util.ExceptionUtils;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.security.SignatureException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.l7tech.server.policy.bundle.ssgman.restman.RestmanMessage.MAPPING_ACTION_ATTRIBUTE;
import static com.l7tech.server.policy.bundle.ssgman.restman.RestmanMessage.MAPPING_ACTION_TAKEN_ATTRIBUTE;

/**
 * This helper holds common logic used in more than one place (i.e. SolutionKitAdminImpl, SolutionKitManagerResource).
 */
public class SolutionKitAdminHelper {
    private static final Logger logger = Logger.getLogger(SolutionKitAdminHelper.class.getName());

    private final SolutionKitManager solutionKitManager;
    private final LicenseManager licenseManager;
    private final SignatureVerifier signatureVerifier;

    public SolutionKitAdminHelper(@NotNull final LicenseManager licenseManager, @NotNull final SolutionKitManager solutionKitManager, @NotNull final SignatureVerifier signatureVerifier) {
        this.solutionKitManager = solutionKitManager;
        this.licenseManager = licenseManager;
        this.signatureVerifier = signatureVerifier;
    }

    /**
     * Retrieve all child solution kit headers, whose parent's GOID is the same as a given parentGoid.
     *
     * @return a collection of child solution kits associated with parentGoid.
     * @throws FindException
     */
    @NotNull
    public Collection<SolutionKitHeader> findAllChildrenHeadersByParentGoid(Goid parentGoid) throws FindException {
        return solutionKitManager.findAllChildrenHeadersByParentGoid(parentGoid);
    }

    /**
     * Retrieve all child solution kits, whose parent's GOID is the same as a given parentGoid.
     *
     * @return a collection of child solution kits associated with parentGoid.
     * @throws FindException
     */
    @NotNull
    public Collection<SolutionKit> findAllChildrenByParentGoid(Goid parentGoid) throws FindException {
        return solutionKitManager.findAllChildrenByParentGoid(parentGoid);
    }

    /**
     * Retrieve solution kit entity with the given ID.
     *
     * @param goid the ID of solution kit entity
     * @return the solution kit entity
     * @throws FindException
     */
    public SolutionKit get(@NotNull Goid goid) throws FindException {
        return solutionKitManager.findByPrimaryKey(goid);
    }

    /**
     * Test installation for the given bundle.
     *
     * @param solutionKit the solution kit to test
     * @param bundle the bundle XML to test
     * @param isUpgrade indicate if the soluton kit is to be upgraded or installed.
     * @return the resulting mapping XML
     */
    @NotNull
    public String testInstall(@NotNull final SolutionKit solutionKit, @NotNull final String bundle, final boolean isUpgrade) throws Exception {
        checkFeatureEnabled(solutionKit);
        validateSolutionKitForInstallOrUpgrade(solutionKit, isUpgrade);

        return solutionKitManager.importBundle(bundle, solutionKit, true);
    }

    /**
     * Install the given solution kit.
     *
     * @param solutionKit the solution kit to install
     * @param bundle the bundle XML to install
     * @param isUpgrade true if this is a upgrade install; false for new first time install
     * @return the saved solution kit entity ID
     */
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

    /**
     * Uninstall solution kit identified by the given ID.
     *
     * @param goid the ID of solution kit entity
     * @return an empty string
     */
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
                String entityTypeStr;
                final RestmanMessage uninstallMappingsMsg = new RestmanMessage(uninstallBundle);
                for (Element element: uninstallMappingsMsg.getMappings()) {
                    entityTypeStr = element.getAttribute(RestmanMessage.MAPPING_TYPE_ATTRIBUTE);
                    if (!InstanceModifier.isModifiableType(entityTypeStr)) {
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

    /**
     * Checks signature and also verifies that signer cert is trusted.
     *
     * @param digest                 SHA-256 digest of the raw input material (i.e. Skar file raw bytes).  Required and cannot be {@code null}.
     *                               Note: this MUST NOT just be the value claimed by the sender -- it must be a freshly
     *                               computed value from hashing the information covered by the signature.
     * @param signatureProperties    Signature properties reader, holding ASN.1 encoded X.509 certificate as Base64 string
     *                               and ASN.1 encoded signature value as Base64 string.
     * @throws SignatureException if signature cannot be validated or signer cert is not trusted.
     */
    public void verifySkarSignature(@NotNull final byte[] digest, @Nullable final String signatureProperties) throws SignatureException {
        signatureVerifier.verify(digest, signatureProperties);
    }

    /**
     * Get a list of solution kits for upgrade, depending on the following three cases:
     * Case 1: if the selected solution kit is a child, then add the parent and the selected child into the return list.
     * Case 2: if the selected solution kit is a neither parent nor child, then add only the selected solution kit into the return list.
     * Case 3: if the selected solution kit is a parent, then add the parent and all children into the return list.
     *
     * @param solutionKit: the selected solution kit, which user selects to upgrade.
     * @return a list of solution kits for upgrade
     */
    @NotNull
    public List<SolutionKit> getSolutionKitsToUpgrade(@Nullable SolutionKit solutionKit) throws FindException {
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
        final Collection<SolutionKit> children = findAllChildrenByParentGoid(solutionKit.getGoid());
        for (final SolutionKit child: children) {
            skList.add(child);
        }

        return skList;
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
    public boolean validateSolutionKitForInstallOrUpgrade(@NotNull final SolutionKit sourceSK, final boolean isUpgrade) throws BadRequestException, SolutionKitConflictException {
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

    private void checkFeatureEnabled(@NotNull final SolutionKit solutionKit) throws SolutionKitException {
        final String featureSet = solutionKit.getProperty(SolutionKit.SK_PROP_FEATURE_SET_KEY);
        if (!StringUtils.isEmpty(featureSet) && !licenseManager.isFeatureEnabled(featureSet)) {
            throw new ForbiddenException(solutionKit.getName() + " is unlicensed.  Required feature set " + featureSet);
        }
    }

    /**
     * Create EntityOwnershipDescriptors for each newly created entity.
     */
    private void updateEntityOwnershipDescriptors(
            @NotNull final String resultMappings,
            @NotNull final SolutionKit solutionKit
    ) throws SAXException, IOException {
        // get solutionKit's current entity ownership descriptors (read-only)
        // todo: consider locking the EntityOwnershipDescriptors set (or even this entire method) IF another thread is upgrading or updating the solution kit content
        // todo: shouldn't really happen though (we'd probably have bigger issues if same solution kit is installed from multiple threads)
        final Set<EntityOwnershipDescriptor> entityOwnershipDescriptors = solutionKit.getEntityOwnershipDescriptors();
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

        final Set<EntityOwnershipDescriptor> newOwnershipDescriptors = new HashSet<>();
        final Set<EntityOwnershipDescriptor> deletedOwnershipDescriptors = new HashSet<>();

        // Find all matches of srdId and targetId in result mappings and save them in a map.
        final RestmanMessage mappingsMsg = new RestmanMessage(resultMappings);

        // loop through the result mappings from RESTMAN
        final Collection<Element> mappings = mappingsMsg.getMappings();
        for (final Element mapping : mappings) {
            // get the initial author mapping action
            final EntityMappingInstructions.MappingAction authorMappingAction = EntityMappingInstructions.MappingAction.valueOf(mapping.getAttribute(MAPPING_ACTION_ATTRIBUTE));
            // get the resulting restman mapping action
            final EntityMappingResult.MappingAction restmanResultMappingAction = EntityMappingResult.MappingAction.valueOf(mapping.getAttribute(MAPPING_ACTION_TAKEN_ATTRIBUTE));
            // skip the entity if its ignored by either the author or restman i.e.
            // if author action is Ignore or result action is Ignored then continue with the next one
            // RESTMAN result action is Ignored if either the author action was Ignore or the author action was Delete but the target entity could not be found to be deleted.
            if (EntityMappingInstructions.MappingAction.Ignore == authorMappingAction || EntityMappingResult.MappingAction.Ignored == restmanResultMappingAction) {
                continue;
            }

            // get entity target-id and entity type
            final String entityId = mapping.getAttribute(RestmanMessage.MAPPING_TARGET_ID_ATTRIBUTE);
            final String entityType = mapping.getAttribute(RestmanMessage.MAPPING_TYPE_ATTRIBUTE);
            if (StringUtils.isNotBlank(entityId) && StringUtils.isNotBlank(entityType)) {
                if (EntityMappingResult.MappingAction.Deleted == restmanResultMappingAction) {
                    // get the entity ownership descriptor for entityId
                    final EntityOwnershipDescriptor existing = ownershipDescriptorMap.get(entityId);
                    if (existing != null) {
                        // the solutionKit own this entityId so add it to the deletedOwnershipDescriptors
                        deletedOwnershipDescriptors.add(existing);
                    }
                } else if (EntityMappingResult.MappingAction.CreatedNew == restmanResultMappingAction) {
                    final boolean isReadOnly = RestmanMessage.isMarkedAsReadOnly(mapping);
                    final EntityOwnershipDescriptor descriptor = new EntityOwnershipDescriptor(solutionKit, entityId, EntityType.valueOf(entityType), isReadOnly);
                    newOwnershipDescriptors.add(descriptor);
                } else if (EntityMappingResult.MappingAction.UpdatedExisting == restmanResultMappingAction || EntityMappingResult.MappingAction.UsedExisting == restmanResultMappingAction) {
                    // this is an update
                    // check if the author is modifying entity read-only status
                    // this can only be done if the solution kit owns this entity
                    final EntityOwnershipDescriptor existing = ownershipDescriptorMap.get(entityId);
                    if (existing != null) {
                        final boolean isReadOnly = RestmanMessage.isMarkedAsReadOnly(mapping);
                        existing.setReadable(!isReadOnly);
                    }
                } else {
                    logger.log(Level.WARNING, "Unrecognized RESTMAN result action '" + restmanResultMappingAction + "'. Entity (" + entityId + "," + entityType + ") ownership information cannot be processed.");
                }
            }
        }

        // update solutionKit EntityOwnershipDescriptors
        solutionKit.removeEntityOwnershipDescriptors(deletedOwnershipDescriptors);
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
