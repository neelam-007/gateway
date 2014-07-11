package com.l7tech.external.assertions.jsonwebtoken.server;

import com.l7tech.external.assertions.jsonwebtoken.JwtDecodeAssertion;
import com.l7tech.external.assertions.jsonwebtoken.jsonwebtoken.JsonWebToken;
import com.l7tech.external.assertions.jsonwebtoken.jsonwebtoken.JwtUtilities;
import com.l7tech.external.assertions.jsonwebtoken.jsonwebtoken.exceptions.IllegalJwtSignatureException;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.gateway.common.security.keystore.SsgKeyEntry;
import com.l7tech.gateway.common.security.password.SecurePassword;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.variable.NoSuchVariableException;
import com.l7tech.policy.variable.Syntax;
import com.l7tech.security.cert.TrustedCertManager;
import com.l7tech.server.DefaultKey;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.AbstractServerAssertion;
import com.l7tech.server.security.keystore.SsgKeyStoreManager;
import com.l7tech.server.security.password.SecurePasswordManager;
import com.l7tech.util.GoidUpgradeMapper;
import org.springframework.context.ApplicationContext;

import javax.inject.Inject;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.PublicKey;
import java.security.cert.*;
import java.text.ParseException;

/**
 * This encodes a JSON Web Token according to the JWT Draft Standard and the JSON Web Algorithms Draft Standard.
 * <p/>
 * User: rseminoff
 * Date: 27/11/12
 */
public class ServerJwtDecodeAssertion extends AbstractServerAssertion<JwtDecodeAssertion> {

    private final SsgKeyStoreManager ssgKeyStoreManager;
    private final TrustedCertManager tcm;
    private final DefaultKey defaultKey;
    private final String[] variablesUsed;
    @Inject private SecurePasswordManager securePasswordManager;

    public ServerJwtDecodeAssertion(final JwtDecodeAssertion assertion, ApplicationContext context) throws PolicyAssertionException {
        super(assertion);
        this.variablesUsed = assertion.getVariablesUsed();
        this.ssgKeyStoreManager = context.getBean("ssgKeyStoreManager", SsgKeyStoreManager.class);
        this.tcm = context.getBean("trustedCertManager", TrustedCertManager.class);
        this.defaultKey = context.getBean("defaultKey", DefaultKey.class);
    }

    @Override
    public AssertionStatus checkRequest(PolicyEnforcementContext context) throws IOException, PolicyAssertionException {

        Object tokenObj;
        String token;
        try {
            String[] tokenVariable = Syntax.getReferencedNames(assertion.getIncomingToken());

            if (tokenVariable.length >= 1) {
                tokenObj = context.getVariable(tokenVariable[0]);
            } else {
                tokenObj = context.getVariable(assertion.getIncomingToken());
            }

        } catch (NoSuchVariableException e) {
            logAndAudit(AssertionMessages.NO_SUCH_VARIABLE_WARNING, assertion.getIncomingToken());
            return AssertionStatus.FAILED;
        }

        String outputVariable = assertion.getOutputVariable();

        // Check to see if any of the variables we need are not defined.
        if (tokenObj instanceof String) {
            token = (String) tokenObj;
            if (outputVariable == null) {
                // SECRET can be NULL if there is no digital signature on the token.
                // We are missing critical variables and cannot continue.
                logAndAudit(AssertionMessages.ASSERTION_MISCONFIGURED, "(JWT Decode) The token or the output variable is missing.  Assertion cannot continue.");
                return AssertionStatus.FAILED;
            }

            if ((token.trim().isEmpty()) || (outputVariable.trim().isEmpty())) {
                // Critical variables are empty, we can not continue
                // (SECRET can be empty as long as there is no digital signature on the token)
                logAndAudit(AssertionMessages.ASSERTION_MISCONFIGURED, "(JWT Decode) The token value or the output variable value is empty.  Assertion cannot continue.");
                return AssertionStatus.FAILED;
            }
        } else {
            logAndAudit(AssertionMessages.ASSERTION_MISCONFIGURED, "(JWT Decode) Incoming JWT token in variable '"+assertion.getIncomingToken()+"'  is an unsupported type.");
            return AssertionStatus.FAILED;
        }

        // Validation begins here.
        JsonWebToken jsonToken = JsonWebToken.initEmptyToken();
        switch (assertion.getAlgorithmSecretLocation()) {
            case JwtUtilities.SELECTED_SECRET_KEY: {
                // The secret is in a private key alias.
                // We need to split the key along "." to get it's OID and alias
                String keyAlias;
                Goid keyStoreGOID = null;
                long keyStoreId = Long.MIN_VALUE;

                String privateKeyReference[] = assertion.getAlgorithmSecretValue().split("[.]");
                if (privateKeyReference.length == 2) {
                    keyAlias = privateKeyReference[1];
                    try {
                        keyStoreGOID = Goid.parseGoid(privateKeyReference[0]);
                    } catch (IllegalArgumentException iae) {
                        try {
                            keyStoreId = Long.parseLong(privateKeyReference[0]);
                        } catch (NumberFormatException nfe) {
                            // Can't parse the OID/GOID.  Nothing we can do.
                            logAndAudit(AssertionMessages.ASSERTION_MISCONFIGURED, "(JWT Decode) Error while parsing the Public Key via Private Key reference");
                            return AssertionStatus.FAILED;
                        }
                    }
                } else {
                    // It's an invalid reference.
                    logAndAudit(AssertionMessages.ASSERTION_MISCONFIGURED, "(JWT Decode) Error while getting Public Key via Private Key - Invalid Reference");
                    return AssertionStatus.FAILED;
                }

                if (keyStoreGOID == null) {
                    // We have a long OID to convert to a GOID.
                    keyStoreGOID = GoidUpgradeMapper.mapOid(EntityType.SSG_KEY_ENTRY, keyStoreId);
                }

                SsgKeyEntry privateKey;
                try {
                    if (keyAlias == null && Goid.isDefault(keyStoreGOID)) {
                        // There is no key alias selected.  This means use the gateway default.
                        privateKey = defaultKey.getSslInfo();
                    } else {
                        privateKey = ssgKeyStoreManager.lookupKeyByKeyAlias(keyAlias, keyStoreGOID);
                    }
                } catch (Exception e) {
                    logAndAudit(AssertionMessages.ASSERTION_MISCONFIGURED, "(JWT Decode) An error occurred reading the private key data from the database (" + e.getMessage() + ")");
                    return AssertionStatus.FAILED;
                }

                // There should be a private key at this point.
                if (privateKey != null && privateKey.isPrivateKeyAvailableAndAccessible()) {
                    // We do indeed have a private key to get the public key from.
                    // We can validate the token.
                    try {
                        if (!jsonToken.validateReceivedToken(token, privateKey.getPublic())) {
                            // The token didn't validate
                            // If the validation fails, it will be already logged.
                            logAndAudit(AssertionMessages.ASSERTION_MISCONFIGURED, "(JWT Decode) An error has occurred during processing which caused the assertion to fail.  Token output can be found in the log.");
                            return AssertionStatus.FAILED;
                        }
                    } catch (IllegalJwtSignatureException e) {
                        logAndAudit(AssertionMessages.ASSERTION_MISCONFIGURED, "(JWT Decode) The received token specifies a signature type not supported by this assertion.  The token must be rejected.");
                        return AssertionStatus.FAILED;
                    }
                } else {
                    // We don't have a private key.
                    logAndAudit(AssertionMessages.ASSERTION_MISCONFIGURED, "(JWT Decode) An error occurred with the private key - it is not available.");
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
                            logAndAudit(AssertionMessages.ASSERTION_MISCONFIGURED, "(JWT Decode) WARNING: Attempting to locate password by migrating the old OID, which may fail.  The password should be reselected in this policy and saved.");
                        } catch (NumberFormatException nfe) {
                            logAndAudit(AssertionMessages.ASSERTION_MISCONFIGURED, "(JWT Decode) Unable to determine the identity of the password to use.");
                            return AssertionStatus.FAILED;
                        }
                    }
                    // Password was not found.
                    if (passwordEncrypted == null) {
                        logAndAudit(AssertionMessages.ASSERTION_MISCONFIGURED, "(JWT Decode) Error while looking up Password: Not Found");
                        return AssertionStatus.FAILED;
                    } else {
                        // else password was found so lets decrypt it
                        String pwSecret = new String(securePasswordManager.decryptPassword(passwordEncrypted.getEncodedPassword()));
                        if (pwSecret.isEmpty()) { // Password is never trimmed.
                            // The password is invalid
                            logAndAudit(AssertionMessages.ASSERTION_MISCONFIGURED, "(JWT Decode) Error retrieving password: Doesn't exist");
                            return AssertionStatus.FAILED;
                        }
                        if (!jsonToken.validateReceivedToken(token, pwSecret.getBytes(), false)) { // No URL Encoding on the secret.
                            // The token didn't validate
                            // If the validation fails, it will be already logged/audited at this point.
                            logAndAudit(AssertionMessages.ASSERTION_MISCONFIGURED, "(JWT Decode) An error has occurred during processing which caused the assertion to fail.  Token output can be found in the log.");
                            return AssertionStatus.FAILED;
                        }
                    }
                } catch (FindException e) {
                    logAndAudit(AssertionMessages.ASSERTION_MISCONFIGURED, "(JWT Decode) Error while looking up Password");
                    return AssertionStatus.FAILED;
                } catch (ParseException e) {
                    logAndAudit(AssertionMessages.ASSERTION_MISCONFIGURED, "(JWT Decode) Error while decrypting Password");
                    return AssertionStatus.FAILED;
                } catch (IllegalJwtSignatureException e) {
                    logAndAudit(AssertionMessages.ASSERTION_MISCONFIGURED, "(JWT Decode) The received token specifies a unsupported signature.  It cannot be used.");
                    return AssertionStatus.FAILED;
                }
                break;
            }
            case JwtUtilities.SELECTED_SECRET_VARIABLE:
            case JwtUtilities.SELECTED_SECRET_VARIABLE_BASE64: {
                // Expand the named variable
                Object theSecret;
                try {
                    String[] varSecret = Syntax.getReferencedNames(assertion.getAlgorithmSecretValue());

                    if (varSecret.length >= 1) {
                        theSecret = context.getVariable(varSecret[0]);
                    } else {
                        theSecret = context.getVariable(assertion.getAlgorithmSecretValue());
                    }

                } catch (NoSuchVariableException e) {
                    logAndAudit(AssertionMessages.ASSERTION_MISCONFIGURED, "(JWT Decode) Can't find the specified variable: " + assertion.getAlgorithmSecretValue());
                    return AssertionStatus.FAILED;
                }

                // The Secret is supported in two flavors: String containing password or PEM Certificate and SSG Certificate.
                if (theSecret instanceof String) {

                    String secret = (String)theSecret;

                    if (secret.isEmpty()) {   // Secret is never trimmed.
                        // No secret in the variable.
                        logAndAudit(AssertionMessages.ASSERTION_MISCONFIGURED, "(JWT Decode) Error retrieving secret: Variable is empty");
                        return AssertionStatus.FAILED;
                    }

                    if (secret.startsWith("-----BEGIN CERTIFICATE-----")) {
                        // This is a PEM certificate...?
                        try {
                            CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
                            // Consume the PEM certificate in the secret variable.
                            ByteArrayInputStream bais = new ByteArrayInputStream(secret.getBytes());
                            X509Certificate cert = (X509Certificate) certFactory.generateCertificate(bais);

                            try {
                                cert.checkValidity();
                                PublicKey pk = cert.getPublicKey();
                                if (pk == null) {
                                    logAndAudit(AssertionMessages.ASSERTION_MISCONFIGURED, "(JWT Decode) No public key was found with configured certificate");
                                    return AssertionStatus.FAILED;
                                }
                                try {
                                    if (!jsonToken.validateReceivedToken(token, pk)) {
                                        // The token didn't validate
                                        // If the validation fails, it will be already logged.
                                        logAndAudit(AssertionMessages.ASSERTION_MISCONFIGURED, "(JWT Decode) An error has occurred during processing which caused the assertion to fail.  Token output can be found in the log.");
                                        return AssertionStatus.FAILED;
                                    }
                                } catch (IllegalJwtSignatureException e) {
                                    logAndAudit(AssertionMessages.ASSERTION_MISCONFIGURED, "(JWT Decode) The received token specifies a signature type not supported by the assertion.  It cannot be used.");
                                    return AssertionStatus.FAILED;
                                }

                            } catch (CertificateExpiredException e) {
                                logAndAudit(AssertionMessages.ASSERTION_MISCONFIGURED, "(JWT Decode) The certificate '"+cert.getSubjectDN().getName()+"' has expired and cannot be used to validate token signatures");
                                return AssertionStatus.FAILED;
                            } catch (CertificateNotYetValidException e) {
                                logAndAudit(AssertionMessages.ASSERTION_MISCONFIGURED, "(JWT Decode) The certificate '" + cert.getSubjectDN().getName() + "' is not yet valid and and cannot be used to validate token signatures");
                                return AssertionStatus.FAILED;
                            }

                        } catch (CertificateException e) {
                            logAndAudit(AssertionMessages.ASSERTION_MISCONFIGURED, "(JWT Decode) PEM Certificate provided in variable '"+assertion.getAlgorithmSecretValue()+"' is invalid and can't be used.");
                            return AssertionStatus.FAILED;  // The token can't be decoded if this occurs.
                        }
                    }

                    if ((assertion.getAlgorithmSecretLocation() & JwtUtilities.SELECTED_SECRET_VARIABLE_BASE64) == JwtUtilities.SELECTED_SECRET_VARIABLE_BASE64) {
                        try {
                            if (!jsonToken.validateReceivedToken(token, secret.getBytes(), true)) { // URL Encoded Secret
                                // The token didn't validate
                                // If the validation fails, it will be already logged or audited by this point..
                                logAndAudit(AssertionMessages.ASSERTION_MISCONFIGURED, "(JWT Decode) An error has occurred during processing which caused the assertion to fail.  Token output can be found in the log.");
                                return AssertionStatus.FAILED;
                            }
                        } catch (IllegalJwtSignatureException e) {
                            logAndAudit(AssertionMessages.ASSERTION_MISCONFIGURED, "(JWT Decode) The received token specifies a unsupported signature.  It cannot be used.");
                            return AssertionStatus.FAILED;
                        }
                    } else {
                        try {
                            if (!jsonToken.validateReceivedToken(token, secret.getBytes(), false)) { // No URL Encoded Secret
                                // The token didn't validate
                                // If the validation fails, it will be already logged or audited.
                                logAndAudit(AssertionMessages.ASSERTION_MISCONFIGURED, "(JWT Decode) An error has occurred during processing which caused the assertion to fail.  Token output can be found in the log.");
                                return AssertionStatus.FAILED;
                            }
                        } catch (IllegalJwtSignatureException e) {
                            logAndAudit(AssertionMessages.ASSERTION_MISCONFIGURED, "(JWT Decode) The received token specifies a signature not supported by this assertion.  It cannot be used.");
                            return AssertionStatus.FAILED;
                        }
                    }

                } else if (theSecret instanceof X509Certificate) {  // In reality, it's sun.security.x509.X509CertImpl, but it's a sun class and probably deprecated soon.

                    X509Certificate secret = (X509Certificate) theSecret;
                    try {
                        secret.checkValidity();
                        PublicKey pk = secret.getPublicKey();
                        if (pk == null) {
                            logAndAudit(AssertionMessages.ASSERTION_MISCONFIGURED, "(JWT Decode) No public key was found with this certificate");
                            return AssertionStatus.FAILED;
                        }
                        try {
                            if (!jsonToken.validateReceivedToken(token, pk)) {
                                // The token didn't validate
                                // If the validation fails, it will be already logged or audited by this point.
                                logAndAudit(AssertionMessages.ASSERTION_MISCONFIGURED, "(JWT Decode) An error has occurred during processing which caused the assertion to fail.  Token output can be found in the log.");
                                return AssertionStatus.FAILED;
                            }
                        } catch (IllegalJwtSignatureException e) {
                            logAndAudit(AssertionMessages.ASSERTION_MISCONFIGURED, "(JWT Decode) The received token specifies a unsupported signature.  It cannot be used.");
                            return AssertionStatus.FAILED;
                        }

                    } catch (CertificateExpiredException e) {
                        logAndAudit(AssertionMessages.ASSERTION_MISCONFIGURED, "(JWT Decode) The certificate '"+secret.getSubjectDN().getName()+"' has expired and cannot be used to validate token signatures");
                        return AssertionStatus.FAILED;
                    } catch (CertificateNotYetValidException e) {
                        logAndAudit(AssertionMessages.ASSERTION_MISCONFIGURED, "(JWT Decode) The certificate '" + secret.getSubjectDN().getName() + "' is not yet valid and and cannot be used to validate token signatures");
                        return AssertionStatus.FAILED;
                    }

                } else {
                    logAndAudit(AssertionMessages.ASSERTION_MISCONFIGURED, "(JWT Decode) The secret contained in variable '"+assertion.getAlgorithmSecretValue()+"' is an unsupported type and cannot be processed.");
                    return AssertionStatus.FAILED;
                }

                break;
            }
            default: { // Includes SELECTED_SECRET_NONE
                // No secret is set.  This can be the case if the NONE signature is used.
                try {
                    if (!jsonToken.validateReceivedTokenNoSecret(token, tcm)) {
                        // The token didn't validate
                        // If the validation fails, it will be already logged.
                        logAndAudit(AssertionMessages.ASSERTION_MISCONFIGURED, "(JWT Decode) An error has occurred during processing which caused the assertion to fail.  Token output can be found in the log.");
                        return AssertionStatus.FAILED;
                    }
                } catch (IllegalJwtSignatureException e) {
                    logAndAudit(AssertionMessages.ASSERTION_MISCONFIGURED, "(JWT Decode) The received token specifies a unsupported signature.  It cannot be used.");
                    return AssertionStatus.FAILED;
                }
            }
        }

        // The token validated, get the payload
        context.setVariable(outputVariable, jsonToken.getPayload());
        context.setVariable(outputVariable + ".header", jsonToken.getHeader());
        // Reserved for implementing <variable>.claims.<claim> support here...
        return AssertionStatus.NONE;

    }
}

