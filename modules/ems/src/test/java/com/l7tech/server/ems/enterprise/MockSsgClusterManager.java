package com.l7tech.server.ems.enterprise;

import com.l7tech.objectmodel.*;
import com.l7tech.server.EntityManagerStub;

import java.util.List;

/**
 * 
 */
public class MockSsgClusterManager extends EntityManagerStub<SsgCluster, EntityHeader> implements SsgClusterManager {

    @Override
    public SsgCluster create(String name, String sslHostName, int adminPort, EnterpriseFolder parentFolder) throws InvalidNameException, SaveException, FindException {
        return null;
    }

    @Override
    public SsgCluster create(String name, String sslHostName, int adminPort, String parentFolderGuid) throws FindException, InvalidNameException, SaveException {
        return null;
    }

    @Override
    public SsgCluster findByGuid(String guid) throws FindException {
        return null;
    }

    @Override
    public void editByGuid(String guid, String newName, String newSslHostname, String newAdminPort) throws FindException, UpdateException, DuplicateHostnameException {
    }

    @Override
    public void moveByGuid(String guid, String newParentGuid) throws FindException, UpdateException {
    }

    @Override
    public void deleteByGuid(String guid) throws FindException, DeleteException {
    }

    @Override
    public List<SsgCluster> findChildSsgClusters(String parentFolderGuid) throws FindException {
        return null;
    }

    @Override
    public List<SsgCluster> findChildSsgClusters(EnterpriseFolder parentFolder) throws FindException {
        return null;
    }

    @Override
    public List<EnterpriseFolder> findAllAncestors(String guid) throws FindException {
        return null;
    }

    @Override
    public List<EnterpriseFolder> findAllAncestors(SsgCluster ssgCluster) {
        return null;
    }
}
