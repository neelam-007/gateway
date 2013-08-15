package com.l7tech.server.policy.assertion.identity;

import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.identity.Group;
import com.l7tech.identity.GroupManager;
import com.l7tech.identity.internal.InternalGroup;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.ObjectNotFoundException;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.identity.MemberOfGroup;
import com.l7tech.server.identity.AuthenticationResult;
import com.l7tech.util.ExceptionUtils;
import org.springframework.context.ApplicationContext;

import java.util.concurrent.atomic.AtomicReference;

public class ServerMemberOfGroup extends ServerIdentityAssertion<MemberOfGroup> {
    public ServerMemberOfGroup(MemberOfGroup data, ApplicationContext applicationContext) {
        super(data, applicationContext);
    }

    /**
     * Attempts to resolve a <code>Group</code> from the <code>groupOid</code> and
     * <code>groupName</code> properties, in that order.
     * <p/>
     * <p>This will cache the Group information for a short time.</p>
     *
     * @throws com.l7tech.objectmodel.FindException
     *          If an error occurs loading the group
     */
    @SuppressWarnings({"ThrowableResultOfMethodCallIgnored"})
    protected Group getGroup() throws FindException, ObjectNotFoundException {
        Group group = null;
        CachedGroup cg = cachedGroup.get();

        if (cg != null && !isStale(cg.cacheTime)) {
            group = cg.group;
        } else {
            GroupManager gman = getIdentityProvider().getGroupManager();

            if (assertion.getGroupId() != null) {
                group = gman.findByPrimaryKey(assertion.getGroupId());
            } else {
                String groupName = assertion.getGroupName();
                if (groupName != null) {
                    group = gman.findByName(groupName);
                }
            }

            cachedGroup.compareAndSet(cg, new CachedGroup(group));
        }

        return group;
    }

    /**
     * Returns <code>AssertionStatus.NONE</code> if the authenticated <code>User</code>
     * is a member of the <code>Group</code> with which this assertion was initialized.
     */
    @Override
    @SuppressWarnings({"ThrowableResultOfMethodCallIgnored"})
    public AssertionStatus checkUser(AuthenticationResult authResult) {
        GroupManager gman;
        try {
            gman = getIdentityProvider().getGroupManager();
        } catch (ObjectNotFoundException e) {
            logAndAudit( AssertionMessages.IDENTITY_PROVIDER_NOT_EXIST, new String[]{ ExceptionUtils.getMessage( e ) }, ExceptionUtils.getDebugException( e ) );
            return AssertionStatus.UNAUTHORIZED;
        } catch (FindException e) {
            logAndAudit( AssertionMessages.IDENTITY_PROVIDER_NOT_FOUND, new String[]{ ExceptionUtils.getMessage( e ) }, ExceptionUtils.getDebugException( e ) );
            return AssertionStatus.UNAUTHORIZED;
        }

        try {

            Group targetGroup = getGroup();
            if (targetGroup == null) {
                logAndAudit( AssertionMessages.MEMBEROFGROUP_GROUP_NOT_EXIST );
                return AssertionStatus.UNAUTHORIZED;
            }

            if (targetGroup instanceof InternalGroup) {
                if (!((InternalGroup) targetGroup).isEnabled()) {
                    logAndAudit( AssertionMessages.MEMBEROFGROUP_GROUP_DISALBED, targetGroup.getName() );
                    return AssertionStatus.UNAUTHORIZED;
                }
            }

            Boolean wasMember = authResult.getCachedGroupMembership(targetGroup);
            if (wasMember == null) {
                if (authResult.getUser() != null &&
                        authResult.getUser().getProviderId().equals(assertion.getIdentityProviderOid())) {
                    // Cache miss
                    if (gman.isMember(authResult.getUser(), targetGroup)) {
                        authResult.setCachedGroupMembership(targetGroup, true);
                        logger.finest("membership established");
                        return AssertionStatus.NONE;
                    }
                }
                authResult.setCachedGroupMembership(targetGroup, false);
                logAndAudit( AssertionMessages.MEMBEROFGROUP_USER_NOT_MEMBER );
                return AssertionStatus.UNAUTHORIZED;
            }

            // Cache hit
            if ( wasMember ) {
                logger.finest("Reusing cached group membership success");
                return AssertionStatus.NONE;
            }
            logAndAudit( AssertionMessages.MEMBEROFGROUP_USING_CACHED_FAIL );
            return AssertionStatus.UNAUTHORIZED;
        } catch (ObjectNotFoundException e) {
            logAndAudit( AssertionMessages.IDENTITY_PROVIDER_NOT_EXIST );
            return AssertionStatus.UNAUTHORIZED;
        } catch (FindException fe) {
            logAndAudit( AssertionMessages.MEMBEROFGROUP_GROUP_NOT_EXIST );
            return AssertionStatus.UNAUTHORIZED;
        }
    }

    private static final long MAX_CACHED_GROUP_AGE = 1000L; // cache for very short time only (1 second)
    private final AtomicReference<CachedGroup> cachedGroup = new AtomicReference<CachedGroup>();

    private boolean isStale(long timestamp) {
        boolean stale = true;
        long timenow = System.currentTimeMillis();

        if (timestamp + MAX_CACHED_GROUP_AGE > timenow) {
            stale = false;
        }

        return stale;
    }

    private static final class CachedGroup {
        private final Group group;
        private final long cacheTime;

        private CachedGroup(final Group group) {
            this.group = group;
            this.cacheTime = System.currentTimeMillis();
        }
    }
}
