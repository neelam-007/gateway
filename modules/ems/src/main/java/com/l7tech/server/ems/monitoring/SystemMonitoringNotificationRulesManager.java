package com.l7tech.server.ems.monitoring;

import com.l7tech.objectmodel.DeleteException;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.GoidEntityManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * @Copyright: Layer 7 Tech. Inc.
 * @Author: ghuang
 * @Date: Feb 3, 2009
 * @since Enterprise Manager 1.0
 */
@Transactional(propagation= Propagation.REQUIRED, rollbackFor=Throwable.class)
public interface SystemMonitoringNotificationRulesManager extends GoidEntityManager<SystemMonitoringNotificationRule, EntityHeader> {
    /**
     * Find a notification rule by the given guid.
     * @param guid: the guid of a notification rule to be found.
     * @return a notification rule object.
     * @throws FindException if an error occurs when retrieving a notification rule from the DB.
     */
    SystemMonitoringNotificationRule findByGuid(final String guid) throws FindException;

    /**
     * Delete a notification rule by the given guid.
     * @param guid: the guid of a notification rule to be deleted.
     * @throws FindException if an error occurs when retrieving a notification rule from the DB.
     * @throws DeleteException if an error occurs when deleting a notification rule from the DB.
     */
    void deleteByGuid(String guid) throws FindException, DeleteException;
}
