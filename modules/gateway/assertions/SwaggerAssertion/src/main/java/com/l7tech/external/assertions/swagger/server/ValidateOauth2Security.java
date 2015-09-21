package com.l7tech.external.assertions.swagger.server;

import com.l7tech.message.HttpRequestKnob;
import io.swagger.models.auth.SecuritySchemeDefinition;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created with IntelliJ IDEA.
 * User: rballantyne
 * Date: 9/11/15
 * Time: 10:33 AM
 */
public class ValidateOauth2Security implements ValidateSecurity {

    private static final Pattern oauth2inHeader = Pattern.compile("^\\s*Bearer\\s+", Pattern.CASE_INSENSITIVE);
    private static final String ACCESS_TOKEN_PARAM = "access_token";

    public boolean checkSecurity(HttpRequestKnob httpRequestKnob, SecuritySchemeDefinition securityDefinition) {
        boolean found = false;
        String authHeaders[] = httpRequestKnob.getHeaderValues(ServerSwaggerAssertion.AUTHORIZATION_HEADER);
        if(authHeaders != null) {
            for (String header : authHeaders) {
                Matcher m = oauth2inHeader.matcher(header);
                if (m.find()) {
                    found = true;
                    break;
                }
            }
        }
        //alternative way of finding the access token
        try {
            String[] values = httpRequestKnob.getParameterValues(ACCESS_TOKEN_PARAM);
            if(values != null && values.length > 0) {
                if(values.length > 1){
                    return false;
                }

                found ^= true;
            }
        } catch (IOException e) {
            return false;
        }
        return found;
    }
}