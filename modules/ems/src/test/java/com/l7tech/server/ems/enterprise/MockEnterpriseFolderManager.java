package com.l7tech.server.ems.enterprise;

import com.l7tech.gateway.common.security.rbac.Role;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.SaveException;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.UpdateException;
import com.l7tech.objectmodel.DeleteException;
import com.l7tech.server.EntityManagerStub;

import java.util.List;

/**
 *
 */
public class MockEnterpriseFolderManager extends EntityManagerStub<EnterpriseFolder,EntityHeader> implements EnterpriseFolderManager {
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
