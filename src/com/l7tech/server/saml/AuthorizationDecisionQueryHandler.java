package com.l7tech.server.saml;

import x0Protocol.oasisNamesTcSAML1.RequestDocument;
import x0Protocol.oasisNamesTcSAML1.ResponseDocument;

/**
 * @author emil
 * @version 28-Jul-2004
 */
public class AuthorizationDecisionQueryHandler implements SamlRequestHandler {
    private RequestDocument request;
    private ResponseDocument response;

    /**
     * Package private constructor
     * @param request
     */
    AuthorizationDecisionQueryHandler(RequestDocument request) {
        if (request == null) {
            throw new IllegalArgumentException();
        }
        this.request = request;
    }

    /**
     * @return the saml request that is processed by the handler
     */
    public RequestDocument getRequest() {
        return request;
    }

    /**
     * @return the saml response
     */
    public ResponseDocument getResponse() {
        if (response !=null) {
            return response;
        }
        response = Responses.getNotImplementedResponse("Authorization query not implemented");
        return response;
    }
}
