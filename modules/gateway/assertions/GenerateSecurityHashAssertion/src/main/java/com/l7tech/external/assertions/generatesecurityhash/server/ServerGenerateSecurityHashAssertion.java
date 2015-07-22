package com.l7tech.external.assertions.generatesecurityhash.server;

import com.l7tech.common.mime.NoSuchPartException;
import com.l7tech.common.mime.PartInfo;
import com.l7tech.external.assertions.generatesecurityhash.GenerateSecurityHashAssertion;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.message.Message;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.variable.Syntax;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.AbstractServerAssertion;
import com.l7tech.server.policy.assertion.AssertionStatusException;
import com.l7tech.server.policy.variable.ExpandVariables;
import com.l7tech.server.util.ContextVariableUtils;
import com.l7tech.util.*;
import org.jetbrains.annotations.NotNull;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.Map;
import java.util.logging.Level;

/**
 * Server side implementation of the {@link com.l7tech.external.assertions.generatesecurityhash.GenerateSecurityHashAssertion}.
 *
 */
public class ServerGenerateSecurityHashAssertion extends AbstractServerAssertion<GenerateSecurityHashAssertion> {
    private final String[] varsUsed;
    private final boolean singleVarExpr;
    private final String expr;
    private final boolean singleVarKeyExpr;
    private final String keyExpr;

    /**
     * Constructor a new ServerGenerateSecurityHashAssertion with the given GenerateHashAssertion bean.
     *
     * @param assertion the GenerateHashAssertion bean.
     */
    public ServerGenerateSecurityHashAssertion(final GenerateSecurityHashAssertion assertion) {
        super(assertion);
        this.varsUsed = assertion.getVariablesUsed();
        this.expr = assertion.dataToSignText();
        this.singleVarExpr = expr != null && Syntax.isOnlyASingleVariableReferenced( expr );
        this.keyExpr = assertion.getKeyText();
        this.singleVarKeyExpr = keyExpr != null && Syntax.isOnlyASingleVariableReferenced( keyExpr );
    }

    @Override
    public AssertionStatus checkRequest(final PolicyEnforcementContext context) {
        final Map<String, Object> vars = Collections.unmodifiableMap( context.getVariableMap( varsUsed, getAudit() ) );

        final Object dataToSignObj;
        if ( singleVarExpr ) {
            dataToSignObj = ExpandVariables.processSingleVariableAsObject( expr, vars, getAudit(), true );
        } else {
            if ( expr == null || ExpandVariables.isVariableReferencedNotFound(assertion.dataToSignText(), vars, getAudit() ) ) {
                logAndAudit(AssertionMessages.GENERATE_HASH_VARIABLE_NOT_SET, "Data to Sign");
                return AssertionStatus.FAILED;
            }
            dataToSignObj = ExpandVariables.process( expr, vars, getAudit(), true );
        }

        final byte[] dataToSign = toByteArray( dataToSignObj, "<Source Object>" );
        final String algorithm = GenerateSecurityHashAssertion.getSupportedAlgorithm().get(assertion.getAlgorithm());
        final String variableName = assertion.getTargetOutputVariable();

        if (dataToSign == null) {
            logAndAudit(AssertionMessages.GENERATE_HASH_VARIABLE_NOT_SET, "Data to Sign");
            return AssertionStatus.FAILED;
        }
        if (algorithm == null || algorithm.trim().isEmpty()) {
            logAndAudit(AssertionMessages.GENERATE_HASH_VARIABLE_NOT_SET, "Algorithm");
            return AssertionStatus.FAILED;
        }
        if (variableName == null || variableName.trim().isEmpty()) {
            logAndAudit(AssertionMessages.GENERATE_HASH_VARIABLE_NOT_SET, "Output Variable");
            return   AssertionStatus.FAILED;
        }
        return signData( context, algorithm, dataToSign, variableName, vars );
    }

    private byte[] toByteArray( Object obj, String what ) {
        try {
            // Preserve old behavior of using UTF-8 for string values
            return ContextVariableUtils.convertContextVariableValueToByteArray( obj, -1, Charsets.UTF8 );

        } catch ( NoSuchPartException e ) {
            getAudit().logAndAudit( AssertionMessages.NO_SUCH_PART, new String[] { what, e.getWhatWasMissing() },
                    ExceptionUtils.getDebugException( e ) );
            throw new AssertionStatusException( AssertionStatus.SERVER_ERROR, "Unable to read " + what + ":" + ExceptionUtils.getMessage( e ), e );
        } catch ( IOException | ContextVariableUtils.NoBinaryRepresentationException e ) {
            getAudit().logAndAudit( AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO,
                    new String[] { "Unable to read source object: " + ExceptionUtils.getMessage( e ) },
                    ExceptionUtils.getDebugException( e ) );
            throw new AssertionStatusException( AssertionStatus.SERVER_ERROR, "Unable to read " + what + ":" + ExceptionUtils.getMessage( e ), e );
        }
    }

    /**
     * Sign a piece of non-xml data and storing the result into a context variable.
     *
     * @param context              the PolicyEnforcementContext to store the context variable to.
     * @param algorithm            the signing algorithm.
     * @param dataToSign           the non-xml data to sign.
     * @param targetOutputVariable the target output variable name.
     * @param vars                 the context variable values.
     * @return AssertionStatus.NONE if no errors were encountered while signing and storing the data.  AssertionStatus.FAILED if
     *         one or more error(s) encountered.
     */
    private AssertionStatus signData(@NotNull final PolicyEnforcementContext context, @NotNull final String algorithm,
                                     @NotNull final byte[] dataToSign, @NotNull final String targetOutputVariable,
                                     Map<String, Object> vars)
    {
        AssertionStatus returnStatus = AssertionStatus.NONE;
        try {
            String output = "";
            if (GenerateSecurityHashAssertion.HMAC_ALGORITHM.matcher(algorithm).matches()) {

                final byte[] key = findHashKey( vars );
                if ( key.length < 1 ) {
                    logAndAudit(AssertionMessages.GENERATE_HASH_VARIABLE_NOT_SET, "Key");
                    returnStatus = AssertionStatus.FAILED;
                }
                output = generateHash( dataToSign, key, algorithm );
            } else {
                output = generateDigest(dataToSign, algorithm);
            }
            context.setVariable(targetOutputVariable, output);
        } catch (Exception e) {
            logAndAudit( AssertionMessages.GENERATE_HASH_ERROR, ExceptionUtils.getDebugException( e ) );
            returnStatus = AssertionStatus.FAILED;
        }
        return returnStatus;
    }

    private byte[] findHashKey( Map<String, Object> vars ) {
        final Object keyObj;
        if ( singleVarKeyExpr ) {
            keyObj = ExpandVariables.processSingleVariableAsObject( keyExpr, vars, getAudit(), true );
        } else {
            if ( keyExpr == null || ExpandVariables.isVariableReferencedNotFound( keyExpr, vars, getAudit() ) ) {
                logAndAudit(AssertionMessages.GENERATE_HASH_VARIABLE_NOT_SET, "Key");
                throw new AssertionStatusException( "Key variable not set" );
            }
            keyObj = ExpandVariables.process( keyExpr, vars, getAudit(), true );
        }

        return toByteArray( keyObj, "<Key>" );
    }

    /**
     * This method takes in data to modify and other various pieces of information and applies the HMAC algorithm to it.
     * It produces a Base64 Encoded String.
     * <p/>
     * The algorithms have to be valid algorithm names for the "javax.crypto.Mac" class.
     * This list can be found at the following link:
     * <a href="http://download.oracle.com/javase/6/docs/technotes/guides/security/StandardNames.html#Mac">
     * http://download.oracle.com/javase/6/docs/technotes/guides/security/StandardNames.html#Mac
     * </a>
     *
     * @param dataToSign input of the data that will be signed.
     * @param key        The key to be used to sign
     * @param algorithm  The algorithm to be used.
     * @return Base 64 encoded String which represents the results of applying HMAC with algorithm and the key specified to the dataToSign.     *
     * @throws NoSuchAlgorithmException if the given algorithm does not exist or is not supported.
     * @throws InvalidKeyException      if the given key is invalid.
     */
    private String generateHash(@NotNull final byte[] dataToSign, @NotNull final byte[] key, @NotNull final String algorithm) throws NoSuchAlgorithmException, InvalidKeyException {
        String toReturn;
        try {
            final Mac messageAuthenticationCode = Mac.getInstance(algorithm);
            messageAuthenticationCode.init( new SecretKeySpec( key, algorithm ) );
            final byte[] digest = messageAuthenticationCode.doFinal( dataToSign );
            logger.log(Level.FINE, "Hash Value: " + HexUtils.encodeBase64(digest, true));
            toReturn = HexUtils.encodeBase64(digest, true);
        } catch (NoSuchAlgorithmException nsaE) {
            logAndAudit(AssertionMessages.GENERATE_HASH_UNSUPPORTED_ALGORITHM, algorithm);
            throw nsaE;
        } catch (InvalidKeyException ikE) {
            logAndAudit( AssertionMessages.GENERATE_HASH_INVALID_KEY, ExceptionUtils.getMessage( ikE ) );
            throw ikE;
        }
        return toReturn;
    }

    /**
     * This method takes in data to modify and other various pieces of information and applies the Digest algorithm to it.
     * It produces a Base64 Encoded String.
     * <p/>
     * The algorithms have to be valid algorithm names for the "java.security.MessageDigest" class.
     * This list can be found at the following link:
     * <a href="http://download.oracle.com/javase/6/docs/technotes/guides/security/StandardNames.html#MessageDigest">
     * http://download.oracle.com/javase/6/docs/technotes/guides/security/StandardNames.html#MessageDigestc
     * </a>
     *
     * @param dataToSign input of the data that will be signed.
     * @param algorithm  The algorithm to be used.
     * @return Base 64 encoded String which represents the results of applying Message Digest algorithm and the to the dataToSign.
     * @throws NoSuchAlgorithmException if the given algorithm does not exist or is not supported.
     */
    private String generateDigest(@NotNull final byte[] dataToSign, @NotNull final String algorithm) throws NoSuchAlgorithmException {
        try {
            final MessageDigest md = MessageDigest.getInstance(algorithm);
            md.update( dataToSign );
            final byte[] digest = md.digest();
            logger.log(Level.FINE, "Hash Value: " + HexUtils.encodeBase64(digest, true));
            return HexUtils.encodeBase64(digest, true);
        } catch (NoSuchAlgorithmException e) {
            logAndAudit(AssertionMessages.GENERATE_HASH_UNSUPPORTED_ALGORITHM, algorithm);
            throw e;
        }
    }

}
