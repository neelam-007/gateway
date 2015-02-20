package com.l7tech.server.event;

import com.l7tech.objectmodel.Goid;
import com.l7tech.util.InetAddressUtil;
import com.l7tech.identity.IdentityProviderConfig;
import com.l7tech.identity.User;
import com.l7tech.gateway.common.spring.remoting.RemoteUtils;
import com.l7tech.objectmodel.IdentityHeader;
import com.l7tech.objectmodel.EntityType;
import org.jetbrains.annotations.Nullable;

import javax.security.auth.Subject;
import java.rmi.server.ServerNotActiveException;
import java.security.AccessController;
import java.security.Principal;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Holds information about a client admin that triggered some behavior.
 */
public class AdminInfo {
    public static final Logger logger = Logger.getLogger(AdminInfo.class.getName());

    public static final String LOCALHOST_IP = InetAddressUtil.getLocalHostAddress();
    public static final String LOCALHOST_SUBJECT = "localsystem";

    private AdminInfo(String login, String id, Goid ipOid, String ip, @Nullable User user, Subject subject) {
        this.ip = ip;
        this.id = id;
        this.identityProviderOid = ipOid;
        this.login = login;
        this.user = user;
        this.subject = subject;
    }

    public final String login;
    public final String id;
    public final Goid identityProviderOid;
    public final String ip;
    @Nullable
    public final User user;
    private final Subject subject;

    /**
     * Finds available information stashed thread-locally about the admin client that triggered the current
     * request.  This information will only be available if the current thread is the thread originally
     * dispatched to handle an admin request.
     *
     * @return an AdminInfo instance.  Never null, but may contain default information if the real admin info
     *         can't be located.
     */
    public static AdminInfo find() { return find(true);  }
    public static AdminInfo find( boolean warnOnMissing ) {
        Subject clientSubject = null;
        String login = null;
        String uniqueId = null;
        String address;
        Goid providerOid = IdentityProviderConfig.DEFAULT_GOID;
        try {
            address = RemoteUtils.getClientHost();
            clientSubject = Subject.getSubject(AccessController.getContext());
        } catch (ServerNotActiveException e) {
            if ( warnOnMissing ) {
                logger.log(Level.WARNING, "The administrative event caused as local call, outside of servicing an adminstrative remote call." +
                    "Will use ip/user" + LOCALHOST_IP + '/' + LOCALHOST_SUBJECT);
            }
            address = LOCALHOST_IP;
            login = LOCALHOST_SUBJECT;
        }
        User u = null;
        if (clientSubject != null) {
            Set principals = clientSubject.getPrincipals();
            if (principals != null && !principals.isEmpty()) {
                Principal p = (Principal)principals.iterator().next();
                if (p instanceof User) {
                    u = (User) p;
                    login = u.getLogin();
                    uniqueId = u.getId();
                    providerOid = u.getProviderId();
                }
                if (login == null) login = p.getName();
                if (uniqueId == null) uniqueId = "principal:"+login;
            }
        }
        if (login == null) login = "unknownClientSubject";
        if (uniqueId == null) uniqueId = "principal:"+login;
        if (address == null) address = LOCALHOST_IP;

        return new AdminInfo(login, uniqueId, providerOid, address, u, clientSubject);
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
            @Override
            public OUT call() throws Exception {
                try {
                    return Subject.doAs(subject, new PrivilegedExceptionAction<OUT>() {
                        @Override
                        public OUT run() throws Exception{
                            return toWrap.call();
                        }
                    });
                } catch ( final PrivilegedActionException e ) {
                    throw e.getException();  // unwrap cause  
                }
            }
        };
    }

    private static <OUT> Callable<OUT> wrapWithConnectionInfo(final String host, final Callable<OUT> toWrap) {
        if (host == null)
            return toWrap;
        return new Callable<OUT>() {
            @Override
            public OUT call() throws Exception {
                return RemoteUtils.callWithConnectionInfo(host, null, toWrap);
            }
        };
    }

    /**
     * @return  Returns the identity header based on the admin information stored in this object
     */
    public IdentityHeader getIdentityHeader() {
        return new IdentityHeader(identityProviderOid, id, EntityType.USER, login, "", null, null);
    }

    /**
     * Invokes the callable method with the current AdminInfo set.
     *
     * @param toWrap    Callable method
     * @return  The type specified from the callable method
     * @throws Exception
     */
    public <OUT> OUT invokeCallable(final Callable<OUT> toWrap) throws Exception {
        final Callable<OUT> wrap = wrapWithSubject(subject, wrapWithConnectionInfo(ip, toWrap));
        return wrap.call();
    }   
}
