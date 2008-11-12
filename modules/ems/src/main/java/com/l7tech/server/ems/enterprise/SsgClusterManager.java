package com.l7tech.server.ems.enterprise;

import com.l7tech.objectmodel.*;

import java.util.List;

/**
 * @since Enterprise Manager 1.0
 * @author rmak
 */
public interface SsgClusterManager extends EntityManager<SsgCluster, EntityHeader> {

    SsgCluster create(String name, String sslHostName, int adminPort, EnterpriseFolder parentFolder) throws InvalidNameException, SaveException;
    SsgCluster create(String name, String sslHostName, int adminPort, String parentFolderGuid) throws FindException, InvalidNameException, SaveException;

    /**
     * Deletes an SSG Cluster with the given GUID.
     *
     * @param guid      GUID of the SSG Cluster
     * @throws FindException if no SSG Cluster with such GUID
     * @throws DeleteException if database delete failed
     */
    void deleteByGuid(String guid) throws FindException, DeleteException;

    List<SsgCluster> findChildSsgClusters(final EnterpriseFolder parentFolder) throws FindException;
}
