package com.l7tech.external.assertions.websocket.server;

import com.l7tech.server.message.AuthenticationContext;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;

/**
 * Created with IntelliJ IDEA.
 * User: cirving
 * Date: 8/23/12
 * Time: 9:50 AM
 * To change this template use File | Settings | File Templates.
 */
public class WebSocketMetadata {

    private String id;
    private AuthenticationContext authenticationContext;
    private String accessToken;
    private HashMap contextVariables;



    public WebSocketMetadata(String id, AuthenticationContext authenticationContext, HttpServletRequest request, HashMap contextVariables) {
        this.id = id;
        this.authenticationContext = authenticationContext;
        this.accessToken = extractOAuthToken(request);
        this.contextVariables = contextVariables;
    }

    private String extractOAuthToken( HttpServletRequest request) {
        if ( request.getHeader("Authorization") != null) {
            String oAuthAuthorizationHeaderValue = request.getHeader("Authorization");
            return oAuthAuthorizationHeaderValue.trim().substring(oAuthAuthorizationHeaderValue.indexOf(' ')+1);
        }

        if ( request.getParameter("access_token") != null) {
            return request.getParameter("access_token");
        }

        return null;
    }

    public boolean hasAccessToken() {
        return accessToken != null;
    }

    public String getId() {
        return id;
    }

    public AuthenticationContext getAuthenticationContext() {
        return authenticationContext;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public HashMap getContextVariables() {
        return contextVariables;
    }
}
