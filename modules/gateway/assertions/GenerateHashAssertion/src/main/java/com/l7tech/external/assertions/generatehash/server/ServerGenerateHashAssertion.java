package com.l7tech.external.assertions.generatehash.server;

import com.l7tech.external.assertions.generatehash.GenerateHashAssertion;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.AbstractServerAssertion;
import com.l7tech.server.policy.variable.ExpandVariables;
import com.l7tech.util.Charsets;
import com.l7tech.util.HexUtils;
import org.jetbrains.annotations.NotNull;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.Map;
import java.util.logging.Level;

/**
 * Server side implementation of the {@link GenerateHashAssertion}.
 *
 */
public class ServerGenerateHashAssertion extends AbstractServerAssertion<GenerateHashAssertion> {

    /**
     * Constructor a new ServerGenerateHashAssertion with the given GenerateHashAssertion bean.
     *
     * @param assertion the GenerateHashAssertion bean.
     */
    public ServerGenerateHashAssertion(final GenerateHashAssertion assertion) {
        super(assertion);
    }

    @Override
    public AssertionStatus checkRequest(final PolicyEnforcementContext context) {
        AssertionStatus returnStatus = AssertionStatus.NONE;

        final Map<String, Object> vars = Collections.unmodifiableMap(context.getVariableMap(assertion.getVariablesUsed(), getAudit()));
        if(ExpandVariables.isVariableReferencedNotFound(assertion.getDataToSignText(), vars, getAudit())){
            logAndAudit(AssertionMessages.GENERATE_HASH_VARIABLE_NOT_SET, "Data to Sign");
            returnStatus = AssertionStatus.FAILED;
        }
        else {
            final String dataToSign = ExpandVariables.process(assertion.getDataToSignText(), vars, getAudit(), true);

            final String algorithm = assertion.getAlgorithm();
            final String variableName = assertion.getTargetOutputVariable();

            if (dataToSign == null) {
                logAndAudit(AssertionMessages.GENERATE_HASH_VARIABLE_NOT_SET, "Data to Sign");
                returnStatus = AssertionStatus.FAILED;
            }
            if (algorithm == null || algorithm.trim().isEmpty()) {
                logAndAudit(AssertionMessages.GENERATE_HASH_VARIABLE_NOT_SET, "Algorithm");
                returnStatus = AssertionStatus.FAILED;
            }
            if (variableName == null || variableName.trim().isEmpty()) {
                logAndAudit(AssertionMessages.GENERATE_HASH_VARIABLE_NOT_SET, "Output Variable");
                returnStatus = AssertionStatus.FAILED;
            }
            if (returnStatus == AssertionStatus.NONE) {
                returnStatus = signData(context, algorithm, dataToSign, variableName);
            }
        }
        return returnStatus;
    }

    /**
     * Sign a piece of non-xml data and storing the result into a context variable.
     *
     * @param context              the PolicyEnforcementContext to store the context variable to.
     * @param algorithm            the signing algorithm.
     * @param dataToSign           the non-xml data to sign.
     * @param targetOutputVariable the target output variable name.
     * @return AssertionStatus.NONE if no errors were encountered while signing and storing the data.  AssertionStatus.FAILED if
     *         one or more error(s) encountered.
     */
    private AssertionStatus signData(@NotNull final PolicyEnforcementContext context, @NotNull final String algorithm,
                                     @NotNull final String dataToSign, @NotNull final String targetOutputVariable) {
        AssertionStatus returnStatus = AssertionStatus.NONE;
        try {
            String output = "";
            if (GenerateHashAssertion.HMAC_ALGORITHM.matcher(algorithm).matches()) {
                // in this case we have to check to make sure a key exists as well
                final Map<String, Object> vars = Collections.unmodifiableMap(context.getVariableMap(assertion.getVariablesUsed(), getAudit()));
                if(ExpandVariables.isVariableReferencedNotFound(assertion.getKeyText(), vars, getAudit())){
                    logAndAudit(AssertionMessages.GENERATE_HASH_VARIABLE_NOT_SET, "Key");
                    returnStatus = AssertionStatus.FAILED;
                }
                else {
                    final String key = ExpandVariables.process(assertion.getKeyText(), vars, getAudit(), true);
                    if (key == null || key.isEmpty()) {
                        logAndAudit(AssertionMessages.GENERATE_HASH_VARIABLE_NOT_SET, "Key");
                        returnStatus = AssertionStatus.FAILED;
                    } else {
                        output = generateHash(dataToSign, key, algorithm);
                    }
                }
            } else {
                output = generateDigest(dataToSign, algorithm);
            }
            context.setVariable(targetOutputVariable, output);
        } catch (Exception e) {
            logAndAudit(AssertionMessages.GENERATE_HASH_ERROR);
            returnStatus = AssertionStatus.FAILED;
        }
        return returnStatus;
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
     * @param dataToSign String input of the data that will be signed.
     * @param key        The key to be used to sign
     * @param algorithm  The algorithm to be used.
     * @return Base 64 encoded String which represents the results of applying HMAC with algorithm and the key specified to the dataToSign.     *
     * @throws NoSuchAlgorithmException if the given algorithm does not exist or is not supported.
     * @throws InvalidKeyException      if the given key is invalid.
     */
    private String generateHash(@NotNull final String dataToSign, @NotNull final String key, @NotNull final String algorithm) throws NoSuchAlgorithmException, InvalidKeyException {
        String toReturn;
        try {
            final Mac messageAuthenticationCode = Mac.getInstance(algorithm);
            messageAuthenticationCode.init(new SecretKeySpec(key.getBytes(Charsets.UTF8), algorithm));
            final byte[] digest = messageAuthenticationCode.doFinal(dataToSign.getBytes(Charsets.UTF8));
            logger.log(Level.FINE, "Hash Value: " + HexUtils.encodeBase64(digest, true));
            toReturn = HexUtils.encodeBase64(digest, true);
        } catch (NoSuchAlgorithmException nsaE) {
            logAndAudit(AssertionMessages.GENERATE_HASH_UNSUPPORTED_ALGORITHM, algorithm);
            throw nsaE;
        } catch (InvalidKeyException ikE) {
            logAndAudit(AssertionMessages.GENERATE_HASH_INVALID_KEY, key);
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
     * @param dataToSign String input of the data that will be signed.
     * @param algorithm  The algorithm to be used.
     * @return Base 64 encoded String which represents the results of applying Message Digest algorithm and the to the dataToSign.
     * @throws NoSuchAlgorithmException if the given algorithm does not exist or is not supported.
     */
    private String generateDigest(@NotNull final String dataToSign, @NotNull final String algorithm) throws NoSuchAlgorithmException {
        try {
            final MessageDigest md = MessageDigest.getInstance(algorithm);
            md.update(dataToSign.getBytes(Charsets.UTF8));
            final byte[] digest = md.digest();
            logger.log(Level.FINE, "Hash Value: " + HexUtils.encodeBase64(digest, true));
            return HexUtils.encodeBase64(digest, true);
        } catch (NoSuchAlgorithmException e) {
            logAndAudit(AssertionMessages.GENERATE_HASH_UNSUPPORTED_ALGORITHM, algorithm);
            throw e;
        }
    }

}
