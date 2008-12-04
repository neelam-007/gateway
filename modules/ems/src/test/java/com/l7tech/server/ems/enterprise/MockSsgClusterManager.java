package com.l7tech.server.ems.enterprise;

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
    public void renameByGuid(String name, String guid) throws FindException, UpdateException {
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
}
