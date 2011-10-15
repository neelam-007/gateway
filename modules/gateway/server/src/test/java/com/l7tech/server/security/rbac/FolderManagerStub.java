/**
 * Copyright (C) 2008 Layer 7 Technologies Inc.
 */
package com.l7tech.server.security.rbac;

import com.l7tech.objectmodel.folder.Folder;
import com.l7tech.objectmodel.folder.FolderHeader;
import com.l7tech.objectmodel.Entity;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.SaveException;
import com.l7tech.server.EntityManagerStub;
import com.l7tech.server.folder.FolderManager;

/** @author alex */

/**
 * TODO [steve] move to com.l7tech.server.folder when development in logging branch is complete
 */
public class FolderManagerStub extends EntityManagerStub<Folder, FolderHeader> implements FolderManager {

    public FolderManagerStub() {
        super();
    }

    public FolderManagerStub( final Folder... entitiesIn ) {
        super( entitiesIn ); 
    }

    @Override
    public Class<? extends Entity> getImpClass() {
        return Folder.class;
    }

    @Override
    public Folder findRootFolder() throws FindException {
        return null;
    }

    @Override
    public void addManageFolderRole(Folder folder) throws SaveException {
    }

    @Override
    public void addReadonlyFolderRole(Folder folder) throws SaveException {
    }
}
