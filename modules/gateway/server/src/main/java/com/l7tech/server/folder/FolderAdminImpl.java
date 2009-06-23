/*
 * Copyright (C) 2008 Layer 7 Technologies Inc.
 */
package com.l7tech.server.folder;

import com.l7tech.gateway.common.admin.FolderAdmin;
import com.l7tech.gateway.common.security.rbac.RbacAdmin;
import com.l7tech.objectmodel.*;
import static com.l7tech.objectmodel.EntityType.FOLDER;
import com.l7tech.objectmodel.folder.Folder;
import com.l7tech.objectmodel.folder.FolderHeader;
import com.l7tech.objectmodel.folder.FolderedEntityManager;
import com.l7tech.server.ServerConfig;
import com.l7tech.server.security.rbac.RoleManager;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.Functions;

import java.text.MessageFormat;
import java.util.Collection;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * @author darmstrong
 */
public class FolderAdminImpl implements FolderAdmin {
    private static final int MAX_FOLDER_NAME_LENGTH = 128;
    private static final int MAX_FOLDER_DEPTH = ServerConfig.getInstance().getIntProperty(ServerConfig.MAX_FOLDER_DEPTH_PROPERTY, 8);

    private static final Pattern replaceRoleName =
            Pattern.compile(MessageFormat.format(RbacAdmin.RENAME_REGEX_PATTERN, ROLE_NAME_TYPE_SUFFIX));

    private final FolderManager folderManager;
    private final RoleManager roleManager;
    private final Map<Class<? extends Entity>, FolderedEntityManager> entityManagerMap;

    public FolderAdminImpl( final FolderManager folderManager,
                            final RoleManager roleManager,
                            final Map<Class<? extends Entity>, FolderedEntityManager> entityManagerMap) {
        this.folderManager = folderManager;
        this.roleManager = roleManager;
        this.entityManagerMap = entityManagerMap;
    }

    @Override
    public void deleteFolder( final long oid ) throws FindException, DeleteException {
        roleManager.deleteEntitySpecificRoles(FOLDER, oid);
        folderManager.delete(oid);
    }

    @Override
    public Folder findByPrimaryKey( final long oid ) throws FindException {
        return folderManager.findByPrimaryKey(oid);
    }

    @Override
    public Collection<FolderHeader> findAllFolders() throws FindException {
        return folderManager.findAllHeaders();
    }

    /**
     *  Save a new folder
     * @param folder: the new folder to be saved
     * @return oid of the new folder if successful.
     * @throws UpdateException
     * @throws SaveException
     * @throws ConstraintViolationException
     */
    @Override
    public long saveFolder( final Folder folder ) throws UpdateException, SaveException, ConstraintViolationException {
        final String name = folder.getName();
        if (name != null) {
            if (name.length() > MAX_FOLDER_NAME_LENGTH) {
                String message = "Folder name cannot exceed "+MAX_FOLDER_NAME_LENGTH+" characters";
                if ( folder.getOid() == Folder.DEFAULT_OID ) {
                    throw new SaveException( message );
                } else {
                    throw new UpdateException( message );
                }
            }
        }

        try {
            long oid;
            Folder rootFolder = folderManager.findRootFolder();

            if (folder.getOid() == Folder.DEFAULT_OID) {
                Folder targetFolder = folderManager.findByPrimaryKey(folder.getFolder().getOid()); // the  parent folder might be changed in other places.
                final int targetMaxDepth = MAX_FOLDER_DEPTH - rootFolder.getNesting(targetFolder);
                validateFolders( folder, targetMaxDepth, 1, new Functions.Binary<SaveException,String,Throwable>() {
                    @Override
                    @SuppressWarnings({"ThrowableInstanceNeverThrown"})
                    public SaveException call(String s, Throwable throwable) {
                        // Overwrite the message, s.
                        s = "The maximum folder depth is " + MAX_FOLDER_DEPTH + ".\nTarget folder cannot be nested further.";
                        return new SaveException(s, throwable);
                    }
                }  );
                oid = folderManager.save(folder);
                folder.setOid(oid);
                try {
                    folderManager.addManageFolderRole( folder );
                    folderManager.addReadonlyFolderRole( folder );
                } catch (SaveException e) {
                    throw new SaveException("Error creating Role for Folder.", e);
                }
            } else {
                folderManager.update(folder);
                try {
                    roleManager.renameEntitySpecificRoles(FOLDER, folder, replaceRoleName);
                } catch (FindException e) {
                    throw new UpdateException("Couldn't find Role to rename", e);
                }
                oid = folder.getOid();
            }
            return oid;
        } catch (DuplicateObjectException doe) {
            //thrown when saving a folder with duplicate folder name
            throw new ConstraintViolationException("Folder name already exists", doe);
        } catch (UpdateException ue) {
            //verify if trying to update with a duplicate folder name
            if (ExceptionUtils.causedBy(ue, DuplicateObjectException.class)) {
                throw new ConstraintViolationException("Folder name already exists", ue);
            } else {
                //rethrow the exception
                throw ue;
            }
        } catch (FindException fe) {
            throw new SaveException("Error finding a folder.", fe);
        }
    }

    /**
     * Move an entity  into a folder
     * @param folder: the folder which the entity will move to.
     * @param entity: the entity to be moved.
     * @throws UpdateException
     */
    @SuppressWarnings({"unchecked"})
    @Override
    public void moveEntityToFolder( final Folder folder, PersistentEntity entity ) throws UpdateException {
        if ( entity == null ) throw new UpdateException( "Entity is required." );

        Folder rootFolder;
        try {
            rootFolder = folderManager.findRootFolder();
        } catch ( FindException fe ) {
            throw new UpdateException( "Error finding root folder.", fe );
        }

        Folder targetFolder = folder != null ? folder : rootFolder;

        if(entity.getOid() == targetFolder.getOid())
            throw new UpdateException("Parent folder cannot be the same as folder id", null);

        if ( entity instanceof Folder ) {
            try {
                if (((Folder)entity).isParentOf(targetFolder))
                    throw new UpdateException("The destination folder is a subfolder of the source folder");

                final int targetMaxDepth = MAX_FOLDER_DEPTH - rootFolder.getNesting(targetFolder);
                for(Folder maybeChild : folderManager.findAll()) {
                    int relativeChildDepth = ((Folder) entity).getNesting(maybeChild);
                    if (relativeChildDepth >= 0) // it's a child of the moved folder
                        validateFolders(maybeChild, targetMaxDepth, relativeChildDepth + 1, new Functions.Binary<UpdateException, String, Throwable>() {
                            @Override
                            @SuppressWarnings({"ThrowableInstanceNeverThrown"})
                            public UpdateException call(String s, Throwable throwable) {
                                return new UpdateException(s, throwable);
                            }
                        });
                }
            } catch (FindException e) {
                throw new UpdateException("Could not retrieve folder list.", e);
            }
        }

        FolderedEntityManager manager = entityManagerMap.get( entity.getClass() );
        manager.updateFolder( entity, folder );
    }

    /**
     * Performs the following validations on the supplied Folder:
     * <ul>
     * <li> validates its parentFolderId points to an existing Folder </li>
     * <li> ensures that the parent folder is not itself </li>
     * <li> checks that the folder depth does not exceed a configured limit </li>
     * </ul>
     *
     * @param folder               The folder to be validated
     * @param targetMaxDepth       The maximum depth allowed where 
     * @param referenceFolderDepth Depth of the of the folder being moved, or 1 for create/save operations.
     * @param exceptionBuilder     Exception builder to be used on failure
     * @throws T                   If the validation fails or if an error prevents it from completing
     */
    private <T extends Throwable> void validateFolders( final Folder folder, int targetMaxDepth, int referenceFolderDepth,
                                                        final Functions.Binary<T,String,Throwable> exceptionBuilder ) throws T {
        Long parentFolderId = folder.getFolder() != null ? folder.getFolder().getOid() : null;
        if (parentFolderId != null) {
            if (parentFolderId == folder.getOid())
                throw exceptionBuilder.call("Parent folder cannot be the same as folder id", null);

            if (referenceFolderDepth > targetMaxDepth)
                throw exceptionBuilder.call("The maximum folder depth is " + MAX_FOLDER_DEPTH + ".\nTarget folder can accept a folder hierarchy at most "
                    + targetMaxDepth + (targetMaxDepth == 1? " level":" levels") + " deep.", null);
        }
    }
}
