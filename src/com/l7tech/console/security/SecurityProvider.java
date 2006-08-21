package com.l7tech.console.security;

import com.l7tech.common.Authorizer;
import com.l7tech.common.security.rbac.Permission;
import com.l7tech.common.security.rbac.RbacAdmin;
import com.l7tech.console.util.Registry;
import com.l7tech.identity.UserBean;
import com.l7tech.objectmodel.FindException;

import javax.security.auth.Subject;
import java.rmi.RemoteException;
import java.security.AccessController;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

/**
 * For SSM-side admin session management.
 * <p/>
 * <br/><br/>
 * Layer 7 Technologies, inc.<br/>
 * User: flascelles<br/>
 * Date: Jun 5, 2003
 */
public abstract class SecurityProvider extends Authorizer implements AuthenticationProvider {
    private static final Logger logger = Logger.getLogger(SecurityProvider.class.getName());
    private final Set<Permission> subjectPermissions = new HashSet<Permission>();

    /**
     * Return the authentication provider associated with this security provider
     *
     * @return the <code>AuthenticationProvider</code>
     */
    public AuthenticationProvider getAuthenticationProvider() {
        return this;
    }

    /**
     * Logoff the session, default implementation, that does nothing
     */
    public void logoff() {
    }

    /**
     * Subclasses reset the credentials using this method.
     */
    protected void resetCredentials() {
        synchronized (SecurityProvider.class) {
            Subject subject = Subject.getSubject(AccessController.getContext());
            if (subject == null) {
                logger.warning("The subject is null");
                return;
            }
            subject.getPrincipals().clear();
            subject.getPrivateCredentials().clear();
            subjectPermissions.clear();
        }
    }

    /**
     * Subclasses update the credentials using this method.
     *
     * @param login the username instance
     */
    protected final void setCredentials(String login, Object creds) {
        synchronized (SecurityProvider.class) {
            Subject subject = Subject.getSubject(AccessController.getContext());
            if (subject == null) {
                logger.warning("The subject is null");
                return;
            }
            subject.getPrincipals().clear();
            final UserBean u = new UserBean();
            u.setLogin(login);
            u.setName(login);
            u.setProviderId(-2); // ...
            subject.getPrincipals().add(u);
            subject.getPrivateCredentials().clear();
            subject.getPrivateCredentials().add(creds);
        }
    }

    /**
     * Determine the permissions for the current user
     *
     * @return the set of permission for the current subject
     * @throws RuntimeException on error retrieving user permissions
     */
    public Collection<Permission> getUserPermissions() throws RuntimeException {
        Set<Permission> result;
        synchronized (this) {
            result = Collections.unmodifiableSet(subjectPermissions);
        }
        if (!result.isEmpty()) return result;
        return refreshPermissionCache();
    }

    public Collection<Permission> refreshPermissionCache() {
        RbacAdmin rbacAdmin = Registry.getDefault().getRbacAdmin();
        if (rbacAdmin == null) {
            throw new IllegalStateException("Unable to obtain admin service");
        }

        try {
            final Collection<Permission> perms = rbacAdmin.findCurrentUserPermissions();
            synchronized (this) {
                subjectPermissions.clear();
                subjectPermissions.addAll(perms);
            }
            return perms;
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        } catch (FindException e) {
            throw new RuntimeException(e);
        }
    }
}
