package com.l7tech.server.ems.standardreports;

import com.l7tech.objectmodel.EntityManager;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.FindException;
import com.l7tech.identity.User;

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
}
