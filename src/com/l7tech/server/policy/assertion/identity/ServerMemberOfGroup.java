/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server.policy.assertion.identity;

import com.l7tech.identity.Group;
import com.l7tech.identity.GroupManager;
import com.l7tech.identity.User;
import com.l7tech.objectmodel.FindException;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.identity.MemberOfGroup;
import com.l7tech.server.policy.assertion.ServerAssertion;
import com.l7tech.server.message.PolicyEnforcementContext;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author alex
 * @version $Revision$
 */
public class ServerMemberOfGroup extends ServerIdentityAssertion implements ServerAssertion {
    public ServerMemberOfGroup( MemberOfGroup data ) {
        super( data );
        _data = data;
    }

    /**
     * Attempts to resolve a <code>Group</code> from the <code>groupOid</code> and
     * <code>groupName</code> properties, in that order.
     * @return
     * @throws com.l7tech.objectmodel.FindException
     * @param context
     */
    protected Group getGroup(PolicyEnforcementContext context) throws FindException {
        GroupManager gman = getIdentityProvider(context).getGroupManager();
        if (_data.getGroupId() != null) {
            return gman.findByPrimaryKey(_data.getGroupId());
        }
        else {
            String groupName = _data.getGroupName();
            if ( groupName != null) {
                return gman.findByName(groupName);
            }
        }
        return null;
    }

    /**
     * Returns <code>AssertionStatus.NONE</code> if the authenticated <code>User</code>
     * is a member of the <code>Group</code> with which this assertion was initialized.
     * @param user
     * @return
     * @param context
     */
    public AssertionStatus checkUser(User user, PolicyEnforcementContext context) {
        try {
            Group targetGroup = getGroup(context);
            if (targetGroup == null) {
                logger.severe("This assertion refers to a group that does not exist." +
                                     "Policy might be corrupted");
                return AssertionStatus.UNAUTHORIZED;
            }
            GroupManager gman = getIdentityProvider(context).getGroupManager();
            if ( gman.isMember(user, targetGroup) ) {
                logger.finest("membership established");
                return AssertionStatus.NONE;
            } else {
                logger.info("user not member of group");
                return AssertionStatus.UNAUTHORIZED;
            }
        } catch (FindException fe) {
            logger.log(Level.SEVERE, "Error finding group that this assertion refers to." +
                                     "Policy might be corrupted", fe);
            return AssertionStatus.UNAUTHORIZED;
        }
    }

    protected MemberOfGroup _data;
    private final Logger logger = Logger.getLogger(getClass().getName());
}
