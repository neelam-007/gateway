package com.l7tech.adminws.security;

/**
 * Layer 7 Technologies, inc.
 * User: flascelles
 * Date: June 2, 2003
 * To change this template use Options | File Templates.
 */
public abstract class InternalIDSecurityAxisHandler extends org.apache.axis.handlers.BasicHandler {
    protected com.l7tech.identity.IdentityProvider getInternalIDProvider() {
        // todo
        return null;
    }

    /**
     * key used by the com.l7tech.adminws.security handlers to get or set the authenticated
     * user inside the axis MessageContext (org.apache.axis.MessageContext)
     *
     * ex.:
     * com.l7tech.identity.User authedUser = MessageContext.getProperty(AUTHENTICATED_USER);
     */
    static final String AUTHENTICATED_USER = "Authenticated_com.l7tech.identity.User";
}
