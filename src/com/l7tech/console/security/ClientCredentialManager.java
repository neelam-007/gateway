package com.l7tech.console.security;

import com.l7tech.common.VersionException;
import com.l7tech.console.event.ConnectionEvent;
import com.l7tech.console.event.ConnectionListener;
import com.l7tech.identity.UserBean;

import javax.security.auth.Subject;
import javax.security.auth.login.LoginException;
import java.net.PasswordAuthentication;
import java.rmi.RemoteException;
import java.security.AccessController;
import java.util.logging.Logger;

/**
 * For SSM-side admin session management.
 * <p/>
 * <br/><br/>
 * Layer 7 Technologies, inc.<br/>
 * User: flascelles<br/>
 * Date: Jun 5, 2003
 */
public abstract class ClientCredentialManager implements ConnectionListener {
    protected static final Logger logger = Logger.getLogger(ClientCredentialManager.class.getName());


    /**
     * Subclasses implement this method to provide the concrete login implementation.
     *
     * @param creds the credentials to authenticate
     * @see com.l7tech.console.security.ClientCredentialManagerImpl
     */
    public abstract void login(PasswordAuthentication creds)
      throws LoginException, VersionException, RemoteException;


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
     *
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
            final UserBean u = new UserBean();
            u.setLogin(pa.getUserName());
            subject.getPrincipals().add(u);
            subject.getPrivateCredentials().clear();
            subject.getPrivateCredentials().add(pa.getPassword());
        }
    }

    /**
     * Invoked on connection event (empty implementation)
     *
     * @param e describing the connection event
     */
    public void onConnect(ConnectionEvent e) {}

    /**
     * Invoked on disconnect (empty implementation)
     *
     * @param e describing the dosconnect event
     */
    public void onDisconnect(ConnectionEvent e) {}

}
