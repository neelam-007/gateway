package com.l7tech.console.security;

import com.l7tech.common.Authorizer;
import com.l7tech.console.util.Registry;
import com.l7tech.identity.IdentityAdmin;
import com.l7tech.identity.UserBean;

import javax.security.auth.Subject;
import java.net.PasswordAuthentication;
import java.rmi.RemoteException;
import java.security.AccessController;
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
    protected static final Logger logger = Logger.getLogger(SecurityProvider.class.getName());
    private Set subjectRoles = new HashSet();

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
     * @param pa the username/password instance
     */
    protected final void setCredentials(PasswordAuthentication pa) {
        synchronized (SecurityProvider.class) {
            Subject subject = Subject.getSubject(AccessController.getContext());
            if (subject == null) {
                logger.warning("The subject is null");
                return;
            }
            subject.getPrincipals().clear();
            final UserBean u = new UserBean();
            u.setLogin(pa.getUserName());
            u.setName(pa.getUserName());
            subject.getPrincipals().add(u);
            subject.getPrivateCredentials().clear();
            subject.getPrivateCredentials().add(pa.getPassword());
        }
    }

    /**
     * Subclasses update the subject roles using this method.
     *
     * @param roles the subject roles
     * @throws IllegalStateException if the method is invoked in wrokng time, i.e. the subject
     *                               is not set
     */
    protected final void setRoles(Set roles) {
        Subject subject = Subject.getSubject(AccessController.getContext());
        if (subject == null) {
            throw new IllegalStateException("The subject is null");
        }
        synchronized (this) {
            subjectRoles.clear();
            subjectRoles.addAll(roles);
        }
    }

    protected Set getSubjectRoles() {
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
    public Set getUserRoles(Subject subject) throws RuntimeException {
        final Set subjectRoles = getSubjectRoles();
        if (!subjectRoles.isEmpty()) {
            return subjectRoles;
        }
        IdentityAdmin is = Registry.getDefault().getIdentityAdmin();
        if (is == null) {
            throw new IllegalStateException("Unable to obtain admin service");
        }
        try {
            final Set roles = is.getRoles(subject);
            setRoles(roles);
            return roles;
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }
}
