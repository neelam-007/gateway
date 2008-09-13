package com.l7tech.server.identity.internal;

import com.l7tech.identity.Group;
import com.l7tech.identity.GroupBean;
import com.l7tech.identity.GroupMembership;
import com.l7tech.identity.Identity;
import com.l7tech.identity.internal.InternalGroup;
import com.l7tech.identity.internal.InternalGroupMembership;
import com.l7tech.identity.internal.InternalUser;
import com.l7tech.server.identity.PersistentGroupManagerImpl;
import com.l7tech.server.security.rbac.RoleManager;
import com.l7tech.objectmodel.DeleteException;
import com.l7tech.objectmodel.UpdateException;
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

    public void configure(InternalIdentityProvider provider) {
        this.setIdentityProvider(provider);
    }

    public InternalGroup reify(GroupBean bean) {
        InternalGroup ig = new InternalGroup(bean.getName());
        ig.setDescription(bean.getDescription());
        ig.setOid(bean.getId() == null ? InternalGroup.DEFAULT_OID : Long.valueOf(bean.getId()));
        return ig;
    }

    public GroupMembership newMembership(InternalGroup group, InternalUser user) {
        long groupOid = Long.parseLong(group.getId());
        long userOid = Long.parseLong(user.getId());
        return InternalGroupMembership.newInternalMembership(groupOid, userOid);
    }

    public Class getMembershipClass() {
        return InternalGroupMembership.class;
    }

    @Transactional(propagation=Propagation.SUPPORTS)
    public InternalGroup cast(Group group) {
        if ( group instanceof GroupBean ) {
            return reify((GroupBean)group);
        } else {
            return (InternalGroup)group;
        }
    }

    public String getTableName() {
        return "internal_group";
    }

    public Class<InternalGroup> getImpClass() {
        return InternalGroup.class;
    }

    public Class<Group> getInterfaceClass() {
        return Group.class;
    }

    protected void addMembershipCriteria(Criteria crit, Group group, Identity identity) {
        crit.add(Restrictions.eq("memberProviderOid", identity.getProviderId()));
    }

    @Override
    protected void postDelete(InternalGroup group) throws DeleteException {
        try {
            roleManager.validateRoleAssignments();
        } catch ( UpdateException ue ) {
            throw new DeleteException( ExceptionUtils.getMessage(ue), ue );
        }
    }
}
