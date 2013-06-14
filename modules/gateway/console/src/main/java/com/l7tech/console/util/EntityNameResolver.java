package com.l7tech.console.util;

import com.l7tech.gateway.common.admin.FolderAdmin;
import com.l7tech.gateway.common.admin.PolicyAdmin;
import com.l7tech.gateway.common.resources.HttpConfiguration;
import com.l7tech.gateway.common.resources.ResourceAdmin;
import com.l7tech.gateway.common.resources.ResourceEntry;
import com.l7tech.gateway.common.security.TrustedCertAdmin;
import com.l7tech.gateway.common.security.keystore.SsgKeyMetadata;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.gateway.common.service.ServiceAdmin;
import com.l7tech.gateway.common.service.ServiceHeader;
import com.l7tech.objectmodel.Entity;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.folder.Folder;
import com.l7tech.objectmodel.folder.HasFolder;
import com.l7tech.objectmodel.folder.HasFolderOid;
import com.l7tech.policy.Policy;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedList;
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

    private final ServiceAdmin serviceAdmin;
    private final PolicyAdmin policyAdmin;
    private final TrustedCertAdmin trustedCertAdmin;
    private final ResourceAdmin resourceAdmin;
    private final FolderAdmin folderAdmin;

    public EntityNameResolver(@NotNull final ServiceAdmin serviceAdmin,
                              @NotNull final PolicyAdmin policyAdmin,
                              @NotNull final TrustedCertAdmin trustedCertAdmin,
                              @NotNull final ResourceAdmin resourceAdmin,
                              @NotNull final FolderAdmin folderAdmin) {
        this.serviceAdmin = serviceAdmin;
        this.policyAdmin = policyAdmin;
        this.trustedCertAdmin = trustedCertAdmin;
        this.resourceAdmin = resourceAdmin;
        this.folderAdmin = folderAdmin;
    }

    /**
     * Resolves a descriptive name for a given EntityHeader which may include a name and/or folder path and/or other unique info depending on the header type.
     * <p/>
     * If there is a non-empty, non-oid name on the header, it will take precedence (this should happen in most cases).
     * <p/>
     * Otherwise the descriptive name will be resolved by looking up the entity that is referenced by the header (this is usually the case is for entities that don't have a name).
     *
     * @param header      the EntityHeader for which to determine a descriptive name. Usually a header that has just been retrieved via an Admin call.
     * @param includePath true if entities in folders should have their folder path included in the descriptive name.
     * @return a name for the given EntityHeader which may include a name and/or folder path and/or other unique info. Cannot be null.
     *         Can be empty if the name on the header is empty/null and the resolver does not know how to look up the referenced entity.
     * @throws FindException if a db error occurs or the entity referenced by the header does not exist.
     */
    @NotNull
    public String getNameForHeader(@NotNull final EntityHeader header, final boolean includePath) throws FindException {
        final String nameOnHeader = header.getName();
        String name = nameOnHeader == null ? StringUtils.EMPTY : nameOnHeader;
        Entity retrievedEntity = null;
        if (header.getType() != null && (StringUtils.isBlank(name) || String.valueOf(header.getOid()).equals(name))) {
            switch (header.getType()) {
                case SERVICE_ALIAS:
                    final PublishedService owningService = serviceAdmin.findByAlias(header.getOid());
                    validateFoundEntity(header, owningService);
                    name = owningService.getName() + " alias";
                    retrievedEntity = owningService;
                    break;
                case POLICY_ALIAS:
                    final Policy owningPolicy = policyAdmin.findByAlias(header.getOid());
                    validateFoundEntity(header, owningPolicy);
                    name = owningPolicy.getName() + " alias";
                    retrievedEntity = owningPolicy;
                    break;
                case SSG_KEY_METADATA:
                    final SsgKeyMetadata metadata = trustedCertAdmin.findKeyMetadata(header.getOid());
                    validateFoundEntity(header, metadata);
                    name = metadata.getAlias();
                    retrievedEntity = metadata;
                    break;
                case RESOURCE_ENTRY:
                    final ResourceEntry resourceEntry = resourceAdmin.findResourceEntryByPrimaryKey(header.getOid());
                    validateFoundEntity(header, resourceEntry);
                    name = resourceEntry.getUri();
                    retrievedEntity = resourceEntry;
                    break;
                case HTTP_CONFIGURATION:
                    final HttpConfiguration httpConfig = resourceAdmin.findHttpConfigurationByPrimaryKey(header.getOid());
                    validateFoundEntity(header, httpConfig);
                    name = httpConfig.getProtocol() + " " + httpConfig.getHost() + " " + httpConfig.getPort();
                    retrievedEntity = httpConfig;
                    break;
                default:
                    logger.log(Level.WARNING, "Name on header is null or empty but entity type is not supported: " + header.getType());
                    name = StringUtils.EMPTY;
            }
        }

        String path = null;
        if (includePath) {
            path = resolvePath(header, retrievedEntity);
        }
        String uniqueInfo = getUniqueInfo(header);
        if (StringUtils.isNotBlank(uniqueInfo)) {
            uniqueInfo = "[" + uniqueInfo + "]";
        }

        return path == null ? name + uniqueInfo : name + uniqueInfo + " (" + path + name + ")";
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
     * Some entity types may require other info than its name and/or path to make it unique from others.
     */
    private String getUniqueInfo(@NotNull final EntityHeader header) {
        String extraInfo = StringUtils.EMPTY;
        if (header.getType() != null) {
            switch (header.getType()) {
                case SERVICE:
                    if (header instanceof ServiceHeader) {
                        final ServiceHeader serviceHeader = (ServiceHeader) header;
                        extraInfo = serviceHeader.getRoutingUri();
                    }
                    break;
                default:
                    extraInfo = StringUtils.EMPTY;
            }
        }
        return extraInfo;
    }

    /**
     * Get the folder path for a HasFolderOid which can have a reference to a Folder.
     * <p/>
     * The path will always start with '/' to represent the root folder (even if the given HasFolderOid does not reference any Folder).
     *
     * @param hasFolder the HasFolderOid which can have a reference to a Folder (most likely a header).
     * @return the folder path for the HasFolderOid or '/' if no Folder is referenced.
     * @throws FindException if there is an error looking up the Folder referenced by the HasFolderOid.
     */
    @NotNull
    public String getPath(@NotNull final HasFolderOid hasFolder) throws FindException {
        Folder folder = null;
        if (hasFolder.getFolderOid() != null) {
            folder = folderAdmin.findByPrimaryKey(hasFolder.getFolderOid());
        }
        return getPathForFolder(folder);
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
        return getPathForFolder(hasFolder.getFolder());
    }

    private String resolvePath(@NotNull final EntityHeader header, @Nullable final Entity retrievedEntity) throws FindException {
        String path = null;
        if (retrievedEntity != null && retrievedEntity instanceof HasFolder) {
            // no need to look up the entity to get the path
            final HasFolder hasFolder = (HasFolder) retrievedEntity;
            path = getPath(hasFolder);
        }

        if (header instanceof HasFolderOid && path == null) {
            // need to look up the entity to get the path
            final HasFolderOid hasFolder = (HasFolderOid) header;
            path = getPath(hasFolder);
        }
        return path;
    }

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
        if (foundEntity == null) {
            throw new FindException("No entity found for type " + header.getType() + " and oid " + header.getOid());
        }
    }
}
