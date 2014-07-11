package com.l7tech.external.assertions.jsonwebtoken.server;

import com.l7tech.external.assertions.jsonwebtoken.JwtEncodeAssertion;
import com.l7tech.external.assertions.jsonwebtoken.jsonwebsignature.algorithms.JwsNone;
import com.l7tech.external.assertions.jsonwebtoken.jsonwebtoken.JsonWebToken;
import com.l7tech.external.assertions.jsonwebtoken.jsonwebtoken.JwtUtilities;
import com.l7tech.external.assertions.jsonwebtoken.jsonwebtoken.exceptions.IllegalJwtSignatureException;
import com.l7tech.external.assertions.jsonwebtoken.jsonwebtoken.exceptions.MissingJwtClaimsException;
import com.l7tech.external.assertions.jsonwebtoken.jsonwebtoken.exceptions.MultipleJwtClaimsException;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.gateway.common.security.keystore.SsgKeyEntry;
import com.l7tech.gateway.common.security.password.SecurePassword;
import com.l7tech.json.InvalidJsonException;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.variable.NoSuchVariableException;
import com.l7tech.policy.variable.Syntax;
import com.l7tech.server.DefaultKey;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.AbstractServerAssertion;
import com.l7tech.server.policy.variable.ExpandVariables;
import com.l7tech.server.security.keystore.SsgKeyStoreManager;
import com.l7tech.server.security.password.SecurePasswordManager;
import com.l7tech.util.GoidUpgradeMapper;
import org.springframework.context.ApplicationContext;

import javax.inject.Inject;
import java.io.IOException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.text.ParseException;
import java.util.Map;

/**
 * This encodes a JSON Web Token according to the JWT Draft Standard and the JSON Web Algorithms Draft Standard.
 * <p/>
 * User: rseminoff
 * Date: 27/11/12
 */
public class ServerJwtEncodeAssertion extends AbstractServerAssertion<JwtEncodeAssertion> {

    @Inject
    private SecurePasswordManager securePasswordManager;
    private final SsgKeyStoreManager ssgKeyStoreManager;
    private final DefaultKey defaultKey;

    private final String[] variablesUsed;

    public ServerJwtEncodeAssertion(final JwtEncodeAssertion assertion, ApplicationContext context) throws PolicyAssertionException {
        super(assertion);
        this.variablesUsed = assertion.getVariablesUsed();
        this.ssgKeyStoreManager = context.getBean("ssgKeyStoreManager", SsgKeyStoreManager.class);
        this.defaultKey = context.getBean("defaultKey", DefaultKey.class);
    }

    @Override
    public AssertionStatus checkRequest(PolicyEnforcementContext context) throws IOException, PolicyAssertionException {

        Map<String, Object> vars = context.getVariableMap(this.variablesUsed, getAudit());

        // Get the variable contents of the Json Payload variable.
        int headerType = assertion.getJwtHeaderType();
        String jsonHeader = null;
        String algorithm = null;
        String jsonPayload = ExpandVariables.process(assertion.getJsonPayload(), vars, getAudit(), true);
        String outputVariable = assertion.getOutputVariable();
        String secret;

        // Corrects MAG-66, MAG-63
        if ( (jsonPayload == null) || (jsonPayload.trim().length() == 0) ) {
            // The payload is invalid.  Stop all processing for this token.
            logAndAudit(AssertionMessages.ASSERTION_MISCONFIGURED, "(JWT Encode) The JSON Payload is not not specified or empty.  The JSON Web Token cannot be created.");
            return AssertionStatus.FAILED;
        }

        // Corrects MAG-64
        if ( (outputVariable == null) || (outputVariable.trim().length() == 0) ) {
            logAndAudit(AssertionMessages.ASSERTION_MISCONFIGURED, "(JWT Encode) No output variable was configured to place the encoded token.");
            return AssertionStatus.FAILED;
        }

        // New header processing.
        // Start with the header variable, only if claims or header is specified.
        if (headerType != JwtUtilities.NO_SUPPLIED_HEADER_CLAIMS) {
            String headerVar = assertion.getJwtHeaderVariable().trim();
            if (headerVar.isEmpty()) {
                logAndAudit(AssertionMessages.ASSERTION_MISCONFIGURED, "(JWT Encode) A Full JWT Header was declared, but the variable containing the header was not configured.");
                return AssertionStatus.FAILED;
            }

            try {
                if (context.getVariable(assertion.getJwtHeaderVariable()) instanceof String) {
                    jsonHeader = (String)context.getVariable(assertion.getJwtHeaderVariable());
                } else {
                    logAndAudit(AssertionMessages.ASSERTION_MISCONFIGURED, "(JWT Encode) The passed header is not a String type and cannot be used.");
                    return AssertionStatus.FAILED;
                }
            } catch (NoSuchVariableException e) {
                logAndAudit(AssertionMessages.NO_SUCH_VARIABLE_WARNING, assertion.getJwtHeaderVariable());
                return AssertionStatus.FAILED;
            }
        }

        // Get the signature algorithm next, only if a full header isn't specified.
        if (headerType != JwtUtilities.SUPPLIED_FULL_JWT_HEADER) {
            // Get the algorithm
            switch (assertion.getSignatureSelected()) {
                case JwtUtilities.SELECTED_SIGNATURE_VARIABLE: {
                    algorithm = ExpandVariables.process(assertion.getSignatureValue(), vars, getAudit(), true);
                    break;
                }
                case JwtUtilities.SELECTED_SIGNATURE_LIST: {
                    algorithm = assertion.getSignatureValue();
                    break;
                }
                default: {  // Includes SELECTED_SIGNATURE_NONE
                    logAndAudit(AssertionMessages.ASSERTION_MISCONFIGURED, "(JWT Encode) The signature algorithm is not defined.");
                    return AssertionStatus.FAILED;
                }
            }
        }

        // Create the token, but don't set the secret at the moment.
        // The secret requires special methods in the token to set them based on their type.
        JsonWebToken token;

        try {
            switch (headerType) {
                case (JwtUtilities.SUPPLIED_FULL_JWT_HEADER): {
                    token = JsonWebToken.initTokenWithJwtHeader(jsonHeader, jsonPayload, null, false);
                    break;
                }
                case (JwtUtilities.SUPPLIED_PARTIAL_CLAIMS): {
                    token = JsonWebToken.initSignedTokenWithAppendedClaims(jsonHeader, jsonPayload, algorithm, null, false);
                    break;
                }
                default: {
                    token = JsonWebToken.initSimpleSignedToken(jsonPayload, algorithm, null, false);
                }
            }
        } catch (MissingJwtClaimsException e1) {
            logAndAudit(AssertionMessages.ASSERTION_MISCONFIGURED, "(JWT Encode) The " + (headerType == JwtUtilities.SUPPLIED_PARTIAL_CLAIMS ? "supplied" : "complete") + " JWT Header is missing required claims and cannot be used.");
            return AssertionStatus.FAILED;
        } catch (IllegalJwtSignatureException e1) {
            logAndAudit(AssertionMessages.ASSERTION_MISCONFIGURED, "(JWT Encode) The " + (headerType == JwtUtilities.SUPPLIED_PARTIAL_CLAIMS ? "supplied" : "complete") + " JWT Header is claiming an unsupported signature type and cannot be used.");
            return AssertionStatus.FAILED;
        } catch (MultipleJwtClaimsException e1) {
            logAndAudit(AssertionMessages.ASSERTION_MISCONFIGURED, "(JWT Encode) The " + (headerType == JwtUtilities.SUPPLIED_PARTIAL_CLAIMS ? "supplied" : "complete") + " JWT Header contains duplicate claims and cannot be used.");
            return AssertionStatus.FAILED;
        } catch (InvalidJsonException e1) {
            logAndAudit(AssertionMessages.ASSERTION_MISCONFIGURED, "(JWT Encode) The " + (headerType == JwtUtilities.SUPPLIED_PARTIAL_CLAIMS ? "supplied" : "complete") + " JWT Header and/or JSON payload is not valid JSON and cannot be used. (" + e1.getMessage() + ")");
            return AssertionStatus.FAILED;
        } catch (Exception e) {
            // MAG-70.  If encoded secrets less than 4 chars are used, this is shown.
            logAndAudit(AssertionMessages.ASSERTION_MISCONFIGURED, "(JWT Encode) Unable to create token due to error. (" + e.getMessage() + ")");
            return AssertionStatus.FAILED;
        }


        // Then the secret.
        switch (assertion.getAlgorithmSecretLocation()) {
            case JwtUtilities.SELECTED_SECRET_KEY: {
                // The secret is in a private key alias.
                // We need to split the key along "." to get it's OID and alias
                // The secret is passed in a byte array, and if it's a private key, it's one of two formats:
                // A variable containing a private key, or a SSG Reference to the Private Key.
                String keyAlias;
                Goid keystoreGOID = null;
                long keystoreOID = Long.MIN_VALUE;

                String privateKeyReference[] = assertion.getAlgorithmSecretValue().split("[.]");
                if (privateKeyReference.length == 2) {
                    keyAlias = privateKeyReference[1];
                    try {
                        keystoreGOID = Goid.parseGoid(privateKeyReference[0]);
                    } catch (IllegalArgumentException iae) {
                        try {
                            keystoreOID = Long.parseLong(privateKeyReference[0]);
                        } catch (NumberFormatException nfe) {
                            // Unable to get a private key reference.
                            logAndAudit(AssertionMessages.ASSERTION_MISCONFIGURED, "(JWT Encode) Error while getting Private Key - Unable to parse the reference");
                            return AssertionStatus.FAILED;
                        }
                    }
                } else {
                    // It's an invalid reference.
                    logAndAudit(AssertionMessages.ASSERTION_MISCONFIGURED, "(JWT Encode) Error while getting Private Key - Invalid Reference");
                    return AssertionStatus.FAILED;
                }

                SsgKeyEntry privateKey;

                if (keystoreGOID == null) {
                    // An OID was retrieved from the policy
                    keystoreGOID = GoidUpgradeMapper.mapOid(EntityType.SSG_KEY_ENTRY, keystoreOID);
                }

                try {
                    if (keyAlias == null && (Goid.isDefault(keystoreGOID))) {
                        // There is no key alias selected.  This means use the gateway default.
                        privateKey = defaultKey.getSslInfo();
                    } else {
                        privateKey = ssgKeyStoreManager.lookupKeyByKeyAlias(keyAlias, keystoreGOID);
                    }
                } catch (Exception e) {
                    logAndAudit(AssertionMessages.ASSERTION_MISCONFIGURED, "(JWT Encode) An error occurred reading the private key data from the database (" + e.getMessage() + ")");
                    return AssertionStatus.FAILED;
                }

                // There should be a private key at this point.
                if (privateKey != null && privateKey.isPrivateKeyAvailableAndAccessible()) {
                    // We do indeed have a private key.
                    try {
                        // Corrects MAG-67
                        privateKey.getCertificate().checkValidity();
                        token.setSecretAsPrivateKey(privateKey.getPrivate());
                    } catch (UnrecoverableKeyException e) {
                        logAndAudit(AssertionMessages.ASSERTION_MISCONFIGURED, "(JWT Encode) Unable to access the private key in the current context");
                        return AssertionStatus.FAILED;
                    } catch (CertificateExpiredException e) {
                        logAndAudit(AssertionMessages.ASSERTION_MISCONFIGURED, "(JWT Encode) Unable to access the private key in the current context, it has expired and cannot be used.");
                        return AssertionStatus.FAILED;
                    } catch (CertificateNotYetValidException e) {
                        logAndAudit(AssertionMessages.ASSERTION_MISCONFIGURED, "(JWT Encode) Unable to access the private key in the current context, it is not yet valid and cannot be used.");
                        return AssertionStatus.FAILED;
                    }
                } else {
                    // We don't have a private key.
                    logAndAudit(AssertionMessages.ASSERTION_MISCONFIGURED, "(JWT Encode) An error occurred with the private key - it is not available.");
                    return AssertionStatus.FAILED;    // We can't securely generate a signature without a private key.
                }
                break;
            }
            case JwtUtilities.SELECTED_SECRET_PASSWORD: {
                // The secret is in the password store
                SecurePassword passwordEncrypted;
                try {
                    try {
                        passwordEncrypted = securePasswordManager.findByPrimaryKey(Goid.parseGoid(assertion.getAlgorithmSecretValue()));
                    } catch (IllegalArgumentException e) {
                        try {
                            passwordEncrypted = securePasswordManager.findByPrimaryKey(GoidUpgradeMapper.mapOid(EntityType.SECURE_PASSWORD, Long.parseLong(assertion.getAlgorithmSecretValue())));
                            logAndAudit(AssertionMessages.ASSERTION_MISCONFIGURED, "(JWT Encode) Attempting to locate password using old-style OID, which may fail.  The password should be reselected in this policy and saved.");
                        } catch (NumberFormatException nfe) {
                            logAndAudit(AssertionMessages.ASSERTION_MISCONFIGURED, "(JWT Encode) Unable to determine the identity of the password to use.");
                            return AssertionStatus.FAILED;
                        }
                    }
                    // Password was not found.
                    if (passwordEncrypted == null) {
                        logAndAudit(AssertionMessages.ASSERTION_MISCONFIGURED, "(JWT Encode) Error while looking up Password: Not Found");
                        return AssertionStatus.FAILED;
                    } else {
                        // else password was found so lets decrypt it
                        secret = new String(securePasswordManager.decryptPassword(passwordEncrypted.getEncodedPassword()));
                    }
                } catch (FindException e) {
                    logAndAudit(AssertionMessages.ASSERTION_MISCONFIGURED, "(JWT Encode) Error while looking up Password");
                    return AssertionStatus.FAILED;
                } catch (ParseException e) {
                    logAndAudit(AssertionMessages.ASSERTION_MISCONFIGURED, "(JWT Encode) Error while decrypting Password");
                    return AssertionStatus.FAILED;
                }

                if (secret.length() > 0) {
                    // We have a secret to pass along. The gateway hands us plaintext passwords.
                    token.setSecret(secret, false);
                }
                break;
            }
            case JwtUtilities.SELECTED_SECRET_VARIABLE:
            case JwtUtilities.SELECTED_SECRET_VARIABLE_BASE64: {
                // Expand the named variable
                // MAG-72 : Regardless of what type of variable the secret is, it's always returned as a String.
                Object theSecret = null;
                try {
                    String[] varSecret = Syntax.getReferencedNames(assertion.getAlgorithmSecretValue());

                    if (varSecret.length >= 1) {
                        theSecret = context.getVariable(varSecret[0]);
                    } else {
                        theSecret = context.getVariable(assertion.getAlgorithmSecretValue());
                    }
                } catch (NoSuchVariableException e) {
                    // Is the algorithm used "NONE"?
                    if ( (algorithm != null) && (!(algorithm.equalsIgnoreCase(JwsNone.jwsAlgorithmName))) ) {
                        logAndAudit(AssertionMessages.ASSERTION_MISCONFIGURED, "(JWT Encode) Can't find the specified variable: '" + assertion.getAlgorithmSecretValue() + "'");
                        return AssertionStatus.FAILED;
                    }
                    // No problem otherwise.  This is an expected behaviour as the UI doesn't contain a "None" radio button for secret type in this version.
                }

                if (theSecret != null) {
                    if (!(theSecret instanceof String)) {
                        // The secret is not a string, and cannot be used to secure the token.  This is an error.
                        logAndAudit(AssertionMessages.ASSERTION_MISCONFIGURED, "(JWT Encode) The passed secret in variable '" + assertion.getAlgorithmSecretValue() + "' is not a String and cannot be used to encode the token.");
                        return AssertionStatus.FAILED;
                    }

                    secret = (String)theSecret;

                    if (secret.length() > 0) {
                        // We have a secret to pass along.  Is it base64 encoded?
                        try {
                            token.setSecret(secret, (assertion.getAlgorithmSecretLocation() & JwtUtilities.SELECTED_SECRET_VARIABLE_BASE64) == JwtUtilities.SELECTED_SECRET_VARIABLE_BASE64);
                        } catch (IllegalArgumentException e) {
                            // MAG-70.  If encoded secrets less than 4 chars are used, this is shown.
                            logAndAudit(AssertionMessages.ASSERTION_MISCONFIGURED,"(JWT Encode) Unable to create token due to error. (" + e.getMessage() + ")");
                            return AssertionStatus.FAILED;
                        }
                    }
                }

                break;
            }
            default: {  // Includes SELECTED_SECRET_NONE
                // This line isn't needed, but was put here to explicitly declare that secret was null in this case.
                secret = null;  // No secret is defined.  Not an issue unless it's needed.
            }
        }

        // Get the token
        byte[] tokenOut = token.getToken();
        if ((tokenOut == null) || (tokenOut.length == 0)) {
            // Something went wrong with the token creation.
            logAndAudit(AssertionMessages.ASSERTION_MISCONFIGURED, "(JWT Encode) Unable to create the JSON Web Token");
            return AssertionStatus.FAILED;
        }

        context.setVariable(outputVariable, new String(tokenOut));
        return AssertionStatus.NONE;
    }

}
