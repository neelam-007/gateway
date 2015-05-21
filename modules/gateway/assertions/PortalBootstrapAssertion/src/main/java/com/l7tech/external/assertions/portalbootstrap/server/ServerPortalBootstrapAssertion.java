package com.l7tech.external.assertions.portalbootstrap.server;

import com.l7tech.external.assertions.portalbootstrap.PortalBootstrapAssertion;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.gateway.common.security.rbac.Role;
import com.l7tech.identity.User;
import com.l7tech.objectmodel.FindException;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.AbstractServerAssertion;
import com.l7tech.server.policy.variable.ExpandVariables;
import com.l7tech.server.security.rbac.RbacServices;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.Pair;

import javax.inject.Inject;
import javax.security.auth.Subject;
import javax.ws.rs.core.SecurityContext;
import java.io.IOException;
import java.security.Principal;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;

/**
 * Server side implementation of the PortalBootstrapAssertion.
 *
 * @see com.l7tech.external.assertions.portalbootstrap.PortalBootstrapAssertion
 */
public class ServerPortalBootstrapAssertion extends AbstractServerAssertion<PortalBootstrapAssertion> {

    @Inject
    private RbacServices rbacServices;

    public ServerPortalBootstrapAssertion(final PortalBootstrapAssertion assertion) throws PolicyAssertionException {
        super(assertion);
    }

    public AssertionStatus checkRequest(final PolicyEnforcementContext context) throws IOException, PolicyAssertionException {
        final String url = ExpandVariables.process(getAssertion().getEnrollmentUrl(), context.getVariableMap(assertion.getVariablesUsed(), getAudit()), getAudit());
        final User user = context.getDefaultAuthenticationContext().getLastAuthenticatedUser();
        if (null == user) {
            logAndAudit(AssertionMessages.PORTAL_BOOTSTRAP_ERROR, new String[]{"An authenticated user is required but not present"});
            return AssertionStatus.FAILED;
        }

        if (!isFullAdministrator(user)) {
            logAndAudit(AssertionMessages.PORTAL_BOOTSTRAP_ERROR, new String[]{"Administrator user required"});
            return AssertionStatus.FAILED;
        }

        //Create the subject for securing manager calls
        final Subject subject = new Subject();
        SecurityContext securityContext = new SecurityContext() {
            @Override
            public Principal getUserPrincipal() {
                return user;  //To change body of implemented methods use File | Settings | File Templates.
            }

            @Override
            public boolean isUserInRole(String role) {
                return false;  //To change body of implemented methods use File | Settings | File Templates.
            }

            @Override
            public boolean isSecure() {
                return false;  //To change body of implemented methods use File | Settings | File Templates.
            }

            @Override
            public String getAuthenticationScheme() {
                return null;  //To change body of implemented methods use File | Settings | File Templates.
            }
        };
        subject.getPrincipals().add(securityContext.getUserPrincipal());
        //Surround the manager call with a 'do as' in order to set the authenticated user.
        try {
            Subject.doAs(subject, new PrivilegedExceptionAction<Void>() {
                @Override
                public Void run() throws Exception {
                    PortalBootstrapManager.getInstance().enrollWithPortal(url);
                    return null;
                }
            });
        } catch (PrivilegedActionException e) {
            logAndAudit(AssertionMessages.PORTAL_BOOTSTRAP_ERROR, new String[]{ExceptionUtils.getMessage(e.getException())}, ExceptionUtils.getDebugException(e));
            return AssertionStatus.FAILED;
        }

        return AssertionStatus.NONE;
    }

    private boolean isFullAdministrator(User user) {
        try {
            for (final Role role : rbacServices.getAssignedRoles(new Pair<>(user.getProviderId(), user.getId()), user)) {
                if (Role.Tag.ADMIN.equals(role.getTag())) {
                    return true;
                }
            }
        } catch (FindException e) {
            return false;
        }
        return false;
    }

    public static void onModuleUnloaded() {
    }
}
