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

    public String getGroupName() {
        return _groupName;
    }

    public void setGroupName(String groupName) {
        _groupName = groupName;
    }

    public MemberOfGroup(long providerOid, String groupName) {
        super(providerOid);
        _groupName = groupName;
    }

    /**
     * Attempts to resolve a <code>Group</code> from the <code>groupOid</code> and <code>groupName</code> properties, in that order.
     * @return
     * @throws FindException
     */
    protected Group getGroup() throws FindException {
        if (_group == null) {
            GroupManager gman = getIdentityProvider().getGroupManager();
            if (_groupName != null) {
                _group = gman.findByName(_groupName);
            }
        }
        return _group;
    }

    /**
     * Returns <code>AssertionStatus.NONE</code> if the authenticated <code>User</code> is a member of the <code>Group</code> with which this assertion was initialized.
     * @param user
     * @return
     */
    public AssertionStatus doCheckUser(User user) {
        try {
            Group targetGroup = getGroup();
            if ( targetGroup.getMembers().contains( user ) )
                return AssertionStatus.NONE;
            else {
                return AssertionStatus.AUTH_FAILED;
            }
        } catch (FindException fe) {
            LogManager.getInstance().getSystemLogger().log(Level.SEVERE, null, fe);
            return AssertionStatus.FAILED;
        }
    }

    protected String _groupName;
    protected transient Group _group;
}
