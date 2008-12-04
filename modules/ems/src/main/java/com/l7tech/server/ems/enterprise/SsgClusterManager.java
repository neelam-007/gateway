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
     * Find the cluster with the given guid.
     * @param guid: the guid of the cluster
     * @return the cluster with the given guid.
     * @throws FindException
     */
    SsgCluster findByGuid(final String guid) throws FindException;

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

    /**
     * Find all children of a given parent folder guid.
     *
     * @param parentFolderGuid The guid of the parent folder
     * @return a list of children of the parent folder
     * @throws FindException if failed to query database
     */
    List<SsgCluster> findChildSsgClusters(String parentFolderGuid) throws FindException;

    /**
     * Find all children of a given parent folder.
     *
     * @param parentFolder: the parent folder
     * @return a list of children of the parent folder
     * @throws FindException if failed to query database
     */
    List<SsgCluster> findChildSsgClusters(final EnterpriseFolder parentFolder) throws FindException;
}
