package com.l7tech.server.event;

import com.l7tech.identity.IdentityProviderConfig;
import com.l7tech.identity.User;
import com.l7tech.spring.remoting.RemoteUtils;

import javax.security.auth.Subject;
import java.util.logging.Logger;
import java.util.Set;
import java.security.AccessController;
import java.security.Principal;
import java.rmi.server.ServerNotActiveException;

/**
 * Holds information about a client admin that triggered some behavior.
 */
public class AdminInfo {
    public static final Logger logger = Logger.getLogger(AdminInfo.class.getName());

    public static final String LOCALHOST_IP = "127.0.0.1";
    public static final String LOCALHOST_SUBJECT = "localsystem";

    public AdminInfo(String login, String id, long ipOid, String ip) {
        this.ip = ip;
        this.id = id;
        this.identityProviderOid = ipOid;
        this.login = login;
    }

    public final String login;
    public final String id;
    public final long identityProviderOid;
    public final String ip;

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
            logger.warning("The administrative event caused as local call, outside of servicing an adminstrative remote call." +
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

        return new AdminInfo(login, uniqueId, providerOid, address);
    }
}
