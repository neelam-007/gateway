/*
 * Copyright (C) 2008 Layer 7 Technologies Inc.
 */
package com.l7tech.server.folder;

import com.l7tech.gateway.common.admin.FolderAdmin;
import com.l7tech.objectmodel.*;
import com.l7tech.objectmodel.folder.Folder;
import com.l7tech.objectmodel.folder.FolderHeader;
import com.l7tech.server.ServerConfig;
import com.l7tech.util.ExceptionUtils;

import java.text.MessageFormat;
import java.util.Collection;

/**
 * @author darmstrong
 */
public class FolderAdminImpl implements FolderAdmin {
    private final FolderManager folderManager;
    private static final int MAX_FOLDER_NAME_LENGTH = 128;

    public FolderAdminImpl(FolderManager folderManager) {
        this.folderManager = folderManager;
    }

    @Override
    public void deleteFolder(long oid) throws FindException, DeleteException {
        folderManager.delete(oid);
    }

    @Override
    public Folder findByPrimaryKey(long oid) throws FindException {
        return folderManager.findByPrimaryKey(oid);
    }

    @Override
    public Collection<FolderHeader> findAllFolders() throws FindException {
        return folderManager.findAllHeaders();
    }

    @Override
    public long saveFolder(Folder folder) throws UpdateException, SaveException, ConstraintViolationException {
        final String name = folder.getName();
        if (name != null) {
            if (name.length() > MAX_FOLDER_NAME_LENGTH) {
                return throwit(folder, "Folder name cannot exceed {0} characters", MAX_FOLDER_NAME_LENGTH);
            }
        }

        final int maxDepth = ServerConfig.getInstance().getIntProperty("policyorganization.maxFolderDepth",8);

        final Folder pf = folder.getParentFolder();
        final Long parentFolderId = pf == null ? null : pf.getOid();
        if (parentFolderId != null) {
            try {
                Folder parentFolder = folderManager.findByPrimaryKey(parentFolderId);
                long folderId = folder.getOid();
                if (parentFolderId == folderId){
                    throw new UpdateException("Parent folder cannot be the same as folder id");
                }

                //This will load all the folders parents
                int levels = 0;
                while (parentFolder != null){
                    levels++;
                    parentFolder = parentFolder.getParentFolder();
                }
                if (levels > maxDepth){
                    throwit(folder, "Folder hierarchy can only be {0} levels deep", maxDepth);
                }
            } catch (FindException e) {
                throw new SaveException("Could not find parent folder", e);
            }
        }

        try {
            if (folder.getOid() == Folder.DEFAULT_OID) {
                return folderManager.save(folder);
            } else {
                folderManager.update(folder);
                return folder.getOid();
            }
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

    private long throwit(Folder folder, String msg, final Object param) throws SaveException, UpdateException {
        if (folder.getOid() == Folder.DEFAULT_OID) {
            throw new SaveException(MessageFormat.format(msg, param));
        } else {
            throw new UpdateException(MessageFormat.format(msg, param));
        }
    }
}
