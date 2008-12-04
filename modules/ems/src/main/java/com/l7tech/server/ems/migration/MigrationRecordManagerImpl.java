package com.l7tech.server.ems.migration;

import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.SaveException;
import com.l7tech.objectmodel.FindException;
import com.l7tech.server.HibernateEntityManager;
import com.l7tech.server.ems.enterprise.SsgCluster;
import com.l7tech.identity.User;

import java.util.Date;
import java.util.Collection;
import java.util.List;
import java.util.ArrayList;

import org.hibernate.criterion.Restrictions;
import org.hibernate.criterion.Criterion;
import org.springframework.transaction.annotation.Transactional;

/**
 * @Copyright: Layer 7 Tech. Inc.
 * @Author: ghuang
 * @Date: Nov 19, 2008
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
                                   final SsgCluster source,
                                   final SsgCluster target,
                                   final String summary,
                                   final byte[] data ) throws SaveException {

        MigrationRecord result = new MigrationRecord( name, System.currentTimeMillis(), user, source, target, summary, data );
        long oid = super.save(result);
        result.setOid( oid );
        return result;
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
        return super.findPage(null, sortProperty.getPropertyName(), ascending, offset, count, asCriterion( user, start, end ));
    }

    @Override
    public int findCount( final User user,
                          final Date start,
                          final Date end ) throws FindException {
        return super.findCount( asCriterion( user, start, end ) );
    }

    //- PROTECTED

    @Override
    protected UniqueType getUniqueType() {
        return UniqueType.NONE;
    }

    //- PRIVATE

    private Criterion[] asCriterion( final User user, final Date start, final Date end ) {
        List<Criterion> criterion = new ArrayList<Criterion>();

        if ( user != null ) {
            criterion.add( Restrictions.eq("provider", user.getProviderId()) );
            criterion.add( Restrictions.eq("userId", user.getId()) );
        }

        if ( start != null && end != null ) {
            criterion.add( Restrictions.between("timeCreated", start.getTime(), end.getTime()) );
        }

        return criterion.toArray( new Criterion[criterion.size()] );
    }

}
