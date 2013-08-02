package com.l7tech.server.ems.enterprise;

import com.l7tech.objectmodel.*;
import com.l7tech.server.OidEntityManagerStub;

import java.util.List;

/**
 *
 */
public class MockEnterpriseFolderManager extends OidEntityManagerStub<EnterpriseFolder,EntityHeader> implements EnterpriseFolderManager {
    @Override
    public EnterpriseFolder create(String name, EnterpriseFolder parentFolder) throws InvalidNameException, SaveException, FindException {
        return null;
    }

    @Override
    public EnterpriseFolder create(String name, String parentFolderGuid) throws FindException, InvalidNameException, SaveException {
        return null;
    }

    @Override
    public void renameByGuid(String name, String guid) throws FindException, UpdateException {
    }

    @Override
    public void moveByGuid(String guid, String newParentGuid) throws FindException, UpdateException {
    }

    @Override
    public void deleteByGuid(String guid) throws FindException, DeleteException {
    }

    @Override
    public void deleteByGuid(String guid, boolean deleteByCascade) throws FindException, DeleteException {
    }

    @Override
    public EnterpriseFolder findRootFolder() throws FindException {
        return null;
    }

    @Override
    public EnterpriseFolder findByGuid(String guid) throws FindException {
        return null;
    }

    @Override
    public List<EnterpriseFolder> findChildFolders(String parentFolderGuid) throws FindException {
        return null;
    }

    @Override
    public List<EnterpriseFolder> findChildFolders(EnterpriseFolder parentFolder) throws FindException {
        return null;
    }
}
