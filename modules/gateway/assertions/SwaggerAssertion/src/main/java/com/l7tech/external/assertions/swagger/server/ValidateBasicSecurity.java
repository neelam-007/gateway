package com.l7tech.external.assertions.swagger.server;

import com.l7tech.message.HttpRequestKnob;
import io.swagger.models.auth.SecuritySchemeDefinition;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created with IntelliJ IDEA.
 * User: rballantyne
 * Date: 9/11/15
 * Time: 11:54 AM
 */
public class ValidateBasicSecurity implements ValidateSecurity {

    private static final Pattern basicAuth = Pattern.compile("^Basic", Pattern.CASE_INSENSITIVE);

    public boolean checkSecurity(HttpRequestKnob httpRequestKnob, SecuritySchemeDefinition securityDefinition) {
        String authHeaders[] = httpRequestKnob.getHeaderValues(ServerSwaggerAssertion.AUTHORIZATION_HEADER);
        if (authHeaders != null) {
            for (String header : authHeaders) {
                Matcher m = basicAuth.matcher(header);
                if(m.find()) {
                    return true;
                }
            }
        }
        return false;
    }
}
