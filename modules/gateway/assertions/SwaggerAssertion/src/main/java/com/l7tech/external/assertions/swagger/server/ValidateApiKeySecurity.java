package com.l7tech.external.assertions.swagger.server;

import com.l7tech.message.HttpRequestKnob;
import io.swagger.models.auth.ApiKeyAuthDefinition;
import io.swagger.models.auth.SecuritySchemeDefinition;

import java.io.IOException;

/**
 * Created with IntelliJ IDEA.
 * User: rballantyne
 * Date: 9/11/15
 * Time: 10:31 AM
 */
public class ValidateApiKeySecurity implements ValidateSecurity {

    public boolean checkSecurity(HttpRequestKnob httpRequestKnob, SecuritySchemeDefinition securityDefinition) {

        if (! (securityDefinition instanceof ApiKeyAuthDefinition)) {
            return false;
        }
        ApiKeyAuthDefinition apiKeyAuthDefinition = (ApiKeyAuthDefinition) securityDefinition;

        switch ( apiKeyAuthDefinition.getIn() ) {
            case HEADER:
                String authHeaders[] = httpRequestKnob.getHeaderValues(apiKeyAuthDefinition.getName());
                return (authHeaders != null && authHeaders.length > 0);
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
