package com.l7tech.server.saml;

import x0Protocol.oasisNamesTcSAML1.RequestDocument;
import x0Protocol.oasisNamesTcSAML1.ResponseDocument;

/**
 * Implementations process the saml request.
 *
 * @author emil
 */
public interface SamlRequestHandler {
    /**
     * @return the saml request document that is processed by the handler
     */
    RequestDocument getRequest();

    /**
     * Produce and return the response document for the request
     * @return the saml response document
     */
    ResponseDocument getResponse();
}
