package com.l7tech.server.saml;

import x0Protocol.oasisNamesTcSAML1.ResponseDocument;
import x0Protocol.oasisNamesTcSAML1.RequestDocument;
import x0Protocol.oasisNamesTcSAML1.RequestType;

import javax.security.auth.Subject;
import java.security.GeneralSecurityException;

/**
 * @author emil
 * @version 5-Aug-2004
 */
public class AuthorityImpl implements Authority {


    public ResponseDocument process(RequestDocument request) throws SamlException {
       return resolve(request).getResponse();
    }

    /**
     * Authenticate the given <code>Subject</code> against the authority.
     *
     * @throws java.security.GeneralSecurityException
     *
     */
    public void authenticate(Subject subject) throws GeneralSecurityException {

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
