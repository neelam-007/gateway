package com.l7tech.server.identity.internal;

import com.l7tech.identity.Group;
import com.l7tech.identity.GroupBean;
import com.l7tech.identity.IdentityProvider;
import com.l7tech.identity.PersistentGroup;
import com.l7tech.identity.internal.GroupMembership;
import com.l7tech.identity.internal.InternalGroup;
import com.l7tech.objectmodel.DeleteException;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.UpdateException;
import com.l7tech.server.identity.PersistentGroupManager;

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
public class InternalGroupManagerServer extends PersistentGroupManager {
    public InternalGroupManagerServer( IdentityProvider provider ) {
        super(provider);
    }

    protected GroupMembership newMembership( long userOid, long groupOid ) {
        return new GroupMembership(userOid,groupOid);
    }

    public Class getMembershipClass() {
        return GroupMembership.class;
    }

    protected void preDelete( Group group ) throws DeleteException {
        if ( Group.ADMIN_GROUP_NAME.equals( group.getName() ) ) {
            logger.severe("an attempt to delete the admin group was made.");
            throw new DeleteException("Cannot delete administrator group.");
        }
    }

    protected void preUpdate( Group group ) throws FindException, UpdateException {
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
}
