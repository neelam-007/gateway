package com.l7tech.adminws.security;

import org.apache.axis.MessageContext;
import org.apache.axis.AxisFault;

/**
 * Layer 7 Technologies, inc.
 * User: flascelles
 * Date: June 2, 2003
 *
 * This axis class handler validates the credentials provided by the admin ws client against the internal
 * ssg identity provider.
 *
 * It relies on a previous handler to parse those credentials beforehand. Such a handler could be the
 * org.apache.axis.handlers.http.HTTPAuthHandler. It is important that the handler parsing the credentials
 * comes first in the flow declaration (server-config.wsdd) as in the sample below:
 *
 * <transport name="http">
 *   <requestFlow>
 *     <handler type="URLMapper"/>
 *     <handler type="java:org.apache.axis.handlers.http.HTTPAuthHandler"/>
 *     <handler type="java:com.l7tech.adminws.security.AuthenticationAxisHandler"/>
 *   </requestFlow>
 * </transport>
 */
public class AuthenticationAxisHandler extends InternalIDSecurityAxisHandler {
    /**
     * Invoked by the axis engine. if successful, will feed the messageContext with
     * a property whose key is AuthenticationAxisHandler.AUTHENTICATED_USER and whose
     * value is the authenticated user object. If authentication fails, this handler
     * will throw an exception (org.apache.axis.AxisFault).
     */
    public void invoke(MessageContext messageContext) throws AxisFault {
        // get the credentials
        String username = messageContext.getUsername();
        String passwd = messageContext.getPassword();
        if (username == null || username.length() < 1 || passwd == null || passwd.length() < 1) throw new AxisFault(NO_CREDS_MESSAGE);
        // retreive the internal identity provider
        com.l7tech.identity.IdentityProvider internalProvider = getInternalIDProvider();
        // compare those credentials against the internal id provider
        // todo
        // add user to message context for more validation (if necessary)
        com.l7tech.identity.User authedUser = null;
        messageContext.setProperty(AUTHENTICATED_USER, authedUser);
    }

    private static final String NO_CREDS_MESSAGE = "No username or password in message context";
}
