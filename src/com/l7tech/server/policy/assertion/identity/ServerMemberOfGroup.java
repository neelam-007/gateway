/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 */

package com.l7tech.server.policy.assertion.identity;

import com.l7tech.identity.Group;
import com.l7tech.identity.GroupManager;
import com.l7tech.objectmodel.FindException;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.identity.MemberOfGroup;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.ServerAssertion;
import com.l7tech.server.identity.AuthenticationResult;
import com.l7tech.common.audit.AssertionMessages;
import org.springframework.context.ApplicationContext;

import java.util.logging.Logger;

/**
 * @author alex
 */
public class ServerMemberOfGroup extends ServerIdentityAssertion implements ServerAssertion {
    public ServerMemberOfGroup( MemberOfGroup data, ApplicationContext applicationContext ) {
        super( data, applicationContext);
        _data = data;
    }

    /**
     * Attempts to resolve a <code>Group</code> from the <code>groupOid</code> and
     * <code>groupName</code> properties, in that order.
     * @param context
     * @throws com.l7tech.objectmodel.FindException
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
     */
    public AssertionStatus checkUser(AuthenticationResult authResult, PolicyEnforcementContext context) {
        try {
            Group targetGroup = getGroup(context);
            if (targetGroup == null) {
                auditor.logAndAudit(AssertionMessages.GROUP_NOTEXIST);
                return AssertionStatus.UNAUTHORIZED;
            }

            GroupManager gman = getIdentityProvider(context).getGroupManager();
            Boolean wasMember = authResult.getCachedGroupMembership(targetGroup);
            if (wasMember == null) {
                if (authResult.getUser() != null &&
                    authResult.getUser().getProviderId() == identityAssertion.getIdentityProviderOid()) {
                    // Cache miss
                    if (gman.isMember(authResult.getUser(), targetGroup)) {
                        authResult.setCachedGroupMembership(targetGroup, true);
                        logger.finest("membership established");
                        return AssertionStatus.NONE;
                    }
                }
                authResult.setCachedGroupMembership(targetGroup, false);
                auditor.logAndAudit(AssertionMessages.USER_NOT_IN_GROUP);
                return AssertionStatus.UNAUTHORIZED;
            }

            // Cache hit
            if (wasMember.booleanValue()) {
                logger.finest("Reusing cached group membership success");
                return AssertionStatus.NONE;
            }
            auditor.logAndAudit(AssertionMessages.CACHED_GROUP_MEMBERSHIP_FAILURE);
            return AssertionStatus.UNAUTHORIZED;
        } catch (FindException fe) {
            auditor.logAndAudit(AssertionMessages.GROUP_NOTEXIST);
            return AssertionStatus.UNAUTHORIZED;
        }
    }

    protected MemberOfGroup _data;
    private final Logger logger = Logger.getLogger(getClass().getName());
}
