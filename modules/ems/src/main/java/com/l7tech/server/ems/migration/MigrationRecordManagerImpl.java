package com.l7tech.server.ems.migration;

import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.SaveException;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.DeleteException;
import com.l7tech.server.HibernateEntityManager;
import com.l7tech.server.management.migration.bundle.MigrationBundle;
import com.l7tech.server.ems.enterprise.SsgCluster;
import com.l7tech.identity.User;
import com.l7tech.server.util.ReadOnlyHibernateCallback;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.Functions;
import com.l7tech.util.HexUtils;

import java.lang.reflect.Constructor;
import java.util.*;
import java.sql.SQLException;

import com.l7tech.util.Pair;
import org.hibernate.criterion.*;
import org.hibernate.Session;
import org.hibernate.HibernateException;
import org.hibernate.Criteria;
import org.hibernate.transform.AliasToBeanConstructorResultTransformer;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.orm.hibernate3.HibernateCallback;
import org.springframework.dao.DataAccessException;

/**
 * @author ghuang
 */
@Transactional(rollbackFor=Throwable.class)
public class MigrationRecordManagerImpl extends HibernateEntityManager<MigrationRecord, EntityHeader> implements MigrationRecordManager {

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
        long oid = super.save(result);
        result.setOid( oid );
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

        record.setOid( MigrationRecord.DEFAULT_OID );
        record.setVersion( 0 );
        record.setName( label );
        record.setSourceCluster( clusterPair.left );
        record.setTargetCluster( clusterPair.right );
        record.calculateSize();

        if ( record.getSourceCluster().isOffline() ) {
            record.setProvider( user.getProviderId() );
            record.setUserId( user.getId() );
        }

        long oid = super.save(record);
        record.setOid( oid );
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
        return super.findPage(null, sortProperty.getPropertyName(), ascending, offset, count, asCriterion( user, start, end, false ));
    }

    @Override
    public Collection<MigrationRecordHeader> findNamedMigrations(
                                           final User user,
                                           final int count,
                                           final Date start,
                                           final Date end) throws FindException {


        return getHibernateTemplate().execute(new ReadOnlyHibernateCallback<List<MigrationRecordHeader>>() {
            @SuppressWarnings({"unchecked"})
            @Override
            protected List<MigrationRecordHeader> doInHibernateReadOnly(Session session) throws HibernateException, SQLException {
                final Criteria criteria = session.createCriteria(getImpClass());

                final ProjectionList projectionList = Projections.projectionList()
                        .add(Property.forName("oid"))
                        .add(Property.forName("name"))
                        .add(Property.forName("version"));

                criteria.setProjection(projectionList);

                final Criterion[] criterions = asCriterion(user, start, end, true);
                for (Criterion criterion : criterions) {
                    criteria.add(criterion);
                }

                criteria.addOrder(Order.asc("name"));

                criteria.setFirstResult(0);
                criteria.setMaxResults(count);

                final Constructor<MigrationRecordHeader> constructor;
                try {
                    constructor = MigrationRecordHeader.class.getConstructor(Long.TYPE, String.class, Integer.TYPE);
                } catch (NoSuchMethodException e) {
                    // unexpected - coding error
                    throw new HibernateException(ExceptionUtils.getMessage(e));
                }
                criteria.setResultTransformer(new AliasToBeanConstructorResultTransformer(constructor));

                return (List<MigrationRecordHeader>)criteria.list();
            }
        });

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
