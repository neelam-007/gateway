package com.l7tech.server.ems.enterprise;

import com.l7tech.objectmodel.*;

import java.util.List;

/**
 * @since Enterprise Manager 1.0
 * @author rmak
 */
public interface SsgClusterManager extends EntityManager<SsgCluster, EntityHeader> {

    SsgCluster create(String name, String sslHostName, int adminPort, EnterpriseFolder parentFolder) throws InvalidNameException, SaveException, FindException;
    SsgCluster create(String name, String sslHostName, int adminPort, String parentFolderGuid) throws FindException, InvalidNameException, SaveException;

    /**
     * Rename the name of the cluster with the guid.
     * @param name: the new name of the cluster
     * @param guid: the guid of the cluster
     * @throws FindException if failed to determine if a folder with <code>parentFolderGuid</code> exists
     * @throws InvalidNameException if <code>name</code> does not conform to name rules
     * @throws SaveException if the new folder cannot be persisted
     */
    void renameByGuid(String name, String guid) throws FindException, UpdateException;

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
