package com.l7tech.server.ems.migration;

import com.l7tech.objectmodel.*;
import com.l7tech.server.ems.enterprise.SsgCluster;
import com.l7tech.server.management.migration.bundle.MigrationBundle;
import com.l7tech.identity.User;
import com.l7tech.util.Functions;
import com.l7tech.util.Pair;

import java.util.Collection;
import java.util.Date;

/**
 * This class manages the data access of Migration Entities.
 *
 * @author ghuang
 */
public interface MigrationRecordManager extends EntityManager<MigrationRecord, EntityHeader> {

    enum SortProperty {
        NAME("name"), TIME("timeCreated");
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
     * @param sourceCluster The source SSG Cluster
     * @param targetCluster The target SSG Cluster
     * @param summary The migration summary.
     * @param bundle The migration summary.
     * @return a new migration record.
     * @throws SaveException if an error occurs
     */
    MigrationRecord create( final String name,
                            final User user,
                            final SsgCluster sourceCluster,
                            final SsgCluster targetCluster,
                            final MigrationSummary summary,
                            final MigrationBundle bundle ) throws SaveException;

    /**
     * Creates / restores a migration record from a byte array representation.
     *
     * @param user The user for the migration (used with offline migrations)
     * @param label An optional new label name to be given to the new record; if not null or empty, it overwrites the label in the data payload.
     * @param data  The serialized migration record.
     * @param clusterCallback Callback to create or validate the source/destination cluster
     */
    MigrationRecord create( User user, String label, byte[] data, Functions.TernaryThrows<Pair<SsgCluster,SsgCluster>,String,String,String,SaveException> clusterCallback) throws SaveException;

    /**
     * Find how many migration records are dated between "start" and "end".
     * @param start: the start date
     * @param end: the end date
     * @return: an integer - how many migration records satisfy the date constrain (between "start" and "end".)
     * @throws FindException
     */
    int findCount(final User user, final Date start, final Date end) throws FindException;

    /**
     * Find a MigrationRecord by primary key. The bundleXml property will not be set.
     *
     * @param goid primary key
     * @return MigrationRecord with all properties apart from bundleXml set.
     * @throws FindException if cannot be found.
     */
    MigrationRecord findByPrimaryKeyNoBundle(Goid goid) throws FindException;

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
     * @return The matching migration records. The bundleXml property will not be set.
     * @throws FindException If an error occurs
     */
    Collection<MigrationRecord> findPage(final User user, final SortProperty sortProperty, final boolean ascending, final int offset, final int count, final Date start, final Date end) throws FindException;


    /**
     * Find labelled migration record headers with the given limits.
     *
     * @param user The user to access migrations for (null for all users)
     * @param count The number of items to return
     * @param start The start date
     * @param end The end date
     * @return The matching migration records. The bundleXml property will not be set.
     * @throws FindException If an error occurs
     */
    Collection<MigrationRecord> findNamedMigrations(final User user, final int count, final Date start, final Date end) throws FindException;

    /**
     * Delete migration records associated with the SSG cluster, which may be the source cluster or the target cluster.
     *
     * @param ssgCluster: a SSG cluater object to find all corresponding migration records.
     * @throws DeleteException
     */
    void deleteBySsgCluster(final SsgCluster ssgCluster) throws DeleteException;
}
