package com.l7tech.server.folder;

import com.l7tech.gateway.common.admin.FolderAdmin;
import com.l7tech.gateway.common.security.rbac.RbacAdmin;
import com.l7tech.gateway.common.security.rbac.Role;
import com.l7tech.objectmodel.*;
import com.l7tech.objectmodel.folder.Folder;
import com.l7tech.objectmodel.folder.FolderHeader;
import com.l7tech.server.FolderSupportHibernateEntityManager;
import com.l7tech.server.security.rbac.RoleManager;
import com.l7tech.util.Config;
import com.l7tech.util.TextUtils;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.text.MessageFormat;
import java.util.*;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import static com.l7tech.gateway.common.security.rbac.OperationType.*;
import static com.l7tech.objectmodel.EntityType.*;
import static org.springframework.transaction.annotation.Propagation.REQUIRED;

/**
 * Implementation of the service/policy folder manager.
 */
@Transactional(propagation=REQUIRED, rollbackFor=Throwable.class)
public class FolderManagerImpl extends FolderSupportHibernateEntityManager<Folder, FolderHeader> implements FolderManager {
    @SuppressWarnings({ "FieldNameHidesFieldInSuperclass" })
    private static final Logger logger = Logger.getLogger(FolderManagerImpl.class.getName());

    private static final String ROLE_NAME_TYPE_SUFFIX = FolderAdmin.ROLE_NAME_TYPE_SUFFIX;
    private static final String ROLE_ADMIN_NAME_PATTERN = RbacAdmin.ROLE_NAME_PREFIX + " {0} " + ROLE_NAME_TYPE_SUFFIX + RbacAdmin.ROLE_NAME_OID_SUFFIX;
    private static final String ROLE_READ_NAME_PATTERN = RbacAdmin.ROLE_NAME_PREFIX_READ + " {0} " + ROLE_NAME_TYPE_SUFFIX + RbacAdmin.ROLE_NAME_OID_SUFFIX;
    private static final Pattern replaceRoleName =
            Pattern.compile(MessageFormat.format(RbacAdmin.RENAME_REGEX_PATTERN, ROLE_NAME_TYPE_SUFFIX));
    static final String AUTO_CREATE_VIEW_ROLE_PROPERTY = "rbac.autoRole.viewFolder.autoCreate";
    static final String AUTO_CREATE_MANAGE_ROLE_PROPERTY = "rbac.autoRole.manageFolder.autoCreate";

    private final RoleManager roleManager;
    private final Config config;

    public FolderManagerImpl( final RoleManager roleManager, @NotNull final Config config) {
        this.roleManager = roleManager;
        this.config = config;
    }

    @Override
    @Transactional(propagation= Propagation.SUPPORTS)
    public Class<? extends Entity> getImpClass() {
        return Folder.class;
    }

    @Override
    @Transactional(propagation=Propagation.SUPPORTS)
    public Class<? extends Entity> getInterfaceClass() {
        return Folder.class;
    }

    @Override
    @Transactional(propagation= Propagation.SUPPORTS)
    public String getTableName() {
        return "policy_folder";
    }

    @Override
    protected FolderHeader newHeader( final Folder entity ) {
        return new FolderHeader( entity );
    }

    @Override
    protected UniqueType getUniqueType() {
        return UniqueType.OTHER;
    }

    @Override
    protected Collection<Map<String, Object>> getUniqueConstraints(Folder entity) {
        Map<String,Object> map = new HashMap<String, Object>();
        map.put("folder", entity.getFolder());
        map.put("name",entity.getName());
        return Arrays.asList(map);
    }

    @Override
    public Folder findByHeader(EntityHeader header) throws FindException {
        Folder result = super.findByHeader(header);
        if (result != null) {
            return result;
        } else {
            return pathLookup(header);
        }
    }

    private Folder pathLookup(EntityHeader header) throws FindException {
        String path = header == null ? null : header.getDescription();
        if (path == null) return null;

        Folder rootFolder = findRootFolder();
        if ("/".equals(path))
            return rootFolder;

        List<Folder> matchingFolders;
        Map<String,Object> lookupFields = new HashMap<String, Object>();

        Folder parent = rootFolder;
        String[] pathElements = path.split("/");
        for(String pathElement : pathElements) {
            if ("".equals(pathElement)) continue;
            lookupFields.put("folder", parent);
            lookupFields.put("name", pathElement);
            matchingFolders = findMatching(Arrays.asList(lookupFields));
            if (matchingFolders == null || matchingFolders.size() != 1) {
                return null;
            } else {
                parent = matchingFolders.get(0);
            }
        }

        return parent;
    }

    @Override
    public void update(Folder entity) throws UpdateException {
        try {
            // check for version conflict
            Folder dbFolderVersion = findByPrimaryKey(entity.getGoid());
            if (dbFolderVersion == null) {
                //folder was deleted by someone else already
                String msg = "Unable to save folder because the folder was deleted by another user.\n" +
                        "Please refresh the tree for an updated version.";
                logger.info("Cannot update because folder '" + entity.getName() + "' was deleted");
                throw new StaleUpdateException(msg);
            } else if (dbFolderVersion.getVersion() != entity.getVersion()) {
                String msg = "Unable to save folder because the folder was edited by another user.\n " +
                        "Please refresh the tree for an updated version.";
                logger.info("Folder " + entity.getName() + "' version mismatch");
                throw new StaleUpdateException(msg);
            }
        } catch (FindException fe) {
            throw new UpdateException("Couldn't find previous version(s) to check for circular folders");
        }

        //proceed with update
        super.update(entity);
    }


    @Override
    public void updateFolder( final Goid entityId, final Folder folder ) throws UpdateException {
        setParentFolderForEntity( entityId, folder );
    }

    @Override
    public void updateFolder( final Folder entity, final Folder folder ) throws UpdateException {
        if ( entity == null ) throw new UpdateException("Folder is required but missing.");
        setParentFolderForEntity( entity.getGoid(), folder );
    }

    @Override
    public Folder findRootFolder() throws FindException {
        //root folder should have no parent folder, ie null
        Collection<Folder> folders = super.findAll();
        for (Folder folder : folders) {
            if (folder.getFolder() == null) {
                return folder;
            }
        }
        throw new FindException("No root folder!!");
    }

    /**
     * Find a folder by an absolute folder path.
     * e.g., given "/folderA/folderB", try to find the folder with name as "folderB" under the folderA.
     * This method is top-to-bottom manner to find the last folder on the folder path, to assure the result is unique.
     *
     * @param absFolderPath: a string represents an absolute folder path
     * @return a Folder object found
     *
     * @throws FindException: thrown if no folder can be found by the given folder path
     */
    @Override
    public Folder findByPath(final String absFolderPath) throws FindException {
        if (StringUtils.isBlank(absFolderPath) || !absFolderPath.startsWith("/")) return null;

        Folder currentFolder = findRootFolder();
        String currentPath = "/";

        final String[] folderNames = absFolderPath.split("/");
        for (String folderName: folderNames) {
            if (folderName.equals("")) continue;

            boolean found = false;
            final Collection<Folder> subFolders = findByFolder(currentFolder.getGoid());
            for (final Folder folder: subFolders) {
                if (folder.getName().equals(folderName)) {
                    found = true;
                    currentFolder = folder; // for next loop, to find its sub-folders
                    break;
                }
            }
            // Update the current path, just in case any error occurring. currentPath will be used for error reporting.
            if (currentPath.equals("/")) {
                currentPath += folderName;
            } else {
                currentPath += "/" + folderName;
            }

            if (!found) {
                throw new FindException("There is no such folder path: " + currentPath);
            }
        }

        return currentFolder;
    }

    /**
     * Build a folder path.  All existing folders on the path starting from the root folder will be ignored until the first
     * new folder is found.  The first new folder and all folders after the first new folders will be created. The last
     * folder on the path will be returned.
     *
     * For example, the absolute folder path is /a/b/c.  If a, b, and c are new, then a, b, and c will be created and c
     * will be returned.  If a is an existing folder and b is the first new one, then b and c will be created and c will
     * be returned.
     *
     * @param absFolderPath an absolute folder path
     * @return a last folder on the folder path.
     *
     * @throws FindException thrown if unable to retrieve entities by folder
     * @throws SaveException thrown if unable to save a folder
     */
    @Override
    public Folder buildByPath(String absFolderPath) throws FindException, SaveException {
        if (StringUtils.isBlank(absFolderPath) || !absFolderPath.startsWith("/")) {
            throw new IllegalArgumentException("The folder path is not an absolute path.");
        }

        final String[] folderNames = absFolderPath.split("/");
        Folder parentFolder = findRootFolder();
        Folder newFolder = null;
        boolean foundFirstNewFolder = false;  // A flag to find the first new folder

        for (String folderName: folderNames) {
            if (folderName.equals("")) continue;

            if (!foundFirstNewFolder) { // If the first new folder is found, then do not run this block anymore.
                boolean found = false;  // A flag to find a matched child folder
                final Collection<Folder> subFolders = findByFolder(parentFolder.getGoid());
                for (final Folder folder: subFolders) {
                    if (folder.getName().equals(folderName)) {
                        found = true;
                        parentFolder = folder; // for next loop, to find its sub-folders
                        break;
                    }
                }
                if (!found) {
                    foundFirstNewFolder = true;
                }
            }

            if (foundFirstNewFolder) {
                newFolder = new Folder(folderName, parentFolder);
                save(newFolder);
                parentFolder = newFolder;
            }
        }

        return newFolder;
    }

    @Override
    public void createRoles( final Folder folder ) throws SaveException {
        if (config.getBooleanProperty(AUTO_CREATE_VIEW_ROLE_PROPERTY, true)) {
            addReadonlyFolderRole( folder );
        }
        if (config.getBooleanProperty(AUTO_CREATE_MANAGE_ROLE_PROPERTY, true)) {
            addManageFolderRole( folder );
        }
    }

    @Override
    public void updateRoles( final Folder folder ) throws UpdateException {
        try {
            roleManager.renameEntitySpecificRoles(FOLDER, folder, replaceRoleName);
        } catch (FindException e) {
            throw new UpdateException("Couldn't find Role to rename", e);
        }
    }

    /**
     * Creates management new role for the specified Folder.
     *
     * @throws com.l7tech.objectmodel.SaveException  if the new Role could not be saved
     */
    @Override
    public void addManageFolderRole( final Folder folder ) throws SaveException {
        // truncate folder name in the role name to avoid going beyond 128 limit
        // cutoff is arbitrarily set to 50
        String folderNameForRole = TextUtils.truncStringMiddle( folder.getName(), 50 );
        String name = MessageFormat.format(ROLE_ADMIN_NAME_PATTERN, folderNameForRole, folder.getGoid() );

        logger.info("Creating new Role: " + name);

        Role role = new Role();
        role.setName(name);

        role.addEntityPermission(READ, FOLDER, folder.getId());

        role.addFolderPermission(READ,   SERVICE, folder, true);
        role.addFolderPermission(UPDATE, SERVICE, folder, true);
        role.addFolderPermission(DELETE, SERVICE, folder, true);

        role.addFolderPermission(READ,   POLICY, folder, true);
        role.addFolderPermission(UPDATE, POLICY, folder, true);
        role.addFolderPermission(DELETE, POLICY, folder, true);

        role.addFolderPermission(READ,   SERVICE_ALIAS, folder, true);
        role.addFolderPermission(DELETE, SERVICE_ALIAS, folder, true);

        role.addFolderPermission(READ,   POLICY_ALIAS, folder, true);
        role.addFolderPermission(DELETE, POLICY_ALIAS, folder, true);

        role.addFolderPermission(READ,   FOLDER, folder, true);

        // Search all identities (for adding IdentityAssertions)
        role.addEntityPermission(READ, ID_PROVIDER_CONFIG, null);
        role.addEntityPermission(READ, USER, null);
        role.addEntityPermission(READ, GROUP, null);

        // Read all JMS queues
        role.addEntityPermission(READ, JMS_CONNECTION, null);
        role.addEntityPermission(READ, JMS_ENDPOINT, null);

        // Read this folders's folder ancestry
        role.addEntityFolderAncestryPermission(FOLDER, folder.getGoid());

        // Read all service templates
        role.addEntityPermission(READ, SERVICE_TEMPLATE, null);

        // Read all JDBC connections
        role.addEntityPermission(READ, JDBC_CONNECTION, null);

        // Read all SSG active connectors (e.g. MQ, SFTP polling)
        role.addEntityPermission(READ, SSG_ACTIVE_CONNECTOR, null);

        // Read all Encapsulated Assertion configs
        role.addEntityPermission(READ, ENCAPSULATED_ASSERTION, null);

        // use all assertions in policy
        role.addEntityPermission(READ, ASSERTION_ACCESS, null);

        // Set role as entity-specific
        role.setEntityType(FOLDER);
        role.setEntityGoid(folder.getGoid());
        role.setDescription("Users assigned to the {0} role have the ability to view, update and delete services and policies within the {1} folder.");

        roleManager.save(role);
    }

    /**
     * Creates readonly role for the specified Folder.
     *
     * @throws SaveException  if the new Role could not be saved
     */
    @Override
    public void addReadonlyFolderRole( final Folder folder ) throws SaveException {
        // truncate folder name in the role name to avoid going beyond 128 limit
        // cutoff is arbitrarily set to 50
        String folderNameForRole = TextUtils.truncStringMiddle( folder.getName(), 50 );
        String name = MessageFormat.format(ROLE_READ_NAME_PATTERN, folderNameForRole, folder.getGoid() );

        logger.info("Creating new Role: " + name);

        Role role = new Role();
        role.setName(name);

        role.addEntityPermission(READ, FOLDER, folder.getId());

        role.addFolderPermission(READ, SERVICE, folder, true);
        role.addFolderPermission(READ, POLICY, folder, true);
        role.addFolderPermission(READ, SERVICE_ALIAS, folder, true);
        role.addFolderPermission(READ, POLICY_ALIAS, folder, true);
        role.addFolderPermission(READ, FOLDER, folder, true);

        // Search all identities (for adding IdentityAssertions)
        role.addEntityPermission(READ, ID_PROVIDER_CONFIG, null);
        role.addEntityPermission(READ, USER, null);
        role.addEntityPermission(READ, GROUP, null);

        // Read all JMS queues
        role.addEntityPermission(READ, JMS_CONNECTION, null);
        role.addEntityPermission(READ, JMS_ENDPOINT, null);

        // Read this folders's folder ancestry
        role.addEntityFolderAncestryPermission(FOLDER, folder.getGoid());

        // Read all service templates
        role.addEntityPermission(READ, SERVICE_TEMPLATE, null);

        // Read all Encapsulated Assertion configs
        role.addEntityPermission(READ, ENCAPSULATED_ASSERTION, null);

        // use all assertions in policy
        role.addEntityPermission(READ, ASSERTION_ACCESS, null);

        // Set role as entity-specific
        role.setEntityType(FOLDER);
        role.setEntityGoid(folder.getGoid());
        role.setDescription("Users assigned to the {0} role have the ability to view services and policies within the {1} folder.");

        roleManager.save(role);
    }
}
