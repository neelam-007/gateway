package com.l7tech.external.assertions.jsonwebtoken.jsonwebtoken;

import com.l7tech.external.assertions.jsonwebtoken.jsonwebtoken.exceptions.MissingJwtClaimsException;
import com.l7tech.external.assertions.jsonwebtoken.jsonwebtoken.exceptions.MultipleJwtClaimsException;
import com.l7tech.json.InvalidJsonException;
import com.l7tech.json.JSONData;
import com.l7tech.json.JSONFactory;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonToken;

import java.io.IOException;
import java.util.HashSet;
import java.util.Map;

/**
 * User: rseminoff
 * Date: 27/02/13
 */
public class JwtHeader {

    // This defines a simple JWT header with signature algorithm, encryption algorithm
    // and token type claims.  The rest can be added by assertion variable.
    // The entire header can also be specified by assertion variable.

    // This class is mainly used by the JsonWebToken class to define the header.

    private String algorithmName;
    private String encryptionName;
    private final String tokenType;
    private String appendedClaims;
    private String generatedHeader;

    private Map<String, String> claims;

    public JwtHeader() {
        super();
        this.algorithmName = "none";
        this.encryptionName = null;
        this.tokenType = "JWT"; // It must be this type.
        this.appendedClaims = null;
        this.generatedHeader = null;
    }

    /**
     * This assigns a full, custom JWT Header.  It performs validation and duplicate claims
     * checking on the header first before assigning.
     *
     * Throws: InvalidJsonException if JSON is invalid, or duplicate claims are found.
     *
     */
    public void setCustomHeader(String header) throws InvalidJsonException, MultipleJwtClaimsException, MissingJwtClaimsException, IOException {

        // Attempt to set a new JWT header.
        // First, validate the passed header and if it validates and contains AT MINIMUM
        // the 'alg' claim, then it's considered legit and allowed to become the new header.
        this.validateHeader(header, true);

        // Header is legit.
        generatedHeader = header;
    }

    public void appendToHeader(String header) throws InvalidJsonException, MultipleJwtClaimsException, MissingJwtClaimsException, IOException {

        if (header.trim().length() == 0) {
            // The passed text to append is empty.
            throw new MissingJwtClaimsException("The JSON payload to append to the header is empty");
        }

        if (header.startsWith("{")) {
            throw new InvalidJsonException("Appended Claims need to be JSON fragments, not a JSON document starting with '{'");
        }

        if (generatedHeader != null) {
            throw new InvalidJsonException("The header has already been generated, no additional claims can be appended to it");
        }

        // This will append claims to the default header.
        // Append it to the existing header, then validate the whole header.
//        appendedClaims = header;
//        String interimHeader = generateHeader();

        String interimHeader = "{" + header + "}"; // Added for checking purposes.

        // Validate it, ensure it has ALG claim.
        this.validateHeader(interimHeader, false);

        // Header is legit.
//        generatedHeader = interimHeader;    // All validates with new claims.
        appendedClaims = header;

    }

    public void setSignatureAlgorithm(String alg) {
        this.algorithmName = alg;
    }
    public String getSignatureAlgorithm() {
        return this.algorithmName;
    }

    // For when it's time.
//    public void setEncryptionAlgorithm() {
//    }

//    public String getEncryptionAlgorithm() {
//        return encryptionName;
//    }

    public String getHeader() {
        return this.generateHeader();
    }

    public String getName(String name) {
        return claims.get(name);
    }

    private String generateHeader() {
        if (generatedHeader != null) {
            return generatedHeader;    // Already generated (custom, perhaps), just return it.
        }

        // This creates a basic header
        StringBuilder interimHeader = new StringBuilder("{\"typ\":\"" + this.tokenType + "\",\r\n \"alg\":\"" + this.algorithmName + "\"");
        //+ (appendClosingBrace ? "}" : "");
        // If there are claims added to this header, add them now.
        if (appendedClaims != null) {
            interimHeader.append(",\r\n" + appendedClaims);
        }

        interimHeader.append("}");
        return interimHeader.toString();
    }

    private void validateHeader(String header, boolean checkForAlgorithmClaim) throws InvalidJsonException, MissingJwtClaimsException, MultipleJwtClaimsException, IOException {

        JSONData jsonHeader = JSONFactory.getInstance().newJsonData(header);
        String algorithm = null;

        Map<String, String> objectMap = (Map<String, String>) jsonHeader.getJsonObject();

        if (checkForAlgorithmClaim) {
            if (!objectMap.containsKey("alg")) {
                throw new MissingJwtClaimsException("No algorithm claims have been found in the header");
            }

            // The algorithm is needed if its specified in the header.
            algorithm = objectMap.get("alg");
            if (algorithm == null) {
                throw new MissingJwtClaimsException("No algorithm claim is null.");
            }

            algorithmName = algorithm;  // Assigns the algorithm name from the header.
        }

        if (objectMap.containsKey("typ")) {
            // MAG-71...corrects being able to specify an invalid header type.
            // At the moment, the only supported type is JWT or JWS, however ...
            // NO ALGORITHM CHECKS ARE DONE TO CONFIRM IF IT IS INDEED A JWT OR JWS!
            Object type = objectMap.get("typ");
            if (type instanceof String) {
                String tType = (String)type;
                if ( (tType.compareTo("JWT") != 0) && (tType.compareTo("JWS") != 0) ) {
                    // This is an invalid header.
                    throw new InvalidJsonException("JWT header 'typ' claim is invalid.  It must be 'JWT'");
                }
            }
        }

        // Check for multiple claims.
        checkForMultipleClaims(header);
        claims = objectMap; // Only if everything passes.
    }

    private void checkForMultipleClaims(String payload) throws MultipleJwtClaimsException, IOException {
        HashSet<String> claims = new HashSet<String>();

        // Go through each line, getting the claim key.  Stash claim key in hashSet,
        // checking first if it exists.  First duplicate key raises an exception.
        // This directly uses the Jackson JSON classes as they are shipped with 7.0, and may have to be
        // modified in the future if Jackson JSON is removed from future releases.
        JsonFactory factory = new JsonFactory();
        JsonParser parser = factory.createJsonParser(payload);
        JsonToken token = parser.nextToken();

        while (parser.hasCurrentToken()) {

            if (token == JsonToken.FIELD_NAME) {
                String currentField = parser.getCurrentName();
                if (claims.contains(currentField)) {
                    //A duplicate name has been found.
                    throw new MultipleJwtClaimsException("Found duplicate " + currentField + " claim.");
                } else {
                    claims.add(currentField);
                }
            }
            token = parser.nextToken();
        }
    }
}
