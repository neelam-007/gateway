package com.l7tech.console.util;

import com.l7tech.console.tree.PaletteFolderRegistry;
import com.l7tech.gateway.common.admin.FolderAdmin;
import com.l7tech.gateway.common.admin.PolicyAdmin;
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
import com.l7tech.objectmodel.*;
import com.l7tech.objectmodel.folder.Folder;
import com.l7tech.objectmodel.folder.HasFolder;
import com.l7tech.objectmodel.folder.HasFolderGoid;
import com.l7tech.objectmodel.imp.NamedEntityImp;
import com.l7tech.objectmodel.imp.NamedGoidEntityImp;
import com.l7tech.policy.AssertionAccess;
import com.l7tech.policy.AssertionRegistry;
import com.l7tech.policy.Policy;
import com.l7tech.policy.PolicyAlias;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.AssertionMetadata;
import com.l7tech.policy.assertion.CustomAssertionHolder;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
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
    private static final String NO_PATH = "--";
    private static final String UNKNOWN_FOLDER = "unknown folder";
    private static final String ROOT = "(root)";

    private final ServiceAdmin serviceAdmin;
    private final PolicyAdmin policyAdmin;
    private final TrustedCertAdmin trustedCertAdmin;
    private final ResourceAdmin resourceAdmin;
    private final FolderAdmin folderAdmin;
    private final AssertionRegistry assertionRegistry;
    private final PaletteFolderRegistry folderRegistry;
    private final String rootFolderName;

    public EntityNameResolver(@NotNull final ServiceAdmin serviceAdmin,
                              @NotNull final PolicyAdmin policyAdmin,
                              @NotNull final TrustedCertAdmin trustedCertAdmin,
                              @NotNull final ResourceAdmin resourceAdmin,
                              @NotNull final FolderAdmin folderAdmin,
                              @NotNull final AssertionRegistry assertionRegistry,
                              @NotNull final PaletteFolderRegistry folderRegistry,
                              @NotNull final String rootFolderName) {
        this.serviceAdmin = serviceAdmin;
        this.policyAdmin = policyAdmin;
        this.trustedCertAdmin = trustedCertAdmin;
        this.resourceAdmin = resourceAdmin;
        this.folderAdmin = folderAdmin;
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
        final String nameOnHeader = header.getType() == EntityType.ASSERTION_ACCESS ? null : header.getName();
        String name = nameOnHeader == null ? StringUtils.EMPTY : nameOnHeader;
        // entity referenced by the header
        Entity entity = null;
        // entity which is related to but not referenced by the header
        Entity relatedEntity = null;
        if (header.getType() != null && (StringUtils.isBlank(name) || name.equalsIgnoreCase(header.getStrId()))) {
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
                    SsgKeyMetadata metadata = trustedCertAdmin.findKeyMetadata(header.getOid());
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
                    final ResourceEntry resourceEntry = resourceAdmin.findResourceEntryByPrimaryKey(header.getOid());
                    validateFoundEntity(header, resourceEntry);
                    entity = resourceEntry;
                    break;
                case HTTP_CONFIGURATION:
                    final HttpConfiguration httpConfig = resourceAdmin.findHttpConfigurationByPrimaryKey(header.getOid());
                    validateFoundEntity(header, httpConfig);
                    entity = httpConfig;
                    break;
                case ASSERTION_ACCESS:
                    if (header.getName() != null) {
                        final Assertion assertion = assertionRegistry.findByClassName(header.getName());
                        name = getNameForAssertion(assertion, header.getName());
                    }
                    break;
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
            if (includePath && header instanceof HasFolderGoid) {
                path = getPath((HasFolderGoid) header);
            }
            uniqueInfo = getUniqueInfo(header, relatedEntity);
        }

        return buildName(name, uniqueInfo, path);
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
            name = httpConfig.getProtocol() + " " + httpConfig.getHost() + " " + httpConfig.getPort();
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
            final String mode = predicate.getMode();
            final String attribute = predicate.getAttribute();
            final String value = predicate.getValue();
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
                name = "not in any security zone";
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
        } else if (entity instanceof NamedGoidEntityImp) {
            final NamedGoidEntityImp named = (NamedGoidEntityImp) entity;
            name = named.getName();
        }
        String path = null;
        if (includePath && entity instanceof HasFolder) {
            path = getPath((HasFolder) entity);
        }
        String uniqueInfo = getUniqueInfo(entity);
        if (StringUtils.isBlank(uniqueInfo) && relatedEntity instanceof Entity) {
            uniqueInfo = getUniqueInfo((Entity) relatedEntity);
        }

        return buildName(name, uniqueInfo, path);
    }

    /**
     * Get the folder path for a HasFolderGoid which can have a reference to a Folder.
     * <p/>
     * The path will always start with '/' to represent the root folder (even if the given HasFolderGoid does not reference any Folder).
     *
     * @param hasFolder the HasFolderGoid which can have a reference to a Folder (most likely a header).
     * @return the folder path for the HasFolderGoid or '/' if no Folder is referenced.
     * @throws FindException if there is an error looking up the Folder referenced by the HasFolderGoid.
     * @throws com.l7tech.gateway.common.security.rbac.PermissionDeniedException
     *                       if the user does not have permission to access an entity required to resolve the path
     */
    @NotNull
    public String getPath(@NotNull final HasFolderGoid hasFolder) throws FindException {
        final String path;
        if (hasFolder instanceof EntityHeader && isRootFolder((EntityHeader) hasFolder)) {
            path = ROOT;
        } else {
            Folder folder = null;
            if (hasFolder.getFolderGoid() != null) {
                folder = folderAdmin.findByPrimaryKey(hasFolder.getFolderGoid());
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
        if (assertion instanceof CustomAssertionHolder) {
            folderNames.add(NO_PATH);
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
            name = String.valueOf(assertion.meta().get(AssertionMetadata.SHORT_NAME));
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

    private void validateFoundEntity(final EntityHeader header, final Entity foundEntity) throws FindException {
        validateFoundEntity(header.getType(), header.getOid(), foundEntity);
    }

    private void validateFoundEntity(final EntityType type, final long oid, final Entity foundEntity) throws FindException {
        if (foundEntity == null) {
            throw new FindException("No entity found for type " + type + " and oid " + oid);
        }
    }

    private void validateFoundEntity(final EntityType type, final Goid goid, final Entity foundEntity) throws FindException {
        if (foundEntity == null) {
            throw new FindException("No entity found for type " + type + " and goid " + goid);
        }
    }

    private String buildName(String name, String uniqueInfo, String path) {
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
                stringBuilder.append(name);
                stringBuilder.append(")");
            } else {
                stringBuilder.append(" " + path);
            }
        }
        return stringBuilder.toString();
    }
}
