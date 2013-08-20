package com.l7tech.server.ems.migration;

import com.l7tech.identity.User;
import com.l7tech.objectmodel.*;
import com.l7tech.server.HibernateGoidEntityManager;
import com.l7tech.server.ems.enterprise.SsgCluster;
import com.l7tech.server.management.migration.bundle.MigrationBundle;
import com.l7tech.server.util.ReadOnlyHibernateCallback;
import com.l7tech.util.Functions;
import com.l7tech.util.HexUtils;
import com.l7tech.util.Pair;
import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.criterion.*;
import org.hibernate.transform.Transformers;
import org.springframework.dao.DataAccessException;
import org.springframework.orm.hibernate3.HibernateCallback;
import org.springframework.transaction.annotation.Transactional;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

/**
 * @author ghuang
 */
@Transactional(rollbackFor=Throwable.class)
public class MigrationRecordManagerImpl extends HibernateGoidEntityManager<MigrationRecord, EntityHeader> implements MigrationRecordManager {

    //- PUBLIC

    @Override
    public Class<MigrationRecord> getImpClass() {
        return MigrationRecord.class;
    }

    @Override
    public Class<MigrationRecord> getInterfaceClass() {
        return MigrationRecord.class;
    }

    @Override
    public String getTableName() {
        return "migration";
    }

    @Override
    public MigrationRecord create( final String name,
                                   final User user,
                                   final SsgCluster sourceCluster,
                                   final SsgCluster targetCluster,
                                   final MigrationSummary summary,
                                   final MigrationBundle bundle ) throws SaveException {

        MigrationRecord result = new MigrationRecord( name, user, sourceCluster, targetCluster, summary, bundle );
        Goid goid = super.save(result);
        result.setGoid(goid);
        return result;
    }

    @Override
    public MigrationRecord create( final User user,
                                   final String label,
                                   final byte[] data,
                                   final Functions.TernaryThrows<Pair<SsgCluster,SsgCluster>,String,String,String,SaveException> clusterCallback ) throws SaveException {

        final MigrationRecord record;
        try {
            record = MigrationRecord.deserializeXml(HexUtils.decodeUtf8(data));
        } catch (Exception e) {
            throw new SaveException("Error loading migration bundle data.", e);
        }

        final Pair<SsgCluster,SsgCluster> clusterPair =
                clusterCallback.call(record.getSourceClusterGuid(), record.getSourceClusterName(), record.getTargetClusterGuid());

        record.setGoid( MigrationRecord.DEFAULT_GOID );
        record.setVersion( 0 );
        record.setName( label );
        record.setSourceCluster( clusterPair.left );
        record.setTargetCluster( clusterPair.right );
        record.calculateSize();

        if ( record.getSourceCluster().isOffline() ) {
            record.setProvider( user.getProviderId() );
            record.setUserId( user.getId() );
        }

        Goid goid = super.save(record);
        record.setGoid( goid );
        return record;
    }

    @Override
    public Collection<MigrationRecord> findPage(
                                           final User user,
                                           final SortProperty sortProperty,
                                           final boolean ascending,
                                           final int offset,
                                           final int count,
                                           final Date start,
                                           final Date end) throws FindException {
        return findRecords(user, sortProperty, ascending, offset, count, start, end, false);
    }

    @Override
    public Collection<MigrationRecord> findNamedMigrations(
                                           final User user,
                                           final int count,
                                           final Date start,
                                           final Date end) throws FindException {


        return findRecords(user, SortProperty.NAME, true, 0, count, start, end, true);

    }

    @Override
    public MigrationRecord findByPrimaryKeyNoBundle(final Goid goid) throws FindException {
        try {
            return getHibernateTemplate().execute(new ReadOnlyHibernateCallback<MigrationRecord>() {
                @SuppressWarnings({ "unchecked" })
                @Override
                public MigrationRecord doInHibernateReadOnly(Session session) throws HibernateException, SQLException {
                    final Criteria criteria = session.createCriteria(getImpClass(), "m");
                    criteria.setProjection(getMigrationRecordNoBundleProjection());
                    criteria.add( Restrictions.eq("goid", goid) );

                    criteria.setResultTransformer(Transformers.aliasToBean(MigrationRecord.class));

                    final Object o = criteria.uniqueResult();
                    return (MigrationRecord) o;
                }
            });
        } catch (Exception e) {
            throw new FindException(e.toString(), e);
        }

    }

    private Collection<MigrationRecord> findRecords(
            final User user,
            final SortProperty sortProperty,
            final boolean ascending,
            final int offset,
            final int count,
            final Date start,
            final Date end,
            final boolean requireName) {
        return getHibernateTemplate().execute(new ReadOnlyHibernateCallback<List<MigrationRecord>>() {
            @SuppressWarnings({"unchecked"})
            @Override
            protected List<MigrationRecord> doInHibernateReadOnly(Session session) throws HibernateException, SQLException {
                final Criteria criteria = session.createCriteria(getImpClass(), "m");

                final ProjectionList projectionList = getMigrationRecordNoBundleProjection();

                criteria.setProjection(projectionList);

                final Criterion[] criterions = asCriterion(user, start, end, requireName);
                for (Criterion criterion : criterions) {
                    criteria.add(criterion);
                }

                if (ascending) {
                    criteria.addOrder(Order.asc("m." + sortProperty.getPropertyName()));
                } else {
                    criteria.addOrder(Order.desc("m." + sortProperty.getPropertyName()));
                }

                criteria.setFirstResult(offset);
                criteria.setMaxResults(count);

                criteria.setResultTransformer(Transformers.aliasToBean(MigrationRecord.class));

                return (List<MigrationRecord>) criteria.list();
            }
        });
    }

    /**
     * The alias prefix used is 'm', any restrictions added must use this prefix.
     * @return MigrationRecord projection with out bundleXml property.
     */
    private ProjectionList getMigrationRecordNoBundleProjection() {
        return Projections.projectionList()
                .add(Property.forName("m.oid"), "oid")
                .add(Property.forName("m.name"), "name")
                .add(Property.forName("m.version"), "version")
                .add(Property.forName("m.timeCreated"), "timeCreated")
                .add(Property.forName("m.provider"), "provider")
                .add(Property.forName("m.userId"), "userId")
                .add(Property.forName("m.targetCluster"), "targetCluster")
                .add(Property.forName("m.sourceCluster"), "sourceCluster")
                .add(Property.forName("m.summaryXml"), "summaryXml")
                // bundle is excluded from this projection
                .add(Property.forName("m.size"), "size");
    }

    @Override
    public void deleteBySsgCluster(final SsgCluster ssgCluster) throws DeleteException {
        try {
            getHibernateTemplate().execute(new HibernateCallback<Void>() {
                @SuppressWarnings({"unchecked"})
                @Override
                public Void doInHibernate(Session session) throws HibernateException, SQLException {
                    final Criteria criteria = session.createCriteria(getImpClass());
                    criteria.add(Restrictions.or(Restrictions.eq("sourceCluster", ssgCluster), Restrictions.eq("targetCluster", ssgCluster)));

                    for (MigrationRecord record: (Collection <MigrationRecord>)criteria.list()) {
                        session.delete(record);
                    }

                    return null;
                }
            });
        } catch (DataAccessException e) {
            throw new DeleteException("Couldn't delete Migration Record", e);
        }
    }

    @Override
    public int findCount( final User user,
                          final Date start,
                          final Date end ) throws FindException {
        return super.findCount( null, asCriterion( user, start, end, false ) );
    }

    //- PROTECTED

    @Override
    protected UniqueType getUniqueType() {
        return UniqueType.NONE;
    }

    //- PRIVATE

    private Criterion[] asCriterion( final User user, final Date start, final Date end, final boolean requireName ) {
        List<Criterion> criterion = new ArrayList<Criterion>();

        if ( user != null ) {
            criterion.add( Restrictions.eq("provider", user.getProviderId()) );
            criterion.add( Restrictions.eq("userId", user.getId()) );
        }

        if ( start != null && end != null ) {
            criterion.add( Restrictions.between("timeCreated", start.getTime(), end.getTime()) );
        }

        if ( requireName ) {
            criterion.add( Restrictions.isNotNull("name") );
            criterion.add( Restrictions.ne("name", ""));
        }

        return criterion.toArray( new Criterion[criterion.size()] );
    }

}
