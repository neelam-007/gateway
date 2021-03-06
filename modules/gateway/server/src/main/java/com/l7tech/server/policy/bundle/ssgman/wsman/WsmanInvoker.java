package com.l7tech.server.policy.bundle.ssgman.wsman;

import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.identity.UserBean;
import com.l7tech.message.*;
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
import org.jetbrains.annotations.NotNull;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.util.logging.Level;

import static com.l7tech.server.policy.bundle.GatewayManagementDocumentUtilities.isAccessDeniedResponse;

/**
 * Common code useful for wsman policy bundle export and install.
 */
public class WsmanInvoker extends BaseGatewayManagementInvoker {

    public static final String GATEWAY_MGMT_ENUMERATE_FILTER = "<env:Envelope xmlns:env=\"http://www.w3.org/2003/05/soap-envelope\"\n" +
            "    xmlns:l7=\"http://ns.l7tech.com/2010/04/gateway-management\"\n" +
            "    xmlns:wsa=\"http://schemas.xmlsoap.org/ws/2004/08/addressing\"\n" +
            "    xmlns:wse=\"http://schemas.xmlsoap.org/ws/2004/08/eventing\"\n" +
            "    xmlns:wsen=\"http://schemas.xmlsoap.org/ws/2004/09/enumeration\"\n" +
            "    xmlns:wsman=\"http://schemas.dmtf.org/wbem/wsman/1/wsman.xsd\"\n" +
            "    xmlns:wxf=\"http://schemas.xmlsoap.org/ws/2004/09/transfer\" xmlns:xs=\"http://www.w3.org/2001/XMLSchema\">\n" +
            "    <env:Header>\n" +
            "        <wsa:Action env:mustUnderstand=\"true\">http://schemas.xmlsoap.org/ws/2004/09/enumeration/Enumerate</wsa:Action>\n" +
            "        <wsa:ReplyTo>\n" +
            "            <wsa:Address env:mustUnderstand=\"true\">http://schemas.xmlsoap.org/ws/2004/08/addressing/role/anonymous</wsa:Address>\n" +
            "        </wsa:ReplyTo>\n" +
            "        <wsa:MessageID env:mustUnderstand=\"true\">{0}</wsa:MessageID>\n" +
            "        <wsa:To env:mustUnderstand=\"true\">http://localhost:8080/wsman</wsa:To>\n" +
            "        <wsman:ResourceURI>{1}</wsman:ResourceURI>\n" +
            "        <wsman:RequestTotalItemsCountEstimate/>\n" +
            "    </env:Header>\n" +
            "    <env:Body>\n" +
            "        <wsen:Enumerate>\n" +
            "            <wsman:OptimizeEnumeration/>\n" +
            "            <wsman:MaxElements>{2}</wsman:MaxElements>\n" +
            "            <wsman:Filter>{3}</wsman:Filter>\n" +
            "<wsman:EnumerationMode>EnumerateObjectAndEPR</wsman:EnumerationMode>" +
            "        </wsen:Enumerate>\n" +
            "    </env:Body>\n" +
            "</env:Envelope>";

    public static final String CREATE_ENTITY_XML = "<env:Envelope xmlns:env=\"http://www.w3.org/2003/05/soap-envelope\"\n" +
            "    xmlns:wsa=\"http://schemas.xmlsoap.org/ws/2004/08/addressing\"\n" +
            "    xmlns:wse=\"http://schemas.xmlsoap.org/ws/2004/08/eventing\"\n" +
            "    xmlns:wsen=\"http://schemas.xmlsoap.org/ws/2004/09/enumeration\"\n" +
            "    xmlns:wsman=\"http://schemas.dmtf.org/wbem/wsman/1/wsman.xsd\"\n" +
            "    xmlns:wxf=\"http://schemas.xmlsoap.org/ws/2004/09/transfer\" xmlns:xs=\"http://www.w3.org/2001/XMLSchema\">\n" +
            "    <env:Header>\n" +
            "        <wsa:Action env:mustUnderstand=\"true\">http://schemas.xmlsoap.org/ws/2004/09/transfer/Create</wsa:Action>\n" +
            "        <wsa:ReplyTo>\n" +
            "            <wsa:Address env:mustUnderstand=\"true\">http://schemas.xmlsoap.org/ws/2004/08/addressing/role/anonymous</wsa:Address>\n" +
            "        </wsa:ReplyTo>\n" +
            "        <wsa:MessageID env:mustUnderstand=\"true\">{0}</wsa:MessageID>\n" +
            "        <wsa:To env:mustUnderstand=\"true\">https://localhost:9443/wsman</wsa:To>\n" +
            "        <wsman:ResourceURI>{1}</wsman:ResourceURI>\n" +
            "        <wsman:OperationTimeout>PT5M0.000S</wsman:OperationTimeout>\n" +
            "    </env:Header>\n" +
            "    <env:Body>\n" +
            "       {2}\n" +
            "    </env:Body>\n" +
            "</env:Envelope>";

    /**
     * Requires in this order: UUID, Resource URI, selector name (id or name), selector value
     */
    public static final String GATEWAY_MGMT_GET_ENTITY = "<env:Envelope xmlns:env=\"http://www.w3.org/2003/05/soap-envelope\"\n" +
            "    xmlns:wsa=\"http://schemas.xmlsoap.org/ws/2004/08/addressing\"\n" +
            "    xmlns:wse=\"http://schemas.xmlsoap.org/ws/2004/08/eventing\"\n" +
            "    xmlns:wsen=\"http://schemas.xmlsoap.org/ws/2004/09/enumeration\"\n" +
            "    xmlns:wsman=\"http://schemas.dmtf.org/wbem/wsman/1/wsman.xsd\"\n" +
            "    xmlns:wxf=\"http://schemas.xmlsoap.org/ws/2004/09/transfer\" xmlns:xs=\"http://www.w3.org/2001/XMLSchema\">\n" +
            "    <env:Header>\n" +
            "        <wsa:Action env:mustUnderstand=\"true\">http://schemas.xmlsoap.org/ws/2004/09/transfer/Get</wsa:Action>\n" +
            "        <wsa:ReplyTo>\n" +
            "            <wsa:Address env:mustUnderstand=\"true\">http://schemas.xmlsoap.org/ws/2004/08/addressing/role/anonymous</wsa:Address>\n" +
            "        </wsa:ReplyTo>\n" +
            "        <wsa:MessageID env:mustUnderstand=\"true\">{0}</wsa:MessageID>\n" +
            "        <wsa:To env:mustUnderstand=\"true\">http://localhost:8080/wsman</wsa:To>\n" +
            "        <wsman:ResourceURI>{1}</wsman:ResourceURI>\n" +
            "        <wsman:OperationTimeout>P0Y0M0DT0H5M0.000S</wsman:OperationTimeout>\n" +
            "        <wsman:SelectorSet>\n" +
            "            <wsman:Selector Name=\"{2}\">{3}</wsman:Selector>\n" +
            "        </wsman:SelectorSet>\n" +
            "    </env:Header>\n" +
            "    <env:Body/>\n" +
            "</env:Envelope>";

    public WsmanInvoker(@NotNull final Functions.Nullary<Boolean> cancelledCallback,
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
    private Pair<AssertionStatus, Document> callManagementAssertion(final String requestXml) throws GatewayManagementDocumentUtilities.AccessDeniedManagementResponse, GatewayManagementDocumentUtilities.UnexpectedManagementResponse {

        final PolicyEnforcementContext context = getContext(requestXml);

        final AssertionStatus assertionStatus;
        try {
            assertionStatus = gatewayManagementInvoker.checkRequest(context);
        } catch (IOException | PolicyAssertionException e) {
            throw new RuntimeException("Unexpected internal error invoking gateway management service: " + e.getMessage(), e);
        }
        final Message response = context.getResponse();
        final Document document;
        try {
            document = response.getXmlKnob().getDocumentReadOnly();
            // validate that an Internal Error was not received. If so this is most likely due to Wiseman being interrupted
            // via user cancellation.
            if (GatewayManagementDocumentUtilities.isInternalErrorResponse(document)) {
                throw new GatewayManagementDocumentUtilities.UnexpectedManagementResponse(true);
            } else if (isAccessDeniedResponse(document)) {
                throw new GatewayManagementDocumentUtilities.AccessDeniedManagementResponse("Access Denied", requestXml);
            }
        } catch (SAXException | IOException e) {
            throw new RuntimeException("Unexpected internal error parsing gateway management response: " + e.getMessage(), e);
        }
        return new Pair<>(assertionStatus, document);
    }

    public Pair<AssertionStatus, Document> callManagementCheckInterrupted(String requestXml) throws InterruptedException,
            GatewayManagementDocumentUtilities.AccessDeniedManagementResponse, GatewayManagementDocumentUtilities.UnexpectedManagementResponse {

        final Pair<AssertionStatus, Document> documentPair;
        try {
            documentPair = callManagementAssertion(requestXml);
        } catch (GatewayManagementDocumentUtilities.UnexpectedManagementResponse e) {
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

    private PolicyEnforcementContext getContext(String requestXml) {

        final Message request = new Message();
        final ContentTypeHeader contentTypeHeader = ContentTypeHeader.SOAP_1_2_DEFAULT;
        try {
            request.initialize(contentTypeHeader, requestXml.getBytes(Charsets.UTF8));
        } catch (IOException e) {
            // this is a programming error. All requests are generated internally.
            throw new RuntimeException("Unexpected internal error preparing gateway management request: " + e.getMessage(), e);
        }

        HttpRequestKnob requestKnob = new HttpRequestKnobAdapter(){
            @Override
            public String getRemoteAddress() {
                return "127.0.0.1";
            }

            @Override
            public String getRequestUrl() {
                // Used when creating a EndpointReferenceType for resource iterators in gateway mgmt assertion.
                return "http://localhost:8080/wsman";
            }
        };
        request.attachKnob(HttpRequestKnob.class, requestKnob);

        HttpResponseKnob responseKnob = new AbstractHttpResponseKnob() {};

        final Message response = new Message();
        response.attachKnob(HttpResponseKnob.class, responseKnob);

        final PolicyEnforcementContext context = PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, response);

        final UserBean authenticatedUser = getAuthenticatedUser();
        if (authenticatedUser != null) {
            context.getDefaultAuthenticationContext().addAuthenticationResult(
                    new AuthenticationResult(authenticatedUser, new HttpBasicToken(authenticatedUser.getLogin(), "".toCharArray()), null, false));
        } else {
            // no action will be allowed - this will result in permission denied later
            logger.warning("No administrative user found. Request to install will fail.");
        }

        return context;
    }
}
