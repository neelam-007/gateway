package com.l7tech.server.identity.internal;

import com.l7tech.identity.Group;
import com.l7tech.identity.GroupBean;
import com.l7tech.identity.GroupMembership;
import com.l7tech.identity.Identity;
import com.l7tech.identity.internal.InternalGroup;
import com.l7tech.identity.internal.InternalGroupMembership;
import com.l7tech.identity.internal.InternalUser;
import com.l7tech.server.identity.PersistentGroupManagerImpl;
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
    public InternalGroupManagerImpl(InternalIdentityProvider identityProvider) {
        super(identityProvider);
    }

    public InternalGroup reify(GroupBean bean) {
        return new InternalGroup(bean);
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
        InternalGroup imp;
        if ( group instanceof GroupBean ) {
            imp = new InternalGroup( (GroupBean)group );
        } else {
            imp = (InternalGroup)group;
        }
        return imp;
    }

    public String getTableName() {
        return "internal_group";
    }

    public Class getImpClass() {
        return InternalGroup.class;
    }

    public Class getInterfaceClass() {
        return Group.class;
    }

    protected void addMembershipCriteria(Criteria crit, Group group, Identity identity) {
        crit.add(Restrictions.eq("memberProviderOid", new Long(identity.getProviderId())));
    }
}
