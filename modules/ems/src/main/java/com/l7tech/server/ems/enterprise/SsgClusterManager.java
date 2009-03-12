package com.l7tech.server.ems.enterprise;

import com.l7tech.objectmodel.*;

import java.util.List;
import java.net.UnknownHostException;

/**
 * @since Enterprise Manager 1.0
 * @author rmak
 */
public interface SsgClusterManager extends EntityManager<SsgCluster, EntityHeader> {

    SsgCluster create(String name, String sslHostName, int adminPort, EnterpriseFolder parentFolder) throws InvalidNameException, SaveException, FindException, UnknownHostException;
    SsgCluster create(String name, String sslHostName, int adminPort, String parentFolderGuid) throws FindException, InvalidNameException, SaveException, UnknownHostException;


    /**
     * Find the cluster with the given guid.
     * @param guid: the guid of the cluster
     * @return the cluster with the given guid.
     * @throws FindException
     */
    SsgCluster findByGuid(final String guid) throws FindException;

    /**
     * Edit the cluster by changing the name, the ssl hostname, or the admin port of the cluster with the guid.
     *
     * @param guid              GUID of the cluster
     * @param newName           the new name of the cluster
     * @param newSslHostname    the new SSL hostname of the cluster
     * @param newAdminPort      the new admin port used by the cluster
     *
     * @throws FindException: thrown if failed to determine if a folder with <code>guid</code> exists
     * @throws UpdateException: thrown if the new folder cannot be persisted
     * @throws DuplicateHostnameException: thrown if the new hostname is the same as the hostname of one other cluster.
     * @throws UnknownHostException: thrown if a checked host name is not recognizable.
     */
    void editByGuid(String guid, String newName, String newSslHostname, String newAdminPort) throws FindException, UpdateException, DuplicateHostnameException, UnknownHostException;

    /**
     * Moves an SSG Cluster into a different parent folder.
     * No action or exception if no change in parent folder.
     *
     * @param guid              GUID of the SSG Cluster to move
     * @param newParentGuid     GUID of the destination parent folder
     * @throws FindException if no SSG Cluster or folder with the given GUIDs
     * @throws UpdateException if name collision in the destination folder,
     *                         or database error
     */
    void moveByGuid(String guid, String newParentGuid) throws FindException, UpdateException;

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

    /**
     * Find all ancestors (i.e., enterprise folders) of the SSG cluster
     * @param guid: the GUID of the SSG Cluster
     * @return a list of enterprise folder objects (Note: the root ancestor will be the first element in the list.)
     */
    List<EnterpriseFolder> findAllAncestors(final String guid) throws FindException;

    /**
     * Find all ancestors (i.e., enterprise folders) of the SSG cluster
     * @param ssgCluster: the SSG Cluster object
     * @return a list of enterprise folder objects (Note: the root ancestor will be the first element in the list.)
     */
    List<EnterpriseFolder> findAllAncestors(final SsgCluster ssgCluster);
}
