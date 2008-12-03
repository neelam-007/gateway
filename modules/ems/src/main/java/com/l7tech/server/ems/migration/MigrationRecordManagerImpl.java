package com.l7tech.server.ems.migration;

import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.SaveException;
import com.l7tech.objectmodel.FindException;
import com.l7tech.server.HibernateEntityManager;
import com.l7tech.server.ems.enterprise.SsgCluster;

import java.util.Date;
import java.util.Collection;

import org.hibernate.criterion.Restrictions;
import org.hibernate.criterion.Criterion;
import org.springframework.transaction.annotation.Transactional;

/**
 * @Copyright: Layer 7 Tech. Inc.
 * @Author: ghuang
 * @Date: Nov 19, 2008
 */
@Transactional(rollbackFor=Throwable.class)
public class MigrationRecordManagerImpl extends HibernateEntityManager<Migration, EntityHeader> implements MigrationRecordManager {

    //- PUBLIC

    @Override
    public Class<Migration> getImpClass() {
        return Migration.class;
    }

    @Override
    public Class<Migration> getInterfaceClass() {
        return Migration.class;
    }

    @Override
    public String getTableName() {
        return "migration";
    }

    @Override
    public Migration create( final String name, 
                             final long timeCreated,
                             final SsgCluster source,
                             final SsgCluster destination,
                             final String summary) throws SaveException {
        Migration result = new Migration(name, timeCreated, source, destination, summary);
        super.save(result);
        return result;
    }

    @Override
    public Collection<Migration> findPage( final SortProperty sortProperty,
                                           final boolean ascending,
                                           final int offset,
                                           final int count,
                                           final Date start,
                                           final Date end) throws FindException {
        Criterion criterion = Restrictions.between("timeCreated", start.getTime(), end.getTime());
        return super.findPage(null, sortProperty.getPropertyName(), ascending, offset, count, criterion);
    }

    @Override
    public int findCount( final Date start,
                          final Date end ) throws FindException {
        Criterion criterion = Restrictions.between("timeCreated", start.getTime(), end.getTime());
        return super.findCount( criterion );
    }

    //- PROTECTED

    @Override
    protected UniqueType getUniqueType() {
        return UniqueType.NONE;
    }
}
