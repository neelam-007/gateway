/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server.identity.fed;

import com.l7tech.identity.Group;
import com.l7tech.identity.GroupBean;
import com.l7tech.identity.IdentityProvider;
import com.l7tech.identity.PersistentGroup;
import com.l7tech.identity.fed.FederatedGroup;
import com.l7tech.identity.fed.FederatedGroupMembership;
import com.l7tech.identity.internal.GroupMembership;
import com.l7tech.objectmodel.DeleteException;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.UpdateException;
import com.l7tech.server.identity.PersistentGroupManager;

/**
 * @author alex
 * @version $Revision$
 */
public class FederatedGroupManager extends PersistentGroupManager {
    public FederatedGroupManager( IdentityProvider provider ) {
        super( provider );
    }

    protected GroupMembership newMembership( long userOid, long groupOid ) {
        return new FederatedGroupMembership(userOid, groupOid);
    }

    protected Class getMembershipClass() {
        return FederatedGroupMembership.class;
    }

    protected void preDelete( Group group ) throws DeleteException {
        // No admin group, don't need to call super
    }

    protected void preUpdate( Group group ) throws FindException, UpdateException {
        // No admin group, don't care
    }

    public PersistentGroup cast(Group group) {
        FederatedGroup imp;
        if ( group instanceof GroupBean ) {
            imp = new FederatedGroup( (GroupBean)group );
        } else {
            imp = (FederatedGroup)group;
        }
        return imp;
    }

    public Class getImpClass() {
        return FederatedGroup.class;
    }

    public Class getInterfaceClass() {
        return Group.class;
    }

    public String getTableName() {
        return "fed_group";
    }
}
