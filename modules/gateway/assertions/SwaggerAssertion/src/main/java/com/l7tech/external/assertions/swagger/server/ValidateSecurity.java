package com.l7tech.external.assertions.swagger.server;

import com.l7tech.message.HttpRequestKnob;
import io.swagger.models.auth.SecuritySchemeDefinition;

/**
 * Created with IntelliJ IDEA.
 * User: rballantyne
 * Date: 9/11/15
 * Time: 10:22 AM
 */
public interface ValidateSecurity {

    public static enum SwaggerSecurityType  { BASIC, APIKEY, OAUTH2, INVALID }

    public boolean checkSecurity(HttpRequestKnob httpRequestKnob,SecuritySchemeDefinition securityDefinition);
}
