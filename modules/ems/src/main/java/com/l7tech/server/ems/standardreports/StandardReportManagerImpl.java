package com.l7tech.server.ems.standardreports;

import com.l7tech.gateway.common.security.rbac.OperationType;
import com.l7tech.gateway.common.security.rbac.Role;
import com.l7tech.identity.User;
import com.l7tech.objectmodel.*;
import com.l7tech.server.HibernateEntityManager;
import com.l7tech.server.ems.enterprise.SsgCluster;
import com.l7tech.server.security.rbac.RoleManager;
import com.l7tech.server.util.JaasUtils;
import com.l7tech.util.TextUtils;
import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Restrictions;
import org.springframework.dao.DataAccessException;
import org.springframework.orm.hibernate3.HibernateCallback;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.sql.SQLException;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.List;

import static com.l7tech.gateway.common.security.rbac.OperationType.*;

/**
 * TODO re-enable role creation for standard reports (once viewing is role, not ownership, based)
 */
@Transactional(propagation= Propagation.REQUIRED, rollbackFor=Throwable.class)
public class StandardReportManagerImpl  extends HibernateEntityManager<StandardReport, EntityHeader> implements StandardReportManager {

    //- PUBLIC

    public StandardReportManagerImpl( final RoleManager roleManager ) {
        this.roleManager = roleManager;
    }

    @Override
    public List<StandardReport> findPage( final User user, String sortProperty, boolean ascending, int offset, int count) throws FindException {
        return findPage( null, sortProperty, ascending, offset, count,  asCriterion(user)  );
    }

    @Override
    public int findCount( final User user ) throws FindException {
        return findCount( null, asCriterion(user) );
    }

    @Override
    public Collection<StandardReport> findByStatus( final String status ) throws FindException {
        if ( status == null ) throw new IllegalArgumentException("status must not be null");
        return super.findByPropertyMaybeNull( "status", status );
    }

    @Override
    public void deleteBySsgCluster(final SsgCluster ssgCluster) throws DeleteException {
        try {
            getHibernateTemplate().execute(new HibernateCallback() {
                @SuppressWarnings({"unchecked"})
                @Override
                public Object doInHibernate(Session session) throws HibernateException, SQLException {
                    final Criteria criteria = session.createCriteria(getImpClass());
                    criteria.add(Restrictions.eq("ssgCluster", ssgCluster));

                    for (StandardReport report: (Collection <StandardReport>)criteria.list()) {
                        session.delete(report);
                    }

                    return null;
                }
            });
        } catch (DataAccessException e) {
            throw new DeleteException("Couldn't delete Standard Report", e);
        }
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

    @Override
    public void delete(StandardReport standardReport) throws DeleteException {
        super.delete(standardReport);
        // roleManager.deleteEntitySpecificRole( EntityType.ESM_STANDARD_REPORT, standardReport.getOid() );
    }

    @Override
    public void delete(Goid goid) throws DeleteException, FindException {
        super.findAndDelete(goid);
    }

    @Override
    public Goid save( final StandardReport entity ) throws SaveException {

        // addReportRole( oid, entity );

        return super.save(entity);
    }

    //- PROTECTED

    @Override
    protected UniqueType getUniqueType() {
        return UniqueType.NONE;
    }

    //- PRIVATE

    private final RoleManager roleManager;

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

     private void addReportRole( final long oid, final StandardReport standardReport ) throws SaveException {
        User currentUser = JaasUtils.getCurrentUser();

        // truncate service name in the role name to avoid going beyond 128 limit
        String reportname = standardReport.getName();

        // cutoff is arbitrarily set to 50
        reportname = TextUtils.truncStringMiddle(reportname, 50);
        String name = MessageFormat.format("Manage {0} Report (#{1})", reportname, oid);

        Role newRole = new Role();
        newRole.setName(name);

        // RUD this report
        newRole.addEntityPermission(READ, EntityType.ESM_STANDARD_REPORT, Long.toString(oid));
        newRole.addEntityPermission(UPDATE, EntityType.ESM_STANDARD_REPORT, Long.toString(oid));
        newRole.addEntityPermission(DELETE, EntityType.ESM_STANDARD_REPORT, Long.toString(oid));

        // Set role as entity-specific
        newRole.setEntityType(EntityType.ESM_STANDARD_REPORT);
        newRole.setEntityOid(oid);
        newRole.setDescription("Users assigned to the {0} role have the ability to read, update and delete the {1} Report.");

        if (currentUser != null) {
            // See if we should give the current user admin permission for this service
            boolean omnipotent;
            try {
                omnipotent = roleManager.isPermittedForAnyEntityOfType(currentUser, OperationType.DELETE, EntityType.ESM_STANDARD_REPORT);
            } catch (FindException e) {
                throw new SaveException("Coudln't get existing permissions", e);
            }

            if (!omnipotent) {
                newRole.addAssignedUser(currentUser);
            }
        }

        roleManager.save(newRole);
    }
}
