package com.l7tech.server.saml;

import x0Protocol.oasisNamesTcSAML1.ResponseDocument;
import x0Protocol.oasisNamesTcSAML1.RequestDocument;

import javax.security.auth.login.LoginException;
import javax.security.auth.Subject;
import java.security.GeneralSecurityException;

/**
 * The <code>Authorty</code> implementations provide SAML protocol request/response
 * message exchange and authentication services.
 *
 * @author emil
 * @version 4-Aug-2004
 */
public interface Authority {
    /**
     *
     * @param request
     * @return
     * @throws SamlException
     */
    ResponseDocument process(RequestDocument request) throws SamlException;

    /**
     * Authenticate the given <code>Subject</code> against the authority.
     *
     * @throws GeneralSecurityException
     */
    void authenticate(Subject subject) throws GeneralSecurityException;

}
