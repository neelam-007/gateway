package com.l7tech.server.saml;

import x0Protocol.oasisNamesTcSAML1.RequestDocument;
import x0Protocol.oasisNamesTcSAML1.RequestType;
import x0Protocol.oasisNamesTcSAML1.ResponseDocument;

/**
 * @author emil
 * @version 28-Jul-2004
 */
public class AuthenticationQueryHandler implements SamlRequestHandler {
    private RequestDocument request;
    private ResponseDocument response;

    /**
     * Package private constructor
     * @param request
     */
    AuthenticationQueryHandler(RequestDocument request) {
        if (request == null) {
            throw new IllegalArgumentException();
        }
        RequestType rt = request.getRequest();
        if (rt == null) {
            throw new CannotProcessException("missing samlp:Request", request);
        }
        if (!rt.isSetAuthenticationQuery()) {
            throw new IllegalArgumentException("Saml request does not contain "+request);
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
        return Responses.getEmpty("");
    }
}
