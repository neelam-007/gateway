package com.l7tech.adminws;

import javax.security.auth.Subject;
import javax.security.auth.login.LoginException;
import java.net.PasswordAuthentication;
import java.security.AccessController;
import java.security.Principal;
import java.util.logging.Logger;

/**
 * Layer 7 Technologies, inc.
 * User: flascelles
 * Date: Jun 5, 2003
 *
 */
public abstract class ClientCredentialManager {
    protected static final Logger logger = Logger.getLogger(ClientCredentialManager.class.getName());


    /**
     * Subclasses implement this method to provide the concrete login implementation.
     *
     * @param creds the credentials to authenticate
     * @see ClientCredentialManagerImpl
     */
    public abstract void login(PasswordAuthentication creds)
            throws LoginException, VersionException;


    /**
     * Subclasses reset the credentials using this method.
     */
    protected void resetCredentials() {
        synchronized (ClientCredentialManager.class) {
            Subject subject = Subject.getSubject(AccessController.getContext());
            if (subject == null) {
                logger.warning("The subject is null");
                return;
            }
            subject.getPrincipals().clear();
            subject.getPrivateCredentials().clear();

        }
    }

    /**
     * Subclasses update the credentials using this method.
     * @param pa the username/password instance
     */
    protected final void setCredentials(PasswordAuthentication pa) {
        synchronized (ClientCredentialManager.class) {
            Subject subject = Subject.getSubject(AccessController.getContext());
            if (subject == null) {
                logger.warning("The subject is null");
                return;
            }
            subject.getPrincipals().clear();
            subject.getPrincipals().add(new ClientPrincipal(pa.getUserName()));
            subject.getPrivateCredentials().clear();
            subject.getPrivateCredentials().add(pa.getPassword());
        }
    }

    /**
     * internal principal holder class
     */
    static class ClientPrincipal implements Principal {
        private final String principal;

        ClientPrincipal(String principal) {
            this.principal = principal;
        }

        /**
         * Returns the name of this principal.
         *
         * @return the name of this principal.
         */
        public String getName() {
            return principal;
        }
    }
}
