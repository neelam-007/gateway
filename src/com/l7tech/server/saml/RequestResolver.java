package com.l7tech.server.saml;

import x0Protocol.oasisNamesTcSAML1.RequestDocument;
import x0Protocol.oasisNamesTcSAML1.RequestType;
import x0Protocol.oasisNamesTcSAML1.ResponseDocument;

/**
 * Utility class that resolves the handler for the given sanl request.
 * 
 * @author emil
 * @version 28-Jul-2004
 */
public class RequestResolver {

    /** cannot instantiate this class */
    private RequestResolver() {
    }

    /**
     * Resolve the request handler for the incoming request.
     *
     * @param rdoc the trequest document
     * @return the <code>SamlRequestHandler</code> for a given request document
     */
    static SamlRequestHandler resolve(RequestDocument rdoc) {
        RequestType request = rdoc.getRequest();

        if (request.isSetAuthenticationQuery()) {
            return new AuthenticationQueryHandler(rdoc);
        } else if (request.isSetAttributeQuery()) {
            return new AttributeQueryHandler(rdoc);
        } else if (request.isSetAuthorizationDecisionQuery()) {
            return new AuthorizationDecisionQueryHandler(rdoc);
        }
        return new UnknownQueryHandler(rdoc);
    }


    private static class UnknownQueryHandler implements SamlRequestHandler {
        private RequestDocument request;

        /**
         * Package private constructor
         * @param request
         */
        UnknownQueryHandler(RequestDocument request) {
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
            return Responses.getEmptySuccess("Unknown saml request");
        }
    }
}
