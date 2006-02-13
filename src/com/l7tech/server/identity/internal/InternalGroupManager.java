package com.l7tech.server.identity.internal;

import com.l7tech.identity.*;
import com.l7tech.identity.internal.InternalGroup;
import com.l7tech.identity.internal.InternalGroupMembership;
import com.l7tech.objectmodel.DeleteException;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.UpdateException;
import com.l7tech.server.identity.PersistentGroupManager;
import org.hibernate.Criteria;
import org.hibernate.criterion.Restrictions;

import java.util.Set;
import java.util.logging.Logger;

/**
 * GroupManager implementation for the internal identity provider.
 * 
 * <br/><br/>
 * Layer 7 Technologies, inc.<br/>
 * User: flascelles<br/>
 * Date: Jun 24, 2003
 */
public class InternalGroupManager extends PersistentGroupManager {
    public InternalGroupManager(IdentityProvider identityProvider) {
        super(identityProvider);
    }

    /**
     * empty subclassing constructor (required for class proxying)
     */
    protected InternalGroupManager() {
    }

    public GroupMembership newMembership(Group group, User user) {
        long groupOid = Long.parseLong(group.getUniqueIdentifier());
        long userOid = Long.parseLong(user.getUniqueIdentifier());
        return InternalGroupMembership.newInternalMembership(groupOid, userOid);
    }

    public Class getMembershipClass() {
        return InternalGroupMembership.class;
    }

    protected String getGetGroupsQuery() {
        return "select grp from grp in class " + getImpClass().getName() + ", " +
          "membership in class " + getMembershipClass().getName() + " " +
          "where membership.groupOid = grp.oid " +
          "and membership.userOid = ?";
    }

    protected void preDelete( PersistentGroup group ) throws DeleteException {
        // TODO don't use the name here
        if ( Group.ADMIN_GROUP_NAME.equals( group.getName() ) ) {
            logger.severe("an attempt to delete the admin group was made.");
            throw new DeleteException("Cannot delete administrator group.");
        }

        // TODO don't use the name here
        if (Group.OPERATOR_GROUP_NAME.equals(group.getName())) {
            logger.severe("an attempt to delete the operator group was made.");
            throw new DeleteException("Cannot delete operator group.");
        }
    }

    protected void preUpdate( PersistentGroup group ) throws FindException, UpdateException {
        // TODO don't use the name here
        if (Group.ADMIN_GROUP_NAME.equals(group.getName())) {
            Set oldAdminUserHeaders = getUserHeaders( group );
            if (oldAdminUserHeaders.size() < 1) {
                logger.severe("Blocked update on admin group because all members were deleted.");
                throw new UpdateException("Cannot update admin group with no memberships!");
            }
        }
    }

    public PersistentGroup cast(Group group) {
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

    private final Logger logger = Logger.getLogger(getClass().getName());

    protected void addMembershipCriteria(Criteria crit, Group group, Identity identity) {
        crit.add(Restrictions.eq("memberProviderOid", new Long(identity.getProviderId())));
    }
}
