package com.l7tech.remote.jini.export;

import com.l7tech.common.Authorizer;
import com.l7tech.identity.Group;
import org.springframework.context.support.ApplicationObjectSupport;

import javax.security.auth.Subject;
import java.security.AccessControlException;
import java.security.AccessController;

/**
 * <code>RemoteService</code> is extended by the concrete Jini services.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version 1.0
 * @deprecated
 */
public abstract class RemoteService extends ApplicationObjectSupport {
    /**
     * Returns a boolean indicating whether the authenticated user is included in the specified
     * logical "roles".
     *
     * @param roles role - a String array specifying the role names
     * @return a boolean indicating whether the user making this request belongs to one or more given
     *         roles; false if not or the user has not been authenticated
     */
    protected boolean isUserInRole(String[] roles) {
        Subject subject = Subject.getSubject(AccessController.getContext());
        if (subject == null) {
            return false;
        }
        return Authorizer.getAuthorizer().isSubjectInRole(subject, roles);
    }


    /**
     * Makes sure that current subject has full write admin role.
     *
     * @throws AccessControlException if not the case
     */
    protected void enforceAdminRole() throws AccessControlException {
        if (!isUserInRole(new String[]{Group.ADMIN_GROUP_NAME})) {
            throw new AccessControlException("Must be member of " + Group.ADMIN_GROUP_NAME +
              " to perform this operation.");
        }
    }
}
