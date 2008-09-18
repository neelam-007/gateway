/**
 * Copyright (C) 2008, Layer 7 Technologies Inc.
 * User: darmstrong
 * Date: Sep 9, 2008
 */
package com.l7tech.server.folder;

import com.l7tech.gateway.common.admin.FolderAdmin;
import com.l7tech.objectmodel.*;
import com.l7tech.objectmodel.folder.FolderHeader;
import com.l7tech.objectmodel.folder.Folder;
import com.l7tech.server.ServerConfig;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.text.MessageFormat;

public class FolderAdminImpl implements FolderAdmin {

    private final Logger logger = Logger.getLogger(getClass().getName());

    private final FolderManager folderManager;
    private static final int MAX_FOLDER_NAME_LENGTH = 128;
    
    public FolderAdminImpl(FolderManager folderManager) {
        this.folderManager = folderManager;
    }

    public void deleteFolder(long oid) throws FindException, DeleteException {
        folderManager.delete(oid);
    }

    public Collection<FolderHeader> findAllFolders() throws FindException {
        return folderManager.findFolderHeaders();
    }

    public long saveFolder(Folder folder) throws UpdateException, SaveException {
        String name = folder.getName();
        if( name != null){
            if(name.length() > MAX_FOLDER_NAME_LENGTH){
                String msg = "Folder name cannot exceed {0} characters";
                if(folder.getOid() == Folder.DEFAULT_OID) {
                    throw new SaveException(MessageFormat.format(msg, MAX_FOLDER_NAME_LENGTH));
                }else{
                    throw new UpdateException(MessageFormat.format(msg, MAX_FOLDER_NAME_LENGTH));
                }
            }
        }
        
        int maxDepth = ServerConfig.getInstance().getIntProperty("policyorganization.maxFolderDepth",8);

        Long parentFolderId = folder.getParentFolderOid();
        if(parentFolderId != null){
            try {
                Folder parentFolder = folderManager.findByPrimaryKey(parentFolderId);
                long folderId = folder.getOid();
                if(parentFolderId == folderId){
                    throw new UpdateException("Parent folder cannot be the same as folder id");
                }

                //This will load all the folders parents
                int levels = 0;
                while(parentFolder != null){
                    levels++;
                    parentFolder = parentFolder.getParentFolder();
                }
                if(levels > maxDepth){
                    String msg = "Folder hierarchy can only be {0} levels deep";
                    logger.log(Level.INFO, msg, maxDepth);
                    if(folder.getOid() == Folder.DEFAULT_OID) {
                        throw new SaveException(MessageFormat.format(msg, maxDepth));
                    }else{
                        throw new UpdateException(MessageFormat.format(msg, maxDepth));
                    }
                }
            } catch (FindException e) {
                throw new SaveException("Could not find parent folder");
            }
        }

        if(folder.getOid() == Folder.DEFAULT_OID) {
            return folderManager.save(folder);
        } else {
            folderManager.update(folder);
            return folder.getOid();
        }

    }
}
