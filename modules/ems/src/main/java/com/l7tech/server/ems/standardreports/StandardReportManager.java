package com.l7tech.server.ems.standardreports;

import com.l7tech.objectmodel.EntityManager;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.DeleteException;
import com.l7tech.identity.User;
import com.l7tech.server.ems.enterprise.SsgCluster;

import java.util.List;

/**
 *
 */
public interface StandardReportManager extends EntityManager<StandardReport, EntityHeader> {

    /**
     * Find reports by page.
     */
    List<StandardReport> findPage( final User user, final String sortProperty, final boolean ascending, final int offset, final int count ) throws FindException;

    /**
     * Find report count.
     */
    int findCount( final User user ) throws FindException;

    /**
     * Delete all reports associated with the SSG cluster
     *
     * @param ssgCluster: a SSG cluster object to find the corresponding standard reports.
     * @throws DeleteException
     */
    void deleteBySsgCluster(final SsgCluster ssgCluster) throws DeleteException;
}
