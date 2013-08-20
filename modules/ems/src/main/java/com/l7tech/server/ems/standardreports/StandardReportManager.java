package com.l7tech.server.ems.standardreports;

import com.l7tech.identity.User;
import com.l7tech.objectmodel.DeleteException;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.GoidEntityManager;
import com.l7tech.server.ems.enterprise.SsgCluster;

import java.util.Collection;
import java.util.List;

/**
 *
 */
public interface StandardReportManager extends GoidEntityManager<StandardReport, EntityHeader> {

    /**
     * Find reports by page.
     *
     * @param user Optional user criteria (may be null)
     * @param sortProperty The property to sort results by
     * @param ascending True to sort in ascending order
     * @param offset The first result offset
     * @param count The maximumn number of results to return
     * @returns The list of matching reports (my be null but never empty)
     * @throws FindException if an error occurs
     */
    List<StandardReport> findPage( User user, String sortProperty, boolean ascending, int offset, int count ) throws FindException;

    /**
     * Find report count.
     *
     * @param user Optional user criteria (may be null)
     * @return The number of matching reports
     * @throws FindException if an error occurs
     */
    int findCount( final User user ) throws FindException;

    /**
     * Find reports by status.
     *
     * @param status The status of interest (should not be null)
     * @return collection of reports with the given status (may be empty but not null)
     * @throws FindException if an error occurs
     */
    Collection<StandardReport> findByStatus( String status ) throws FindException;

    /**
     * Delete all reports associated with the SSG cluster
     *
     * @param ssgCluster: a SSG cluster object to find the corresponding standard reports.
     * @throws DeleteException
     */
    void deleteBySsgCluster( SsgCluster ssgCluster ) throws DeleteException;
}
