package com.l7tech.external.assertions.jsonwebtoken.server;

import com.l7tech.external.assertions.jsonwebtoken.jsonwebtoken.JwtHeader;
import com.l7tech.external.assertions.jsonwebtoken.jsonwebtoken.exceptions.MissingJwtClaimsException;
import com.l7tech.external.assertions.jsonwebtoken.jsonwebtoken.exceptions.MultipleJwtClaimsException;
import com.l7tech.json.InvalidJsonException;
import com.l7tech.json.JSONData;
import com.l7tech.json.JSONFactory;
import org.junit.Test;

import java.io.IOException;
import java.util.Map;

/**
 * User: rseminoff
 * Date: 28/02/13
 */
public class QuickJsonTest {

    @Test
    public void testDuplicateClaims() throws InvalidJsonException {

        String duplicateClaims = "{ \"iss\":\"claims\",\r\n" +
                                 "  \"iss\":\"claims2\" }";

        JSONData jsonData = JSONFactory.getInstance().newJsonData(duplicateClaims);

        String algorithm = null;
        String tokenType = null;
        try {
            Map<String, String> objectMap = (Map<String, String>) jsonData.getJsonObject();
            algorithm = objectMap.get("iss");   // look for "alg" claim in the header.
            tokenType = objectMap.get("typ");   // Look for "typ" claim in the header.  This may not be present.
        } catch (InvalidJsonException e) {
            // Header has failed.  Throw InvalidJsonException.
            throw new InvalidJsonException("The new JWT Header is not valid JSON and cannot be used");
        }
    }

    @Test (expected=MultipleJwtClaimsException.class)
    public void testMultipleClaimsMethod() throws InvalidJsonException, MultipleJwtClaimsException, MissingJwtClaimsException, IOException {
        JwtHeader header = new JwtHeader();
        String duplicateClaims = "\"iss\":\"claims\",\r\n" +
                                 "\"iss\":\"claims2\"";
        header.appendToHeader(duplicateClaims);
    }

}
