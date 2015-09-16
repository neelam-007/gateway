package com.l7tech.external.assertions.swagger.server;

import com.l7tech.external.assertions.swagger.SwaggerAssertion;
import com.l7tech.message.HttpRequestKnob;
import io.swagger.models.auth.ApiKeyAuthDefinition;
import io.swagger.models.auth.In;
import io.swagger.models.auth.SecuritySchemeDefinition;

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

    private static final String HEADER = "header";
    private static final String QUERY = "query";


    public boolean checkSecurity(HttpRequestKnob httpRequestKnob, SecuritySchemeDefinition securityDefinition) {

        if (! (securityDefinition instanceof ApiKeyAuthDefinition)) {
            return false;
        }
        ApiKeyAuthDefinition apiKeyAuthDefinition = (ApiKeyAuthDefinition) securityDefinition;

        switch ( apiKeyAuthDefinition.getIn() ) {
            case HEADER:
                Pattern apiKey = Pattern.compile(apiKeyAuthDefinition.getName(), Pattern.CASE_INSENSITIVE);

                String authHeaders[] = httpRequestKnob.getHeaderValues(ServerSwaggerAssertion.AUTHORIZATION_HEADER);
                if (authHeaders != null) {
                    for (String header : authHeaders) {
                        Matcher m = apiKey.matcher(header);
                        if (m.find()) {
                            return true;
                        }
                    }
                }
                return false;

            case QUERY:
                try {
                    return (httpRequestKnob.getParameter(apiKeyAuthDefinition.getName()) != null);
                } catch (IOException e) {
                    //TODO: decide appropriate response
                    //  IOException here means api_key was multi-valued parameter!!
                    //  if legit return true;
                    //  if not fall through and return false below
                    return false;
                }

            default:
                return false;
        }
    }

}
