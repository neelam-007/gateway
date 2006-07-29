package com.l7tech.console.security;

import com.l7tech.common.Authorizer;
import com.l7tech.common.security.rbac.RbacAdmin;
import com.l7tech.common.security.rbac.Role;
import com.l7tech.console.util.Registry;
import com.l7tech.identity.User;
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
    private Set<Role> subjectRoles = new HashSet<Role>();

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
            subjectRoles.clear();
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
     * Subclasses update the subject roles using this method.
     *
     * @param roles the subject roles
     * @throws IllegalStateException if the method is invoked in wrokng time, i.e. the subject
     *                               is not set
     */
    protected final void setRoles(Collection<Role> roles) {
        Subject subject = Subject.getSubject(AccessController.getContext());
        if (subject == null) {
            throw new IllegalStateException("The subject is null");
        }
        synchronized (this) {
            subjectRoles.clear();
            subjectRoles.addAll(roles);
        }
    }

    protected Set<Role> getSubjectRoles() {
        synchronized (this) {
            return Collections.unmodifiableSet(subjectRoles);
        }
    }

    /**
     * Determine the roles (groups) for hte given subject
     *
     * @param subject the subject
     * @return the set of user roles for the given subject
     * @throws RuntimeException on error retrieving user roles
     */
    public Collection<Role> getUserRoles(Subject subject) throws RuntimeException {
        final Set<Role> subjectRoles = getSubjectRoles();
        if (!subjectRoles.isEmpty()) {
            return subjectRoles;
        }

        RbacAdmin rbacAdmin = Registry.getDefault().getRbacAdmin();
        if (rbacAdmin == null) {
            throw new IllegalStateException("Unable to obtain admin service");
        }

        try {
            Set<User> users = subject.getPrincipals(User.class);
            if (users.size() > 1) throw new IllegalStateException("Multiple Users in current Subject");
            User u = users.iterator().next();
            final Collection<Role> roles = rbacAdmin.findRolesForUser(u);
            setRoles(roles);
            return roles;
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        } catch (FindException e) {
            throw new RuntimeException(e);
        }
    }
}
