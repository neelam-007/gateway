package com.l7tech.adminws.security;

import org.apache.axis.MessageContext;
import org.apache.axis.AxisFault;

/**
 * Layer 7 Technologies, inc.
 * User: flascelles
 * Date: June 2, 2003
 *
 * This axis class handler verifies that an authenticated user is authorised to make an admin request.
 *
 * It relies on a previous handler to authenticate credentials and feed the message handler with the ID of the
 * authenticated user (com.l7tech.adminws.security.AuthenticationAxisHandler).
 *
 * It is important that this handler come after the credential parsing and the authentication handlers
 * as in the sample below:
 *
 * <transport name="http">
 *   <requestFlow>
 *     <handler type="URLMapper"/>
 *     <handler type="java:com.l7tech.adminws.security.AuthenticationAxisHandler"/>
 *     <handler type="java:com.l7tech.adminws.security.AuthorizationAxisHandler"/>
 *   </requestFlow>
 * </transport>
 */
public class AuthorizationAxisHandler extends InternalIDSecurityAxisHandler {

    /**
     * Invoked by the axis engine. Looks for a property in the Message Context
     * whose key is AuthenticationAxisHandler.AUTHENTICATED_USER and whose
     * value is the authenticated user object. If this user is authorized to
     * make the request, this handler succeeds. OTherwise, an exception is
     * thrown (org.apache.axis.AxisFault).
     */
    public void invoke(MessageContext messageContext) throws AxisFault {
        Long authedUserId = (Long)messageContext.getProperty(AUTHENTICATED_USER);
        // get the internal provider
        if (authedUserId != null && userIsMemberOfGroup(authedUserId.longValue(), "SSGAdmin")) {
            // user is authorized
            return;
        }
        else
        {
            throw new AxisFault("Server.Unauthorized", "com.l7tech.adminws.security.AuthorizationAxisHandler failed", null, null );
        }
    }
}
