package com.l7tech.server.identity.internal;

import com.l7tech.identity.Group;
import com.l7tech.identity.GroupBean;
import com.l7tech.identity.GroupMembership;
import com.l7tech.identity.Identity;
import com.l7tech.identity.internal.InternalGroup;
import com.l7tech.identity.internal.InternalGroupMembership;
import com.l7tech.identity.internal.InternalUser;
import com.l7tech.objectmodel.*;
import com.l7tech.server.identity.PersistentGroupManagerImpl;
import com.l7tech.server.security.rbac.RoleManager;
import com.l7tech.util.ExceptionUtils;
import org.hibernate.Criteria;
import org.hibernate.criterion.Restrictions;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * GroupManager implementation for the internal identity provider.
 * 
 * <br/><br/>
 * Layer 7 Technologies, inc.<br/>
 * User: flascelles<br/>
 * Date: Jun 24, 2003
 */
@Transactional(propagation=Propagation.REQUIRED, rollbackFor=Throwable.class)
public class InternalGroupManagerImpl
        extends PersistentGroupManagerImpl<InternalUser, InternalGroup, InternalUserManager, InternalGroupManager>
        implements InternalGroupManager
{
    private final RoleManager roleManager;

    public InternalGroupManagerImpl( final RoleManager roleManager ) {
        this.roleManager = roleManager;
    }

    @Override
    public void configure(InternalIdentityProvider provider) {
        this.setIdentityProvider(provider);
    }

    @Override
    public InternalGroup reify(GroupBean bean) {
        InternalGroup ig = new InternalGroup(bean.getName());
        ig.setDescription(bean.getDescription());
        ig.setGoid(bean.getId() == null ? InternalGroup.DEFAULT_GOID : Goid.parseGoid(bean.getId()));
        return ig;
    }

    @Override
    public GroupMembership newMembership(InternalGroup group, InternalUser user) {
        Goid groupOid = Goid.parseGoid(group.getId());
        Goid userOid = Goid.parseGoid(user.getId());
        return InternalGroupMembership.newInternalMembership(groupOid, userOid);
    }

    @Override
    public Class getMembershipClass() {
        return InternalGroupMembership.class;
    }

    @Override
    @Transactional(propagation=Propagation.SUPPORTS)
    public InternalGroup cast(Group group) {
        if ( group instanceof GroupBean ) {
            return reify((GroupBean)group);
        } else {
            return (InternalGroup)group;
        }
    }

    @Override
    public String getTableName() {
        return "internal_group";
    }

    @Override
    public Class<InternalGroup> getImpClass() {
        return InternalGroup.class;
    }

    @Override
    public Class<Group> getInterfaceClass() {
        return Group.class;
    }

    @Override
    protected IdentityHeader newHeader( final InternalGroup entity ) {
        return new IdentityHeader(getProviderGoid(), entity.getGoid(), EntityType.GROUP, entity.getName(), entity.getDescription(), null, entity.getVersion(), entity.isEnabled());
    }

    @Override
    protected void addMembershipCriteria(Criteria crit, Group group, Identity identity) {
        crit.add(Restrictions.eq("memberProviderGoid", identity.getProviderId()));
    }

    @Override
    protected void preDelete(InternalGroup group) throws DeleteException {
    }

    @Override
    protected void postDelete(InternalGroup group) throws DeleteException {
        if (group != null)
            roleManager.deleteRoleAssignmentsForGroup(group);
        try {
            roleManager.validateRoleAssignments();
        } catch ( UpdateException ue ) {
            throw new DeleteException( ExceptionUtils.getMessage(ue), ue );
        }
    }

    @Override
    protected void postUpdate(InternalGroup group) throws UpdateException {
        roleManager.validateRoleAssignments();
    }
}
