package com.l7tech.server.ems.migration;

import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityManager;
import com.l7tech.objectmodel.SaveException;
import com.l7tech.objectmodel.FindException;
import com.l7tech.server.ems.enterprise.SsgCluster;
import com.l7tech.identity.User;

import java.util.Collection;
import java.util.Date;

/**
 * This class manages the data access of Migration Entities.
 *
 * @Copyright: Layer 7 Tech. Inc.
 * @Author: ghuang
 * @Date: Nov 19, 2008
 */
public interface MigrationRecordManager extends EntityManager<MigrationRecord, EntityHeader> {

    enum SortProperty {
        NAME("name"), TIME("timeCreated"), FROM("sourceCluster"), TO("destinationCluster");
        private final String name;
        SortProperty( String name ) {
            this.name = name;
        }
        String getPropertyName() {
            return name;
        }
    }

    /**
     * Create a a new migration record.
     *
     * @param name The name for the migration (may be null)
     * @param user The user performing the migration
     * @param source The source cluster.
     * @param destination The destination cluster.
     * @param summary The migration summary.
     * @param data The migration summary.
     * @return a new migration record.
     * @throws SaveException if an error occurs
     */
    MigrationRecord create( final String name, final User user, final SsgCluster source, final SsgCluster destination, final String summary, final byte[] data ) throws SaveException;

    /**
     * Find how many migration records are dated between "start" and "end".
     * @param start: the start date
     * @param end: the end date
     * @return: an integer - how many migration records satisfy the date constrain (between "start" and "end".)
     * @throws FindException
     */
    int findCount(final User user, final Date start, final Date end) throws FindException;

    /**
     * Find a "page" worth of migrations for the given sort, offset, count, start, and end.
     *
     * @param user The user to access migrations for (null for all users)
     * @param sortProperty The property to sort by (e.g. name)
     * @param ascending True to sort in ascending order
     * @param offset The initial offset 0 for the first page
     * @param count The number of items to return
     * @param start The start date
     * @param end The end date
     * @throws FindException If an error occurs
     */
    Collection<MigrationRecord> findPage(final User user, final SortProperty sortProperty, final boolean ascending, final int offset, final int count, final Date start, final Date end) throws FindException;
}
