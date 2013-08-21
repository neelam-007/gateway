package com.l7tech.server.ems.monitoring;

import com.l7tech.objectmodel.DeleteException;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.EntityManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * This class manages the notification rules setup for each SSG Cluster.
 *
 * @Copyright: Layer 7 Tech. Inc.
 * @Author: ghuang
 * @Date: Feb 7, 2009
 * @since Enterprise Manager 1.0
 */
@Transactional(propagation= Propagation.REQUIRED, rollbackFor=Throwable.class)
public interface SsgClusterNotificationSetupManager extends EntityManager<SsgClusterNotificationSetup, EntityHeader> {
    /**
     * Find the notification rules setup by the given SSG Cluster guid.
     * @param ssgClusterGuid: the GUID of a SSG Cluster
     * @return a SsgClusterNotificationSetup object.
     * @throws FindException if there is a data access error.
     */
    SsgClusterNotificationSetup findByEntityGuid(String ssgClusterGuid) throws FindException;

    /**
     * Delete the notification setup assoicated with the SSG cluster.
     * @param guid: the GUID of the SSG cluster.
     * @throws FindException: thrown when an error in finding a notification setup.
     * @throws DeleteException: thrown when an error in deleting a notification setup.
     */
    void deleteBySsgClusterGuid(String guid) throws FindException, DeleteException;
}
