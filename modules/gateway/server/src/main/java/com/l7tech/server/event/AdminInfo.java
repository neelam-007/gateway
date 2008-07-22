package com.l7tech.server.event;

import com.l7tech.identity.IdentityProviderConfig;
import com.l7tech.identity.User;
import com.l7tech.gateway.common.spring.remoting.RemoteUtils;

import javax.security.auth.Subject;
import java.rmi.server.ServerNotActiveException;
import java.security.AccessController;
import java.security.Principal;
import java.security.PrivilegedAction;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Holds information about a client admin that triggered some behavior.
 */
public class AdminInfo {
    public static final Logger logger = Logger.getLogger(AdminInfo.class.getName());

    public static final String LOCALHOST_IP = "127.0.0.1";
    public static final String LOCALHOST_SUBJECT = "localsystem";

    public AdminInfo(String login, String id, long ipOid, String ip, Subject subject) {
        this.ip = ip;
        this.id = id;
        this.identityProviderOid = ipOid;
        this.login = login;
        this.subject = subject;
    }

    public final String login;
    public final String id;
    public final long identityProviderOid;
    public final String ip;
    private final Subject subject;

    /**
     * Finds available information stashed thread-locally about the admin client that triggered the current
     * request.  This information will only be available if the current thread is the thread originally
     * dispatched to handle an admin request.
     *
     * @return an AdminInfo instance.  Never null, but may contain default information if the real admin info
     *         can't be located.
     */
    public static AdminInfo find() {
        Subject clientSubject = null;
        String login = null;
        String uniqueId = null;
        String address;
        long providerOid = IdentityProviderConfig.DEFAULT_OID;
        try {
            address = RemoteUtils.getClientHost();
            clientSubject = Subject.getSubject(AccessController.getContext());
        } catch (ServerNotActiveException e) {
            logger.log(Level.WARNING, "The administrative event caused as local call, outside of servicing an adminstrative remote call." +
              "Will use ip/user" + LOCALHOST_IP + '/' + LOCALHOST_SUBJECT);
            address = LOCALHOST_IP;
            login = LOCALHOST_SUBJECT;
        }
        if (clientSubject != null) {
            Set principals = clientSubject.getPrincipals();
            if (principals != null && !principals.isEmpty()) {
                Principal p = (Principal)principals.iterator().next();
                if (p instanceof User) {
                    User u = (User) p;
                    login = u.getLogin();
                    uniqueId = u.getId();
                    providerOid = u.getProviderId();
                }
                if (login == null) login = p.getName();
                if (uniqueId == null) uniqueId = "principal:"+login;
            }
        }

        return new AdminInfo(login, uniqueId, providerOid, address, clientSubject);
    }

    /**
     * Wrap the specified Callable so that it will be executed in a thread-local context configured
     * with this AdminInfo.
     * <p/>
     * This can be used to ensure that an asynchronous job started in service of an admin request
     * has enough information to tie its audit records back to the admin user and host that originally
     * made the request.  See Bug #4032.
     *
     * @param delegate the Callable to wrap.  Required.
     * @return a Callable that will set up the AdminInfo before invoking the delegate
     */
    public <OUT> Callable<OUT> wrapCallable(Callable<OUT> delegate) {
        return wrapWithSubject(subject, wrapWithConnectionInfo(ip, delegate));
    }

    private static <OUT> Callable<OUT> wrapWithSubject(final Subject subject, final Callable<OUT> toWrap) {
        if (subject == null)
            return toWrap;
        return new Callable<OUT>() {
            public OUT call() {
                return Subject.doAs(subject, new PrivilegedAction<OUT>() {
                    public OUT run() {
                        try {
                            return toWrap.call();
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    }
                });
            }
        };
    }

    private static <OUT> Callable<OUT> wrapWithConnectionInfo(final String host, final Callable<OUT> toWrap) {
        if (host == null)
            return toWrap;
        return new Callable<OUT>() {
            public OUT call() throws Exception {
                return RemoteUtils.callWithConnectionInfo(host, null, toWrap);
            }
        };
    }
}
