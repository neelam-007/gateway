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
 * It relies on a previous handler to authenticate credentials and feed the message handler with a user object
 * (com.l7tech.adminws.security.AuthenticationAxisHandler).
 *
 * It is important that this handler come after the credential parsing and the authentication handlers
 * as in the sample below:
 *
 * <transport name="http">
 *   <requestFlow>
 *     <handler type="URLMapper"/>
 *     <handler type="java:org.apache.axis.handlers.http.HTTPAuthHandler"/>
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
        com.l7tech.identity.User authedUser = (com.l7tech.identity.User)messageContext.getProperty(AUTHENTICATED_USER);
        long callingUserOid = authedUser.getOid();
        // get the internal provider
        // retreive the internal identity provider
        com.l7tech.identity.IdentityProvider internalProvider = getInternalIDProvider();
        // get the admin group from the internal provider
        com.l7tech.identity.Group adminGroup = null;
        // todo
        // verify that calling user is member of that group
        java.util.Collection members = adminGroup.getMemberHeaders();
        java.util.Iterator i = members.iterator();
        while (i.hasNext()) {
            com.l7tech.objectmodel.EntityHeader header = (com.l7tech.objectmodel.EntityHeader)i.next();
            if (header.getOid() == callingUserOid)
                // success!
                return;
        }
        throw new AxisFault("User " + authedUser + " (oid: " + callingUserOid + ") not member of admin group");
    }
}
