package com.l7tech.external.assertions.radius.server;

import com.l7tech.external.assertions.radius.RadiusAssertion;
import com.l7tech.external.assertions.radius.RadiusAuthenticateAssertion;
import com.l7tech.external.assertions.radius.RadiusUtils;
import com.l7tech.gateway.common.audit.Messages;
import com.l7tech.gateway.common.security.password.SecurePassword;
import com.l7tech.message.Message;
import com.l7tech.objectmodel.FindException;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.credential.LoginCredentials;
import com.l7tech.policy.variable.NoSuchVariableException;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.AbstractServerAssertion;
import com.l7tech.server.policy.assertion.AssertionStatusException;
import com.l7tech.server.policy.variable.ExpandVariables;
import com.l7tech.server.policy.variable.RadiusAuthenticationContext;
import com.l7tech.server.security.password.SecurePasswordManager;
import com.l7tech.util.HexUtils;
import com.l7tech.util.Pair;
import net.jradius.client.RadiusClient;
import net.jradius.client.auth.RadiusAuthenticator;
import net.jradius.dictionary.Attr_UserName;
import net.jradius.dictionary.Attr_UserPassword;
import net.jradius.exception.RadiusException;
import net.jradius.exception.TimeoutException;
import net.jradius.packet.AccessAccept;
import net.jradius.packet.AccessRequest;
import net.jradius.packet.RadiusPacket;
import net.jradius.packet.attribute.AttributeList;
import net.jradius.packet.attribute.RadiusAttribute;
import net.jradius.packet.attribute.value.AttributeValue;
import net.jradius.packet.attribute.value.DateValue;
import net.jradius.packet.attribute.value.IntegerValue;
import net.jradius.packet.attribute.value.StringValue;

import javax.inject.Inject;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.ParseException;
import java.util.Date;
import java.util.Map;

/**
 * Server side implementation of the RadiusAuthenticateAssertion.
 *
 * @see com.l7tech.external.assertions.radius.RadiusAuthenticateAssertion
 */
public class ServerRadiusAuthenticateAssertion extends AbstractServerAssertion<RadiusAuthenticateAssertion> {

    private static final int SUCCESS = 0;
    private static final int RADIUS_SERVER_ERROR = -1;
    private static final int RADIUS_SERVER_TIMEOUT = -2;
    private static final int UNKNOWN_HOST = -3;
    private static final int SECRET_NOT_FOUND = -4;
    private static final int CONFIGURATION_ERROR = -5;
    public static final int DEFAULT_RETRIES = 0;
    private static final int CHALLENGE_OCCURRED = -6;

    private final String reasonCode;
    private final String[] variablesUsed;
    private boolean accessChallengeOccurred;

    @Inject
    SecurePasswordManager securePasswordManager;

    public ServerRadiusAuthenticateAssertion(final RadiusAuthenticateAssertion assertion) throws PolicyAssertionException {
        super(assertion);
        this.variablesUsed = assertion.getVariablesUsed();
        this.reasonCode = assertion.getPrefix() + "." + RadiusAssertion.REASON_CODE;
    }

    public AssertionStatus checkRequest(final PolicyEnforcementContext context) throws IOException, PolicyAssertionException {
        final Map<String, Object> variableMap = context.getVariableMap(variablesUsed, getAudit());

        LoginCredentials credentials = getLoginCredentials(context);

        if (credentials == null) {
            logAndAudit(Messages.EXCEPTION_INFO);
            return AssertionStatus.AUTH_REQUIRED;
        }

        try {
            if (authenticate(context, credentials, variableMap)) {
                context.setVariable(reasonCode, SUCCESS);
                return AssertionStatus.NONE;
            } else {
                if (accessChallengeOccurred) {
                    context.setVariable(reasonCode, CHALLENGE_OCCURRED);
                }
                logAndAudit(Messages.EXCEPTION_INFO_WITH_MORE_INFO, credentials.getLogin());
                return AssertionStatus.AUTH_FAILED;
            }
        } catch (UnknownHostException e) {
            context.setVariable(reasonCode, UNKNOWN_HOST);
            logAndAudit(Messages.EXCEPTION_INFO_WITH_MORE_INFO, "Unknown host " + e.getMessage());
        } catch (TimeoutException e) {
            context.setVariable(reasonCode, RADIUS_SERVER_TIMEOUT);
            logAndAudit(Messages.EXCEPTION_INFO_WITH_MORE_INFO, e.getMessage());
        } catch (RadiusException e) {
            context.setVariable(reasonCode, RADIUS_SERVER_ERROR);
            logAndAudit(Messages.EXCEPTION_INFO_WITH_MORE_INFO, e.getMessage());
        } catch (FindException e) {
            context.setVariable(reasonCode, SECRET_NOT_FOUND);
            logAndAudit(Messages.EXCEPTION_INFO_WITH_MORE_INFO, e.getMessage());
        } catch (ParseException e) {
            context.setVariable(reasonCode, CONFIGURATION_ERROR);
            logAndAudit(Messages.EXCEPTION_INFO_WITH_MORE_INFO, e.getMessage());
        }

        return AssertionStatus.AUTH_FAILED;

    }

    /**
     * Retrieve the LoginCredential from the context. If no credential is found, return null.
     *
     * @param context The PolicyEnforementContext
     * @return The LoginCredential or null if not found from the context.
     */
    private LoginCredentials getLoginCredentials(final PolicyEnforcementContext context) throws AssertionStatusException {
        try {
            Message targetMessage = context.getOrCreateTargetMessage(assertion, false);
            return context.getAuthenticationContext(targetMessage).getLastCredentials();
        } catch (NoSuchVariableException e) {
            throw new AssertionStatusException(AssertionStatus.SERVER_ERROR, e.getMessage(), e);
        }
    }

    /**
     * Authenticate against Radius server
     *
     * @param credentials The Login Credential.
     * @return true when success authenticate, false when authenticate failed.
     * @throws java.io.IOException
     * @throws net.jradius.exception.RadiusException
     */
    private boolean authenticate(PolicyEnforcementContext ctx, LoginCredentials credentials, Map<String, Object> variableMap) throws IOException, RadiusException, FindException, ParseException {

        String host = ExpandVariables.process(assertion.getHost(), variableMap, getAudit());
        int authPort = RadiusUtils.parseIntValue(ExpandVariables.process(assertion.getAuthPort(), variableMap, getAudit()));
        int acctPort = RadiusUtils.parseIntValue(ExpandVariables.process(assertion.getAcctPort(), variableMap, getAudit()));
        int timeout = RadiusUtils.parseIntValue(ExpandVariables.process(assertion.getTimeout(), variableMap, getAudit()));

        InetAddress inetAddress = InetAddress.getByName(host);
        RadiusClient radiusClient = null;
        String s = getSharedSecret();

        try {
            radiusClient = getRadiusClient(authPort, acctPort, timeout, inetAddress, s);
            AttributeList attributeList = new AttributeList();
            for (Map.Entry<String, String> entry : assertion.getAttributes().entrySet()) {
                if (entry.getKey().equals("State")) {
                    byte[] value = ExpandVariables.process(entry.getValue(), variableMap, getAudit()).getBytes();
                    attributeList.add(RadiusUtils.newAttribute(entry.getKey(), value));

                } else {
                    String value = ExpandVariables.process(entry.getValue(), variableMap, getAudit());
                    attributeList.add(RadiusUtils.newAttribute(entry.getKey(), value));
                }

            }

            //Setting the password
            attributeList.add(new Attr_UserName(credentials.getLogin()));
            attributeList.add(new Attr_UserPassword(new String(credentials.getCredentials())));

            AccessRequest request = new AccessRequest(radiusClient, attributeList);

            RadiusPacket replyPacket = null;
            RadiusAuthenticator authenticator = RadiusClient.getAuthProtocol(assertion.getAuthenticator());
            if (authenticator.getAuthName().equals("pap")) {
                // We only want to inject our Authenticator if the Gateway is configured to use 'pap'
                authenticator = new PAPAuthenticationWithAC();
            }
            accessChallengeOccurred = false;
            AttributeList challengeAttributes = null;
            try {
                replyPacket = radiusClient.authenticate(request, authenticator, DEFAULT_RETRIES);
            } catch (RadiusExceptionWithAttributes e) {
                accessChallengeOccurred = true;
                challengeAttributes = e.getListOfAttributes();

            }

            //store RadiusAuthenticationContext in a context variable
            RadiusAuthenticationContext radiusAuthCtx = createRadiusAuthenticationContext(replyPacket);
            if (null != radiusAuthCtx) {
                for (Pair<String, Object> attribute : radiusAuthCtx.getRadiusAttributes()) {
                    ctx.setVariable(assertion.getPrefix() + "." + attribute.getKey(), attribute.getValue());
                }
            }
            // store the challenge attirbutes as context variables
            if (accessChallengeOccurred) {
                if (null != challengeAttributes && null != challengeAttributes.getAttributeList()) {
                    for (RadiusAttribute challengeAttribute : challengeAttributes.getAttributeList()) {
                        ctx.setVariable(assertion.getPrefix() + "." + challengeAttribute.getAttributeName(), extractAttributeValue(challengeAttribute));
                    }
                }

            }

            return (replyPacket instanceof AccessAccept);

        } finally {
            if (radiusClient != null) {
                radiusClient.close();
            }
        }

    }

    RadiusClient getRadiusClient(int authPort, int acctPort, int timeout, InetAddress inetAddress, String secret) throws IOException {
        RadiusClient radiusClient;
        radiusClient = new RadiusClient(inetAddress, secret, authPort, acctPort, timeout);
        return radiusClient;
    }

    String getSharedSecret() throws FindException, ParseException {
        SecurePassword secret = securePasswordManager.findByPrimaryKey(assertion.getSecretGoid());
        if (secret == null) throw new FindException();
        return new String(securePasswordManager.decryptPassword(secret.getEncodedPassword()));
    }

    /**
     * creates RadiusAuthentication context out of reply packet
     *
     * @param replyPacket RadiusPacket
     * @return RadiusAuthenticationContext
     */
    private RadiusAuthenticationContext createRadiusAuthenticationContext(RadiusPacket replyPacket) {
        if (null == replyPacket) {
            return null;
        }
        AttributeList replyPacketAttributes = replyPacket.getAttributes();
        RadiusAuthenticationContext radiusAuthenticationContext = new RadiusAuthenticationContext();
        for (RadiusAttribute replyAttribute : replyPacketAttributes.getAttributeList()) {
            radiusAuthenticationContext.addRadiusAttribute(replyAttribute.getAttributeName(), extractAttributeValue(replyAttribute));
        }
        return radiusAuthenticationContext;
    }

    /**
     *  Helper method to pull out the attribute value based on its type. If it is not
     *  a date or Integer or String, is is base64 encoded
     * @param attribute
     * @return
     */
    private Object extractAttributeValue(RadiusAttribute attribute) {
        Object value;
        AttributeValue valueObject = attribute.getValue();
        if (valueObject instanceof DateValue) {
            value = new Date(((DateValue) valueObject).getValue());
        } else if (valueObject instanceof IntegerValue) {
            value = ((IntegerValue) valueObject).getValue().intValue();
        } else if (valueObject instanceof StringValue) {
            value = new String(valueObject.getBytes());
        } else {
            value = HexUtils.encodeBase64(valueObject.getBytes(), true);
        }
        return value;
    }

}
