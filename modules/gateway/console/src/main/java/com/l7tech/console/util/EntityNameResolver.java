package com.l7tech.console.util;

import com.l7tech.console.tree.PaletteFolderRegistry;
import com.l7tech.gateway.common.admin.FolderAdmin;
import com.l7tech.gateway.common.admin.PolicyAdmin;
import com.l7tech.gateway.common.cluster.ClusterStatusAdmin;
import com.l7tech.gateway.common.esmtrust.TrustedEsm;
import com.l7tech.gateway.common.esmtrust.TrustedEsmUser;
import com.l7tech.gateway.common.resources.HttpConfiguration;
import com.l7tech.gateway.common.resources.ResourceAdmin;
import com.l7tech.gateway.common.resources.ResourceEntry;
import com.l7tech.gateway.common.security.TrustedCertAdmin;
import com.l7tech.gateway.common.security.keystore.SsgKeyMetadata;
import com.l7tech.gateway.common.security.rbac.*;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.gateway.common.service.PublishedServiceAlias;
import com.l7tech.gateway.common.service.ServiceAdmin;
import com.l7tech.gateway.common.service.ServiceHeader;
import com.l7tech.gateway.common.uddi.UDDIProxiedServiceInfoHeader;
import com.l7tech.gateway.common.uddi.UDDIServiceControlHeader;
import com.l7tech.objectmodel.*;
import com.l7tech.objectmodel.folder.Folder;
import com.l7tech.objectmodel.folder.HasFolder;
import com.l7tech.objectmodel.folder.HasFolderId;
import com.l7tech.objectmodel.imp.NamedEntityImp;
import com.l7tech.policy.AssertionAccess;
import com.l7tech.policy.AssertionRegistry;
import com.l7tech.policy.Policy;
import com.l7tech.policy.PolicyAlias;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.AssertionMetadata;
import com.l7tech.policy.assertion.CustomAssertionHolder;
import com.l7tech.policy.assertion.EncapsulatedAssertion;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Utility class for resolving names of entities.
 */
public class EntityNameResolver {
    private static final Logger logger = Logger.getLogger(EntityNameResolver.class.getName());
    private static final String PATH_SEPARATOR = "/";
    private static final String ELLIPSIS = "...";
    private static final int MAX_FOLDER_PATH = 4;
    // if an entity only has a folder path of length = 1, then it is in the root folder
    private static final int MIN_FOLDER_PATH = 1;
    private static final int FIRST_FOLDER_INDEX = 0;
    private static final int SECOND_LAST_FOLDER_INDEX = 3;
    private static final String MULTIPLE_PATH = "<multiple>";
    private static final String UNKNOWN_FOLDER = "unknown folder";
    private static final String ROOT = "(root)";
    private static final String NO_PROTOCOL = "<no protocol>";
    private static final String NO_PORT = "<no port>";
    private static final String NO_HTTP_CONFIG_PATH = "<no path>";
    private static final Set<EntityType> IGNORE_HEADER_NAMES;

    private final ServiceAdmin serviceAdmin;
    private final PolicyAdmin policyAdmin;
    private final TrustedCertAdmin trustedCertAdmin;
    private final ResourceAdmin resourceAdmin;
    private final FolderAdmin folderAdmin;
    private final ClusterStatusAdmin clusterStatusAdmin;
    private final AssertionRegistry assertionRegistry;
    private final PaletteFolderRegistry folderRegistry;
    private final String rootFolderName;

    static {
        IGNORE_HEADER_NAMES = new HashSet<>();
        IGNORE_HEADER_NAMES.add(EntityType.ASSERTION_ACCESS);
        IGNORE_HEADER_NAMES.add(EntityType.TRUSTED_ESM);
        IGNORE_HEADER_NAMES.add(EntityType.TRUSTED_ESM_USER);
    }

    public EntityNameResolver(@NotNull final ServiceAdmin serviceAdmin,
                              @NotNull final PolicyAdmin policyAdmin,
                              @NotNull final TrustedCertAdmin trustedCertAdmin,
                              @NotNull final ResourceAdmin resourceAdmin,
                              @NotNull final FolderAdmin folderAdmin,
                              @NotNull final ClusterStatusAdmin clusterStatusAdmin,
                              @NotNull final AssertionRegistry assertionRegistry,
                              @NotNull final PaletteFolderRegistry folderRegistry,
                              @NotNull final String rootFolderName) {
        this.serviceAdmin = serviceAdmin;
        this.policyAdmin = policyAdmin;
        this.trustedCertAdmin = trustedCertAdmin;
        this.resourceAdmin = resourceAdmin;
        this.folderAdmin = folderAdmin;
        this.clusterStatusAdmin = clusterStatusAdmin;
        this.assertionRegistry = assertionRegistry;
        this.folderRegistry = folderRegistry;
        this.rootFolderName = rootFolderName;
    }

    /**
     * Resolves a descriptive name for a given EntityHeader which may include a name and/or folder path and/or other unique info depending on the header type.
     * <p/>
     * If there is a non-empty, non-oid name on the header, it will take precedence (this should happen in most cases) unless it is for an AssertionAccess entity.
     * <p/>
     * Otherwise the descriptive name will be resolved by looking up the entity that is referenced by the header (this is usually the case is for entities that don't have a name).
     *
     * @param header      the EntityHeader for which to determine a descriptive name. Usually a header that has just been retrieved via an Admin call.
     * @param includePath true if entities in folders should have their folder path included in the descriptive name.
     * @return a name for the given EntityHeader which may include a name and/or folder path and/or other unique info. Cannot be null.
     *         Can be empty if the name on the header is empty/null and the resolver does not know how to look up the referenced entity.
     * @throws FindException if a db error occurs or the entity referenced by the header does not exist.
     * @throws com.l7tech.gateway.common.security.rbac.PermissionDeniedException
     *                       if the user does not have permission to access an entity required to resolve the name.
     */
    @NotNull
    public String getNameForHeader(@NotNull final EntityHeader header, final boolean includePath) throws FindException {
        final String nameOnHeader = IGNORE_HEADER_NAMES.contains(header.getType()) ? null : header.getName();
        String name = nameOnHeader == null ? StringUtils.EMPTY : nameOnHeader;
        // entity referenced by the header
        Entity entity = null;
        // entity which is related to but not referenced by the header
        Entity relatedEntity = null;
        if (header.getType() != null && (StringUtils.isBlank(name))) {
            switch (header.getType()) {
                case POLICY:
                    final Policy policy = policyAdmin.findPolicyByPrimaryKey(header.getGoid());
                    validateFoundEntity(header, policy);
                    name = getNameForEntity(policy, includePath);
                    break;
                case SERVICE:
                    final PublishedService service = serviceAdmin.findServiceByID(header.getStrId());
                    validateFoundEntity(header, service);
                    name = getNameForEntity(service, includePath);
                    break;
                case FOLDER:
                    final Folder folder = folderAdmin.findByPrimaryKey(header.getGoid());
                    validateFoundEntity(header, folder);
                    name = getNameForEntity(folder, includePath);
                    break;
                case SERVICE_ALIAS:
                    final PublishedService owningService = serviceAdmin.findByAlias(header.getGoid());
                    validateFoundEntity(header, owningService);
                    name = owningService.getName() + " alias";
                    relatedEntity = owningService;
                    break;
                case POLICY_ALIAS:
                    final Policy owningPolicy = policyAdmin.findByAlias(header.getGoid());
                    validateFoundEntity(header, owningPolicy);
                    name = owningPolicy.getName() + " alias";
                    relatedEntity = owningPolicy;
                    break;
                case SSG_KEY_METADATA:
                    SsgKeyMetadata metadata = trustedCertAdmin.findKeyMetadata(header.getGoid());
                    if (metadata == null) {
                        // may not have been persisted yet
                        if (header instanceof KeyMetadataHeaderWrapper) {
                            final KeyMetadataHeaderWrapper keyHeader = (KeyMetadataHeaderWrapper) header;
                            metadata = new SsgKeyMetadata(keyHeader.getKeystoreOid(), keyHeader.getAlias(), null);
                        }
                    }
                    validateFoundEntity(header, metadata);
                    entity = metadata;
                    break;
                case RESOURCE_ENTRY:
                    final ResourceEntry resourceEntry = resourceAdmin.findResourceEntryByPrimaryKey(header.getGoid());
                    validateFoundEntity(header, resourceEntry);
                    entity = resourceEntry;
                    break;
                case HTTP_CONFIGURATION:
                    final HttpConfiguration httpConfig = resourceAdmin.findHttpConfigurationByPrimaryKey(header.getGoid());
                    validateFoundEntity(header, httpConfig);
                    entity = httpConfig;
                    break;
                case ASSERTION_ACCESS:
                    if (header.getName() != null) {
                        final Assertion assertion = assertionRegistry.findByClassName(header.getName());
                        name = getNameForAssertion(assertion, header.getName());
                    }
                    break;
                case SERVICE_USAGE:
                    if (header instanceof ServiceUsageHeader) {
                        final ServiceUsageHeader usageHeader = (ServiceUsageHeader) header;
                        final PublishedService usageService = serviceAdmin.findServiceByID(usageHeader.getServiceGoid().toHexString());
                        validateFoundEntity(EntityType.SERVICE, usageHeader.getServiceGoid(), usageService);
                        name = getNameForEntity(usageService, includePath) + " on node " + usageHeader.getNodeId();
                    }
                    break;
                case UDDI_SERVICE_CONTROL:
                    if (header instanceof UDDIServiceControlHeader) {
                        final UDDIServiceControlHeader uddiControlHeader = (UDDIServiceControlHeader) header;
                        if (uddiControlHeader.getPublishedServiceGoid() != null) {
                            final PublishedService controlService = serviceAdmin.findServiceByID(uddiControlHeader.getPublishedServiceGoid().toHexString());
                            validateFoundEntity(EntityType.SERVICE, uddiControlHeader.getPublishedServiceGoid(), controlService);
                            name = getNameForEntity(controlService, includePath);
                        }
                    }
                    break;
                case UDDI_PROXIED_SERVICE_INFO:
                    if (header instanceof UDDIProxiedServiceInfoHeader) {
                        final UDDIProxiedServiceInfoHeader proxiedServiceInfoHeader = (UDDIProxiedServiceInfoHeader) header;
                        if (proxiedServiceInfoHeader.getPublishedServiceGoid() != null) {
                            final PublishedService proxiedService = serviceAdmin.findServiceByID(proxiedServiceInfoHeader.getPublishedServiceGoid().toHexString());
                            validateFoundEntity(EntityType.SERVICE, proxiedServiceInfoHeader.getPublishedServiceGoid(), proxiedService);
                            name = getNameForEntity(proxiedService, includePath);
                        }
                    }
                    break;
                case TRUSTED_ESM:
                    final TrustedEsm trustedEsm = clusterStatusAdmin.findTrustedEsm(header.getGoid());
                    validateFoundEntity(header, trustedEsm);
                    entity = trustedEsm;
                    break;
                case TRUSTED_ESM_USER:
                    final TrustedEsmUser trustedEsmUser = clusterStatusAdmin.findTrustedEsmUser(header.getGoid());
                    validateFoundEntity(header, trustedEsmUser);
                    entity = trustedEsmUser;
                default:
                    logger.log(Level.WARNING, "Name on header is null or empty but entity type is not supported: " + header.getType());
                    name = StringUtils.EMPTY;
            }
        }

        if (isRootFolder(header)) {
            name = rootFolderName;
        }

        String path = null;
        String uniqueInfo = null;
        if (StringUtils.isBlank(name) && entity != null) {
            // get the name and/or path using the full entity
            name = getNameForEntity(entity, includePath);
        } else if (entity == null) {
            if (includePath) {
                if (header instanceof HasFolderId) {
                    path = getPath((HasFolderId) header);
                } else if (header.getType() == EntityType.ASSERTION_ACCESS) {
                    final Assertion assertion = assertionRegistry.findByClassName(header.getName());
                    if (assertion != null) {
                        path = getPaletteFolders(assertion);
                    }
                }
            }
            uniqueInfo = getUniqueInfo(header, relatedEntity);
        }

        return buildName(name, uniqueInfo, path, header.getType() != EntityType.ASSERTION_ACCESS);
    }

    /**
     * Resolves a descriptive name for a given EntityHeader which may include a name and/or folder path and/or other unique info depending on the header type.
     * <p/>
     * Will include the path if it is available.
     *
     * @see #getNameForHeader(com.l7tech.objectmodel.EntityHeader, boolean)
     */
    @NotNull
    public String getNameForHeader(@NotNull final EntityHeader header) throws FindException {
        return getNameForHeader(header, true);
    }

    /**
     * Resolves a descriptive name for a given Entity which may include a name and/or folder path and/or other unique info depending on the Entity.
     *
     * @param entity      the Entity for which to determine a descriptive name.
     * @param includePath true if entities in folders should have their folder path included in the descriptive name.
     * @return a descriptive name for a given Entity which may include a name and/or folder path and/or other unique info depending on the Entity.
     * @throws FindException if a db error occurs when retrieving information needed to resolve a name for the entity.
     * @throws com.l7tech.gateway.common.security.rbac.PermissionDeniedException
     *                       if the user does not have permission to access an entity required to resolve the name.
     */
    @NotNull
    public String getNameForEntity(@NotNull final Entity entity, final boolean includePath) throws FindException {
        String name = StringUtils.EMPTY;
        Object relatedEntity = null;
        if (entity instanceof PublishedServiceAlias) {
            final PublishedServiceAlias alias = (PublishedServiceAlias) entity;
            final PublishedService owningService = serviceAdmin.findServiceByID(String.valueOf(alias.getEntityGoid()));
            validateFoundEntity(EntityType.SERVICE, alias.getGoid(), owningService);
            name = owningService.getName() + " alias";
            relatedEntity = owningService;
        } else if (entity instanceof PolicyAlias) {
            final PolicyAlias alias = (PolicyAlias) entity;
            final Policy owningPolicy = policyAdmin.findPolicyByPrimaryKey(alias.getEntityGoid());
            validateFoundEntity(EntityType.POLICY, alias.getGoid(), owningPolicy);
            name = owningPolicy.getName() + " alias";
            relatedEntity = owningPolicy;
        } else if (entity instanceof SsgKeyMetadata) {
            final SsgKeyMetadata metadata = (SsgKeyMetadata) entity;
            name = metadata.getAlias();
        } else if (entity instanceof ResourceEntry) {
            final ResourceEntry resource = (ResourceEntry) entity;
            name = resource.getUri();
        } else if (entity instanceof HttpConfiguration) {
            final HttpConfiguration httpConfig = (HttpConfiguration) entity;
            final String protocol = httpConfig.getProtocol() == null ? NO_PROTOCOL : httpConfig.getProtocol().toString();
            final String port = httpConfig.getPort() == 0 ? NO_PORT : String.valueOf(httpConfig.getPort());
            final String path = httpConfig.getPath() == null ? NO_HTTP_CONFIG_PATH : httpConfig.getPath();
            name = protocol + " " + httpConfig.getHost() + " " + port + " " + path;
        } else if (entity instanceof AssertionAccess) {
            final AssertionAccess assertionAccess = (AssertionAccess) entity;
            final Assertion assertion = assertionRegistry.findByClassName(assertionAccess.getName());
            name = getNameForAssertion(assertion, assertionAccess.getName());
            relatedEntity = assertion;
        } else if (entity instanceof Role) {
            final Role role = (Role) entity;
            name = role.getDescriptiveName();
            final Entity roleEntity = role.getCachedSpecificEntity();
            if (includePath && roleEntity instanceof HasFolder && roleEntity instanceof NamedEntity) {
                name = name + " (" + getPath((HasFolder) roleEntity) + ((NamedEntity) roleEntity).getName() + ")";
            }
        } else if (entity instanceof Folder && isRootFolder((Folder) entity)) {
            name = rootFolderName;
        } else if (entity instanceof TrustedEsm) {
            final TrustedEsm trustedEsm = (TrustedEsm) entity;
            name = trustedEsm.getTrustedCert() != null ? trustedEsm.getTrustedCert().getSubjectDn() : trustedEsm.getName();
        } else if (entity instanceof TrustedEsmUser) {
            final TrustedEsmUser trustedEsmUser = (TrustedEsmUser) entity;
            name = trustedEsmUser.getEsmUserDisplayName() != null ? trustedEsmUser.getEsmUserDisplayName() : trustedEsmUser.getSsgUserId();
        } else if (entity instanceof ObjectIdentityPredicate) {
            final ObjectIdentityPredicate predicate = (ObjectIdentityPredicate) entity;
            final EntityHeader header = predicate.getHeader();
            if (header != null) {
                name = header.getType().getName() + " \"" + getNameForHeader(header, includePath) + "\"";
            } else if (predicate.getPermission() != null && predicate.getPermission().getEntityType() != null) {
                name = predicate.getPermission().getEntityType().getName() + " " + predicate.getTargetEntityId();
            } else {
                name = predicate.getTargetEntityId();
            }
        } else if (entity instanceof AttributePredicate) {
            final AttributePredicate predicate = (AttributePredicate) entity;
            final Permission permission = predicate.getPermission();
            final String mode = predicate.getMode();
            String attribute = getDisplayNameForAttribute(predicate, permission);
            String value = getDisplayNameForValue(predicate, permission);
            if (mode == null || AttributePredicate.EQUALS.equalsIgnoreCase(mode) || AttributePredicate.STARTS_WITH.equalsIgnoreCase(mode)) {
                final String operation = AttributePredicate.STARTS_WITH.equalsIgnoreCase(mode) ? "starts with" : "equals";
                name = attribute + " " + operation + " " + value;
            } else {
                // unknown mode
                name = "attribute=" + attribute + " mode=" + mode + " value=" + value;
            }
        } else if (entity instanceof SecurityZonePredicate) {
            final SecurityZonePredicate predicate = (SecurityZonePredicate) entity;
            if (predicate.getRequiredZone() != null) {
                name = "in security zone \"" + predicate.getRequiredZone().getName() + "\"";
            } else {
                name = "without a security zone";
            }
        } else if (entity instanceof FolderPredicate) {
            final FolderPredicate predicate = (FolderPredicate) entity;
            name = "in folder \"" + getNameForEntity(predicate.getFolder(), includePath) + "\"";
            if (predicate.isTransitive()) {
                name = name + " and subfolders";
            }
        } else if (entity instanceof EntityFolderAncestryPredicate) {
            final EntityFolderAncestryPredicate predicate = (EntityFolderAncestryPredicate) entity;
            if (predicate.getEntityType() != null && predicate.getEntityId() != null) {
                final EntityHeader header = new EntityHeader(predicate.getEntityId(), predicate.getEntityType(), null, null);
                final String entityName = "\"" + getNameForHeader(header, includePath) + "\"";
                name = "ancestors of " + predicate.getEntityType().getName().toLowerCase() + " " + entityName;
            } else {
                logger.log(Level.WARNING, "Unable to determine name for EntityFolderAncestryPredicate because it is missing entity type and/or entity id.");
            }
        } else if (entity instanceof NamedEntityImp) {
            final NamedEntityImp named = (NamedEntityImp) entity;
            name = named.getName();
        }
        String path = null;
        if (includePath) {
            if (entity instanceof HasFolder) {
                path = getPath((HasFolder) entity);
            } else if (entity instanceof AssertionAccess) {
                final Assertion assertion = assertionRegistry.findByClassName(((AssertionAccess) entity).getName());
                if (assertion != null) {
                    path = getPaletteFolders(assertion);
                }
            }
        }
        String uniqueInfo = getUniqueInfo(entity);
        if (StringUtils.isBlank(uniqueInfo) && relatedEntity instanceof Entity) {
            uniqueInfo = getUniqueInfo((Entity) relatedEntity);
        }

        return buildName(name, uniqueInfo, path, !(entity instanceof AssertionAccess));
    }

    /**
     * Get the folder path for a HasFolderId which can have a reference to a Folder.
     * <p/>
     * The path will always start with '/' to represent the root folder (even if the given HasFolderId does not reference any Folder).
     *
     * @param hasFolder the HasFolderId which can have a reference to a Folder (most likely a header).
     * @return the folder path for the HasFolderId or '/' if no Folder is referenced.
     * @throws FindException if there is an error looking up the Folder referenced by the HasFolderId.
     * @throws com.l7tech.gateway.common.security.rbac.PermissionDeniedException
     *                       if the user does not have permission to access an entity required to resolve the path
     */
    @NotNull
    public String getPath(@NotNull final HasFolderId hasFolder) throws FindException {
        final String path;
        if (hasFolder instanceof EntityHeader && isRootFolder((EntityHeader) hasFolder)) {
            path = ROOT;
        } else {
            Folder folder = null;
            if (hasFolder.getFolderId() != null) {
                folder = folderAdmin.findByPrimaryKey(hasFolder.getFolderId());
            }
            path = getPathForFolder(folder);
        }
        return path;
    }

    /**
     * Retrieve a comma-separated list of palette folders that the assertion belongs to.
     *
     * @param assertion the Assertion for which to retrieve its palette folders.
     * @return a comma-separated list of palette folders that the assertion belongs to.
     */
    @NotNull
    public String getPaletteFolders(@NotNull final Assertion assertion) {
        final List<String> folderNames = new ArrayList<>();
        if (assertion instanceof CustomAssertionHolder || assertion instanceof EncapsulatedAssertion) {
            folderNames.add(MULTIPLE_PATH);
        } else {
            final Object paletteFolders = assertion.meta().get(AssertionMetadata.PALETTE_FOLDERS);
            if (paletteFolders instanceof String[]) {
                final String[] folderIds = (String[]) paletteFolders;
                for (int i = 0; i < folderIds.length; i++) {
                    final String folderId = folderIds[i];
                    final String folderName = folderRegistry.getPaletteFolderName(folderId);
                    if (folderName != null) {
                        folderNames.add(folderName);
                    } else {
                        folderNames.add(UNKNOWN_FOLDER);
                    }
                }
            }
        }
        return StringUtils.join(folderNames, ",");
    }

    /**
     * Get the folder path for a HasFolder entity.
     * <p/>
     * The path will always start with '/' to represent the root folder (even if the given HasFolder entity does not have a Folder).
     *
     * @param hasFolder the HasFolder entity for which to retrieve a folder path.
     * @return the folder path for the HasFolder entity of '/' if there is no Folder.
     */
    @NotNull
    public String getPath(@NotNull final HasFolder hasFolder) {
        final String path;
        if (hasFolder instanceof Folder && isRootFolder((Folder) hasFolder)) {
            path = ROOT;
        } else {
            path = getPathForFolder(hasFolder.getFolder());
        }
        return path;
    }

    private boolean isRootFolder(@NotNull final EntityHeader header) {
        return EntityType.FOLDER == header.getType() && Folder.ROOT_FOLDER_ID.equals(header.getGoid());
    }

    private boolean isRootFolder(@NotNull final Folder folder) {
        return Folder.ROOT_FOLDER_ID.equals(folder.getGoid());
    }

    private String getNameForAssertion(final Assertion assertion, final String defaultName) {
        final String name;
        if (assertion instanceof CustomAssertionHolder) {
            name = CustomAssertionHolder.CUSTOM_ASSERTION;
        } else if (assertion != null) {
            name = String.valueOf((Object) assertion.meta().get(AssertionMetadata.PALETTE_NODE_NAME));
        } else {
            name = defaultName;
        }
        return name;
    }

    /**
     * Some entity types may require other info than its name and/or path to make it unique from others.
     */
    private String getUniqueInfo(@NotNull final EntityHeader header, @Nullable final Entity retrievedEntity) {
        String extraInfo = StringUtils.EMPTY;
        if (header.getType() != null) {
            switch (header.getType()) {
                case SERVICE:
                    if (header instanceof ServiceHeader) {
                        final ServiceHeader serviceHeader = (ServiceHeader) header;
                        if (serviceHeader.getRoutingUri() != null) {
                            extraInfo = serviceHeader.getRoutingUri();
                        }
                    }
                    break;
                case SERVICE_ALIAS:
                    if (retrievedEntity != null) {
                        extraInfo = getUniqueInfo(retrievedEntity);
                    }
                    break;
                default:
                    extraInfo = StringUtils.EMPTY;
            }
        }
        return extraInfo;
    }

    private String getUniqueInfo(@NotNull final Entity entity) {
        String extraInfo = StringUtils.EMPTY;
        if (entity instanceof PublishedService) {
            final PublishedService service = (PublishedService) entity;
            if (service.getRoutingUri() != null) {
                extraInfo = service.getRoutingUri();
            }
        }
        return extraInfo;
    }

    @NotNull
    private String getPathForFolder(@Nullable final Folder folder) {
        Folder f = folder;
        final LinkedList<String> folderPath = new LinkedList<>();
        while (null != f) {
            folderPath.add(f.getName());
            f = f.getFolder();
        }

        // root folder is represented by '/'
        final StringBuilder path = new StringBuilder(PATH_SEPARATOR);

        if (folderPath.size() > MIN_FOLDER_PATH && folderPath.size() <= MAX_FOLDER_PATH) {
            for (int i = folderPath.size() - 2; i >= 0; i--) {
                path.append(folderPath.get(i));
                path.append(PATH_SEPARATOR);
            }
        } else if (folderPath.size() > MAX_FOLDER_PATH) {
            path.append(folderPath.get(SECOND_LAST_FOLDER_INDEX));
            path.append(PATH_SEPARATOR).append(ELLIPSIS).append(PATH_SEPARATOR);
            path.append(folderPath.get(FIRST_FOLDER_INDEX));
            path.append(PATH_SEPARATOR);
        }

        return path.toString();
    }

    private String getDisplayNameForValue(final AttributePredicate predicate, final Permission permission) {
        String value = predicate.getValue();
        if (permission != null &&
                permission.getEntityType() == EntityType.ASSERTION_ACCESS &&
                (predicate.getMode() == null || AttributePredicate.EQUALS.equalsIgnoreCase(predicate.getMode()))) {
            // we don't want to show the full class name
            final Assertion assertion = assertionRegistry.findByClassName(predicate.getValue());
            if (assertion != null) {
                value = getNameForAssertion(assertion, predicate.getValue());
            }
        }
        return value;
    }

    private String getDisplayNameForAttribute(final AttributePredicate predicate, final Permission permission) {
        String attribute = predicate.getAttribute();
        if (permission != null && permission.getEntityType() != null) {
            final Map<String, String> availableAttributes = RbacAttributeCollector.collectAttributes(permission.getEntityType());
            if (availableAttributes.containsKey(attribute)) {
                attribute = availableAttributes.get(attribute);
            }
        }
        return attribute;
    }

    private void validateFoundEntity(final EntityHeader header, final Entity foundEntity) throws FindException {
        validateFoundEntity(header.getType(), header.getGoid(), foundEntity);
    }

    private void validateFoundEntity(final EntityType type, final Goid goid, final Entity foundEntity) throws FindException {
        if (foundEntity == null) {
            throw new FindException("No entity found for type " + type + " and goid " + goid);
        }
    }

    private String buildName(String name, String uniqueInfo, String path, boolean includeNameInPath) {
        final StringBuilder stringBuilder = new StringBuilder(name);
        if (StringUtils.isNotBlank(uniqueInfo)) {
            stringBuilder.append(" [");
            stringBuilder.append(uniqueInfo);
            stringBuilder.append("]");
        }
        if (StringUtils.isNotBlank(path)) {
            if (!ROOT.equals(path)) {
                stringBuilder.append(" (");
                stringBuilder.append(path);
                if (includeNameInPath) {
                    stringBuilder.append(name);
                }
                stringBuilder.append(")");
            } else {
                stringBuilder.append(" " + path);
            }
        }
        return stringBuilder.toString();
    }
}
