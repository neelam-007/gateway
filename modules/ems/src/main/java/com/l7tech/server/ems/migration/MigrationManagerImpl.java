package com.l7tech.server.ems.migration;

import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.Entity;
import com.l7tech.objectmodel.SaveException;
import com.l7tech.objectmodel.FindException;
import com.l7tech.server.HibernateEntityManager;
import com.l7tech.server.ems.enterprise.SsgCluster;

import java.util.Date;
import java.util.Collection;

import org.hibernate.criterion.Restrictions;
import org.hibernate.criterion.Criterion;

/**
 * @Copyright: Layer 7 Tech. Inc.
 * @Author: ghuang
 * @Date: Nov 19, 2008
 */
public class MigrationManagerImpl extends HibernateEntityManager<Migration, EntityHeader> implements MigrationManager {
    public Class<? extends Entity> getImpClass() {
        return Migration.class;
    }

    public Class<? extends Entity> getInterfaceClass() {
        return Migration.class;
    }

    public String getTableName() {
        return "migration";
    }

    public Migration create(String name, long timeCreated, SsgCluster source, SsgCluster destination, String summary) throws SaveException {
        Migration result = new Migration(name, timeCreated, source, destination, summary);
        super.save(result);
        return result;
    }

    public Collection<Migration> findPage(SortProperty sortProperty, boolean ascending, int offset, int count, Date start, Date end) throws FindException {
        Criterion criterion = Restrictions.between("timeCreated", start.getTime(), end.getTime());
        return super.findPage(null, sortProperty.getPropertyName(), ascending, offset, count, new Criterion[]{criterion});
    }

    public int findCount(Date start, Date end) throws FindException {
        Criterion criterion = Restrictions.between("timeCreated", start.getTime(), end.getTime());
        return super.findCount(new Criterion[]{criterion});
    }

    @Override
    protected UniqueType getUniqueType() {
        return UniqueType.NONE;
    }
}
