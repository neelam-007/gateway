package com.l7tech.server.saml;

import x0Protocol.oasisNamesTcSAML1.ResponseDocument;
import x0Protocol.oasisNamesTcSAML1.RequestDocument;

/**
 * @author emil
 * @version 5-Aug-2004
 */
public class AuthorityImpl implements Authority {
    public ResponseDocument process(RequestDocument request) throws SamlException {
       return RequestResolver.resolve(request).getResponse();
    }
}
