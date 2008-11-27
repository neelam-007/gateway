package com.l7tech.server.ems.standardreports;

import com.l7tech.server.HibernateEntityManager;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.FindException;
import com.l7tech.identity.User;

import java.util.List;

import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Restrictions;

/**
 * 
 */
public class StandardReportManagerImpl  extends HibernateEntityManager<StandardReport, EntityHeader> implements StandardReportManager {

    //- PUBLIC

    @Override
    public List<StandardReport> findPage( final User user, String sortProperty, boolean ascending, int offset, int count) throws FindException {
        return findPage( getInterfaceClass(), sortProperty, ascending, offset, count,  asCriterion(user)  );
    }

    @Override
    public int findCount( final User user ) throws FindException {
        return findCount( asCriterion(user) );
    }

    @Override
    public Class<StandardReport> getImpClass() {
        return StandardReport.class;
    }

    @Override
    public Class<StandardReport> getInterfaceClass() {
        return StandardReport.class;
    }

    @Override
    public String getTableName() {
        return "report";
    }

    //- PRIVATE

    private Criterion[] asCriterion( final User user ) {
        Criterion[] criterion;

        if ( user == null ) {
            criterion = new Criterion[0];
        } else {
            criterion = new Criterion[2];
            criterion[0] = Restrictions.eq("provider", user.getProviderId());
            criterion[1] = Restrictions.eq("userId", user.getId());
        }

        return criterion;
    }
}
