package com.l7tech.external.assertions.swagger.server;

import com.l7tech.external.assertions.swagger.SwaggerAssertion;
import com.l7tech.message.HttpRequestKnob;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created with IntelliJ IDEA.
 * User: rballantyne
 * Date: 9/11/15
 * Time: 10:31 AM
 */
public class ValidateApiKeySecurity implements ValidateSecurity {

    private static final Pattern apiKey = Pattern.compile("apikey", Pattern.CASE_INSENSITIVE);

    public boolean checkSecurity(HttpRequestKnob httpRequestKnob) {
        String authHeaders[] = httpRequestKnob.getHeaderValues(ServerSwaggerAssertion.AUTHORIZATION_HEADER);
        if(authHeaders != null) {
            for (String header : authHeaders) {
                Matcher m = apiKey.matcher(header);
                if (m.find()) {
                    return true;
                }
            }
        }
        try {
            return (httpRequestKnob.getParameter("apiKey") != null || httpRequestKnob.getParameter("api_key") != null);
        } catch (IOException e) {
            //TODO: decide appropriate response
            //  IOException here means api_key was multi-valued parameter!!
            //  if legit return true;
            //  if not fall through and return false below
            return false;
        }
    }
}
