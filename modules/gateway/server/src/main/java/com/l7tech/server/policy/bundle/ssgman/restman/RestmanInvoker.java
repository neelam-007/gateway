package com.l7tech.server.policy.bundle.ssgman.restman;

import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.common.mime.NoSuchPartException;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.identity.UserBean;
import com.l7tech.message.AbstractHttpResponseKnob;
import com.l7tech.message.HttpRequestKnob;
import com.l7tech.message.HttpRequestKnobAdapter;
import com.l7tech.message.Message;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.security.token.http.HttpBasicToken;
import com.l7tech.server.identity.AuthenticationResult;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import com.l7tech.server.policy.bundle.GatewayManagementDocumentUtilities;
import com.l7tech.server.policy.bundle.ssgman.BaseGatewayManagementInvoker;
import com.l7tech.server.policy.bundle.ssgman.GatewayManagementInvoker;
import com.l7tech.util.Charsets;
import com.l7tech.util.Functions;
import com.l7tech.util.Pair;
import org.apache.http.HttpHeaders;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.util.logging.Level;

import static com.l7tech.server.policy.bundle.GatewayManagementDocumentUtilities.UnexpectedManagementResponse;
import static com.l7tech.server.policy.bundle.ssgman.restman.RestmanMessage.MAPPING_ACTION_ATTRIBUTE;

/**
 * Common code useful for all restman policy bundle installers (and exporter).
 */
public class RestmanInvoker extends BaseGatewayManagementInvoker {
    private static final String RESTMAN_VAR_PREFIX = "RestGatewayMan";
    public static final String VAR_RESTMAN_ACTION = RESTMAN_VAR_PREFIX + "." + MAPPING_ACTION_ATTRIBUTE;
    private static final String VAR_RESTMAN_BASE_URI = RESTMAN_VAR_PREFIX + ".baseuri";
    public static final String VAR_RESTMAN_URI = RESTMAN_VAR_PREFIX + ".uri";
    public static final String VAR_RESTMAN_CONTENT_TYPE = RESTMAN_VAR_PREFIX + ".contentType";
    public static final String URL_1_0_BUNDLE = "1.0/bundle";
    private static final String URL_LOCALHOST_8080_RESTMAN = "http://localhost:8080/restman/";
    public static final String UTF_8 = "UTF-8";

    public RestmanInvoker(@NotNull final Functions.Nullary<Boolean> cancelledCallback,
                          @NotNull final GatewayManagementInvoker gatewayManagementInvoker) {
        super(cancelledCallback, gatewayManagementInvoker);
    }

    /**
     * Call the gateway management assertion.
     *
     * @param requestXml request XML to sent to server assertion
     * @return Pair of assertion status and response document
     * @throws com.l7tech.server.policy.bundle.GatewayManagementDocumentUtilities.UnexpectedManagementResponse if the response from the management assertion
     * is an Internal Error.
     */
    @NotNull
    private Pair<AssertionStatus, RestmanMessage> callManagementAssertion(
            final String requestXml) throws GatewayManagementDocumentUtilities.AccessDeniedManagementResponse, UnexpectedManagementResponse {
        return callManagementAssertion(getContext(requestXml));
    }

    private Pair<AssertionStatus, RestmanMessage> callManagementAssertion(
            final PolicyEnforcementContext context) throws GatewayManagementDocumentUtilities.AccessDeniedManagementResponse, UnexpectedManagementResponse {
        final AssertionStatus assertionStatus;
        try {
            assertionStatus = gatewayManagementInvoker.checkRequest(context);
        } catch (IOException | PolicyAssertionException e) {
            throw new RuntimeException("Unexpected internal error invoking gateway management service: " + e.getMessage(), e);
        }
        final RestmanMessage responseMessage;
        try {
            responseMessage = new RestmanMessage(context.getResponse());

            // check for unexpected internal error
            if (responseMessage == null) {
                throw new UnexpectedManagementResponse("Unexpected exception: a call result was expected.");
            } else if (responseMessage.isErrorResponse()) {
                throw new UnexpectedManagementResponse(responseMessage.getAsFormattedString());
            }
        } catch (SAXException | IOException | NoSuchPartException e) {
            throw new RuntimeException("Unexpected internal error parsing gateway management response: " + e.getMessage(), e);
        }
        return new Pair<>(assertionStatus, responseMessage);
    }

    public Pair<AssertionStatus, RestmanMessage> callManagementCheckInterrupted(@Nullable PolicyEnforcementContext pec, String requestXml)
            throws InterruptedException, GatewayManagementDocumentUtilities.AccessDeniedManagementResponse, UnexpectedManagementResponse {

        final Pair<AssertionStatus, RestmanMessage> documentPair;
        try {
            if (pec == null) {
                documentPair = callManagementAssertion(requestXml);
            } else {
                documentPair = callManagementAssertion(pec);
            }
        } catch (UnexpectedManagementResponse e) {
            if (e.isCausedByMgmtAssertionInternalError() && cancelledCallback.call()) {
                throw new InterruptedException("Possible interruption detected due to internal error");
            } else {
                throw e;
            }
        } catch (GatewayManagementDocumentUtilities.AccessDeniedManagementResponse e) {
            if (logger.isLoggable(Level.FINE)) {
                logger.log(Level.FINE, "Access denied for request:" + e.getDeniedRequest());
            }
            throw e;
        }
        return documentPair;
    }

    public PolicyEnforcementContext getContext(String requestXml) {
        final Message request = new Message();
        final ContentTypeHeader contentTypeHeader = ContentTypeHeader.XML_DEFAULT;
        try {
            request.initialize(contentTypeHeader, requestXml.getBytes(Charsets.UTF8));
        } catch (IOException e) {
            // this is a programming error. All requests are generated internally.
            throw new RuntimeException("Unexpected internal error preparing gateway management request: " + e.getMessage(), e);
        }

        HttpRequestKnob requestKnob = new HttpRequestKnobAdapter() {
            @Override
            public String getRemoteAddress() {
                return "127.0.0.1";
            }

            @Override
            public String getRemoteHost() {
                return "127.0.0.1";
            }

            @Override
            public String getRequestUrl() {
                return URL_LOCALHOST_8080_RESTMAN;
            }

            @Override
            public String getHeaderFirstValue(String name) {
                if (HttpHeaders.CONTENT_TYPE.equals(name)) {
                    return ContentTypeHeader.XML_DEFAULT.getFullValue();
                } else {
                    return super.getHeaderFirstValue(name);
                }
            }
        };
        request.attachHttpRequestKnob(requestKnob);   // note: attachKnob(HttpRequestKnob.class, requestKnob) returns null for Message.getTcpKnob() in ServerRESTGatewayManagementAssertion

        final Message response = new Message();
        response.attachHttpResponseKnob(new AbstractHttpResponseKnob() {});   // note: attachKnob(HttpResponseKnob.class, new AbstractHttpResponseKnob() {}) returns null for Message.getHttpResponseKnob() in ServerRESTGatewayManagementAssertion

        final PolicyEnforcementContext context = PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, response);

        final UserBean authenticatedUser = getAuthenticatedUser();
        if (authenticatedUser != null) {
            context.getDefaultAuthenticationContext().addAuthenticationResult(
                    new AuthenticationResult(authenticatedUser, new HttpBasicToken(authenticatedUser.getLogin(), "".toCharArray()), null, false));
        } else {
            // no action will be allowed - this will result in permission denied later
            logger.warning("No administrative user found. Request to install will fail.");
        }

        context.setVariable(VAR_RESTMAN_URI, URL_1_0_BUNDLE + "?test=false");
        context.setVariable(VAR_RESTMAN_CONTENT_TYPE, ContentTypeHeader.XML_DEFAULT.getFullValue());
        context.setVariable(VAR_RESTMAN_BASE_URI, URL_LOCALHOST_8080_RESTMAN);
        context.setVariable(VAR_RESTMAN_ACTION, "PUT");
        final PublishedService service = new PublishedService();
        service.setRoutingUri("/restman*");
        context.setService(service);

        return context;
    }
}
