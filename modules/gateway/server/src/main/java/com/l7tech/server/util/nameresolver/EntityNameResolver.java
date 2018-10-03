package com.l7tech.server.util.nameresolver;

import com.l7tech.gateway.common.admin.FolderAdmin;
import com.l7tech.gateway.common.security.rbac.EntityFolderAncestryPredicate;
import com.l7tech.gateway.common.security.rbac.ObjectIdentityPredicate;
import com.l7tech.gateway.common.security.rbac.PermissionDeniedException;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.gateway.common.service.ServiceAdmin;
import com.l7tech.gateway.common.service.ServiceHeader;
import com.l7tech.objectmodel.*;
import com.l7tech.objectmodel.folder.Folder;
import com.l7tech.objectmodel.folder.HasFolder;
import com.l7tech.objectmodel.folder.HasFolderId;
import com.l7tech.objectmodel.imp.NamedEntityImp;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.AssertionMetadata;
import com.l7tech.policy.assertion.CustomAssertionHolder;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Utility class for resolving names of entities.
 */
public class EntityNameResolver {
    private static final Logger LOGGER = Logger.getLogger(EntityNameResolver.class.getName());
    private static final String PATH_SEPARATOR = "/";
    private static final String ELLIPSIS = "...";
    private static final int MAX_FOLDER_PATH = 4;
    // if an entity only has a folder path of length = 1, then it is in the root folder
    private static final int MIN_FOLDER_PATH = 1;
    private static final int FIRST_FOLDER_INDEX = 0;
    private static final int SECOND_LAST_FOLDER_INDEX = 3;
    private static final String ROOT = "(root)";
    private static final Set<EntityType> IGNORE_HEADER_NAMES;
    protected final FolderAdmin folderAdmin;
    private static final String ROOT_FOLDER_PATTERN = "${rootFolderName}";
    private List<EntityNameResolver> resolverList;
    private ServiceAdmin serviceAdmin;

    public EntityNameResolver(final FolderAdmin folderAdmin) {
        this.folderAdmin = folderAdmin;
    }

    public void setResolverList(List<EntityNameResolver> resolverList) {
        this.resolverList = resolverList;
    }

    public void setServiceAdmin(ServiceAdmin serviceAdmin) {
        this.serviceAdmin = serviceAdmin;
    }

    static {
        IGNORE_HEADER_NAMES = new HashSet<>();
        IGNORE_HEADER_NAMES.add(EntityType.ASSERTION_ACCESS);
        IGNORE_HEADER_NAMES.add(EntityType.TRUSTED_ESM);
        IGNORE_HEADER_NAMES.add(EntityType.TRUSTED_ESM_USER);
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
     * Can be empty if the name on the header is empty/null and the resolver does not know how to look up the referenced entity.
     * @throws FindException             if a db error occurs or the entity referenced by the header does not exist.
     * @throws PermissionDeniedException if the user does not have permission to access an entity required to resolve the name.
     */
    public String getNameForHeader(final EntityHeader header, final boolean includePath) throws FindException {
        final String nameOnHeader = IGNORE_HEADER_NAMES.contains(header.getType()) ? null : header.getName();
        String name = nameOnHeader == null ? StringUtils.EMPTY : nameOnHeader;
        // entity which is related to but not referenced by the header
        if (header.getType() != null && (StringUtils.isBlank(name))) {
            EntityNameResolver nameResolver = getNameResolver(header);
            if (nameResolver != null) {
                return nameResolver.resolve(header, includePath);
            }
            return StringUtils.EMPTY;
        } else {
            if (isRootFolder(header)) {
                name = ROOT_FOLDER_PATTERN;
            }
            String path = null;
            String uniqueInfo = getUniqueInfo(header, null);
            if (includePath && header instanceof HasFolderId) {
                path = getPath((HasFolderId) header);
            }
            return buildName(name, uniqueInfo, path, true);
        }
    }

    private EntityNameResolver getNameResolver(final EntityHeader header) {
        for (EntityNameResolver nameResolver : resolverList) {
            if (nameResolver.canResolveName(header)) {
                return nameResolver;
            }
        }
        return null;
    }

    private EntityNameResolver getNameResolver(final Entity entity) {
        for (EntityNameResolver nameResolver : resolverList) {
            if (nameResolver.canResolveName(entity)) {
                return nameResolver;
            }
        }
        return null;
    }


    /**
     * Resolves a descriptive name for a given Entity which may include a name and/or folder path and/or other unique info depending on the Entity.
     *
     * @param entity      the Entity for which to determine a descriptive name.
     * @param includePath true if entities in folders should have their folder path included in the descriptive name.
     * @return a descriptive name for a given Entity which may include a name and/or folder path and/or other unique info depending on the Entity.
     * @throws FindException             if a db error occurs when retrieving information needed to resolve a name for the entity.
     * @throws PermissionDeniedException if the user does not have permission to access an entity required to resolve the name.
     */

    public String getNameForEntity(final Entity entity, final boolean includePath) throws FindException {
        String name = StringUtils.EMPTY;
        EntityNameResolver nameResolver = getNameResolver(entity);
        if (nameResolver != null) {
            return nameResolver.resolve(entity, includePath);
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
        } else if (entity instanceof EntityFolderAncestryPredicate) {
            final EntityFolderAncestryPredicate predicate = (EntityFolderAncestryPredicate) entity;
            if (predicate.getEntityType() != null && predicate.getEntityId() != null) {
                final EntityHeader header = new EntityHeader(predicate.getEntityId(), predicate.getEntityType(), null, null);
                final String entityName = "\"" + getNameForHeader(header, includePath) + "\"";
                name = "ancestors of " + predicate.getEntityType().getName().toLowerCase() + " " + entityName;
            } else {
                LOGGER.log(Level.WARNING, "Unable to determine name for EntityFolderAncestryPredicate because it is missing entity type and/or entity id.");
            }
        } else if (entity instanceof NamedEntityImp) {
            final NamedEntityImp named = (NamedEntityImp) entity;
            name = named.getName();
        }
        return name;
    }

    /**
     * Get the folder path for a HasFolderId which can have a reference to a Folder.
     * <p/>
     * The path will always start with '/' to represent the root folder (even if the given HasFolderId does not reference any Folder).
     *
     * @param hasFolder the HasFolderId which can have a reference to a Folder (most likely a header).
     * @return the folder path for the HasFolderId or '/' if no Folder is referenced.
     * @throws FindException             if there is an error looking up the Folder referenced by the HasFolderId.
     * @throws PermissionDeniedException if the user does not have permission to access an entity required to resolve the path
     */

    public String getPath(final HasFolderId hasFolder) throws FindException {
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
     * Get the folder path for a HasFolder entity.
     * <p/>
     * The path will always start with '/' to represent the root folder (even if the given HasFolder entity does not have a Folder).
     *
     * @param hasFolder the HasFolder entity for which to retrieve a folder path.
     * @return the folder path for the HasFolder entity of '/' if there is no Folder.
     */

    public String getPath(final HasFolder hasFolder) {
        final String path;
        if (hasFolder instanceof Folder && isRootFolder((Folder) hasFolder)) {
            path = ROOT;
        } else {
            path = getPathForFolder(hasFolder.getFolder());
        }
        return path;
    }

    protected boolean isRootFolder(final EntityHeader header) {
        return EntityType.FOLDER == header.getType() && Folder.ROOT_FOLDER_ID.equals(header.getGoid());
    }

    protected boolean isRootFolder(final Folder folder) {
        return Folder.ROOT_FOLDER_ID.equals(folder.getGoid());
    }

    protected boolean canResolveName(final EntityHeader entityHeader) {
        return false;
    }

    protected boolean canResolveName(final Entity entity) {
        return false;
    }

    protected String resolve(final EntityHeader entityHeader, final boolean includePath) throws FindException {
        return null;
    }


    protected String resolve(final Entity entity, final boolean includePath) throws FindException {
        return null;
    }

    protected void validateFoundEntity(final EntityHeader header, final Entity foundEntity) throws FindException {
        validateFoundEntity(header.getType(), header.getGoid(), foundEntity);
    }

    protected void validateFoundEntity(final EntityType type, final Goid goid, final Entity foundEntity) throws FindException {
        if (foundEntity == null) {
            throw new FindException("No entity found for type " + type + " and goid " + goid);
        }
    }

    protected String getUniqueInfo(final Entity entity) {
        String extraInfo = StringUtils.EMPTY;
        if (entity instanceof PublishedService) {
            final PublishedService service = (PublishedService) entity;
            if (service.getRoutingUri() != null) {
                extraInfo = service.getRoutingUri();
            }
        }
        return extraInfo;
    }

    protected String buildName(final String name, final String uniqueInfo, final String path, final boolean includeNameInPath) {
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

    protected String getPathForFolder(@Nullable final Folder folder) {
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

    /**
     * Some entity types may require other info than its name and/or path to make it unique from others.
     */
    protected String getUniqueInfo(final EntityHeader header, @Nullable final Entity retrievedEntity) {
        String extraInfo = StringUtils.EMPTY;
        if (header.getType() != null) {
            switch (header.getType()) {
                case SERVICE:
                    if (header instanceof ServiceHeader) {
                        String routingUri = ((ServiceHeader) header).getRoutingUri();
                        extraInfo = routingUri != null ? routingUri : StringUtils.EMPTY;
                    }
                    break;
                case SERVICE_ALIAS:
                    extraInfo = getServiceAliasExtraInfo(header, retrievedEntity);
                    break;
                default:
                    extraInfo = StringUtils.EMPTY;
            }
        }
        return extraInfo;
    }

    private String getServiceAliasExtraInfo(final EntityHeader header, @Nullable final Entity retrievedEntity) {
        String extraInfo = StringUtils.EMPTY;
        if (retrievedEntity != null) {
            extraInfo = getUniqueInfo(retrievedEntity);
        } else {
            try {
                final PublishedService service = serviceAdmin.findByAlias(header.getGoid());
                extraInfo = getUniqueInfo(service);
            } catch (FindException e) {
                LOGGER.log(Level.SEVERE, "Not able to find published service.");
            }
        }
        return extraInfo;
    }

    protected String getNameForAssertion(final Assertion assertion, final String defaultName) {
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
}
