/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.policy.assertion.identity;

import com.l7tech.identity.Group;
import com.l7tech.identity.GroupManager;
import com.l7tech.identity.User;
import com.l7tech.logging.LogManager;
import com.l7tech.objectmodel.FindException;
import com.l7tech.policy.assertion.AssertionStatus;

import java.util.Set;
import java.util.logging.Level;

/**
 * Asserts that the requestor is a member of a particular group.
 *
 * @author alex
 * @version $Revision$
 */
public class MemberOfGroup extends IdentityAssertion {
    public MemberOfGroup() {
        super();
    }

    public MemberOfGroup( long providerOid, String groupOid ) {
        super( providerOid );
        _groupOid = groupOid;
    }

    public void setGroupOid( String oid ) {
        if ( oid != _groupOid ) _group = null;
        _groupOid = oid;

    }

    public String getGroupOid() {
        return _groupOid;
    }

    protected Group getGroup() throws FindException {
        if ( _group == null ) {
            try {
                GroupManager gman = getIdentityProvider().getGroupManager();
                _group = gman.findByPrimaryKey( _groupOid );
            } catch ( FindException fe ) {
                LogManager.getInstance().getSystemLogger().log(Level.SEVERE, null, fe);
            }
        }
        return _group;
    }

    public AssertionStatus doCheckUser( User user ) {
        Set groups = user.getGroups();
        try {
            if ( groups.contains( getGroup() ) )
                return AssertionStatus.NONE;
            else {
                return AssertionStatus.AUTH_FAILED;
            }
        } catch ( FindException fe ) {
            LogManager.getInstance().getSystemLogger().log(Level.SEVERE, null, fe);
            return AssertionStatus.FAILED;
        }
    }

    protected String _groupOid;
    protected transient Group _group;
}
