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

    private static final Pattern oauth2inHeader = Pattern.compile("^\\s*bearer\\s+", Pattern.CASE_INSENSITIVE);

    public boolean checkSecurity(HttpRequestKnob httpRequestKnob, SecuritySchemeDefinition securityDefinition) {

        boolean inAuthHeader = false;
        boolean inParameters = false;

        String authHeaders[] = httpRequestKnob.getHeaderValues(ServerSwaggerAssertion.AUTHORIZATION_HEADER);
        if(authHeaders != null) {
            for (String header : authHeaders) {
                Matcher m = oauth2inHeader.matcher(header);
                if (m.find()) {
                    inAuthHeader = true;
                }
            }
        }
        //alternative way of finding the access token
        try {
            inParameters = (httpRequestKnob.getParameterValues("access_token").length == 1);
        } catch (IOException e) {
            //TODO: decide appropriate response
            //  IOException here means api_key was multi-valued parameter!!
            //  if legit return true;
            //  if not fall through and return false below
            return false;
        }

        return ( (inAuthHeader || inParameters) &&  !( inAuthHeader && inParameters) );
    }
}