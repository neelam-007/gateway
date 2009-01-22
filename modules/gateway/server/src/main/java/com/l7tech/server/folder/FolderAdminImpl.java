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
            if (folder.getOid() == Folder.DEFAULT_OID) {
                validateFolders( folder, new Functions.Binary<SaveException,String,Throwable>(){
                    @Override
                    @SuppressWarnings({"ThrowableInstanceNeverThrown"})
                    public SaveException call(String s, Throwable throwable) {
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
        }
    }

    @SuppressWarnings({"unchecked"})
    @Override
    public void moveEntityToFolder( final Folder folder, final PersistentEntity entity ) throws UpdateException {
        if ( entity == null ) throw new UpdateException( "Entity is required." );

        Folder targetFolder = folder;
        if ( targetFolder == null ) {
            try {
                targetFolder = folderManager.findRootFolder();
            } catch ( FindException fe ) {
                throw new UpdateException( "Error finding root folder.", fe );
            }

            if ( targetFolder == null ) {
                throw new UpdateException( "Error finding root folder." );                
            }
        }

        if ( entity instanceof Folder ) {
            validateFolders( (Folder) entity, new Functions.Binary<UpdateException,String,Throwable>(){
                @Override
                @SuppressWarnings({"ThrowableInstanceNeverThrown"})
                public UpdateException call(String s, Throwable throwable) {
                    return new UpdateException(s, throwable);
                }
            } );
        }

        FolderedEntityManager manager = entityManagerMap.get( entity.getClass() );
        manager.updateFolder( entity, folder );
    }

    private <T extends Throwable> void validateFolders( final Folder folder, final Functions.Binary<T,String,Throwable> exceptionBuilder ) throws T {
        final int maxDepth = ServerConfig.getInstance().getIntProperty("policyorganization.maxFolderDepth",8);
        final Folder pf = folder.getFolder();
        final Long parentFolderId = pf == null ? null : pf.getOid();
        if (parentFolderId != null) {
            try {
                Folder parentFolder = folderManager.findByPrimaryKey(parentFolderId);
                long folderId = folder.getOid();
                if (parentFolderId == folderId){
                    throw exceptionBuilder.call("Parent folder cannot be the same as folder id", null);
                }

                //This will load all the folders parents
                int levels = 0;
                while (parentFolder != null){
                    levels++;
                    parentFolder = parentFolder.getFolder();
                }
                if (levels > maxDepth){
                    throw exceptionBuilder.call("Folder hierarchy can only be "+maxDepth+" levels deep", null);
                }
            } catch (FindException e) {
                throw exceptionBuilder.call("Could not find parent folder", e);
            }
        }
    }

}
