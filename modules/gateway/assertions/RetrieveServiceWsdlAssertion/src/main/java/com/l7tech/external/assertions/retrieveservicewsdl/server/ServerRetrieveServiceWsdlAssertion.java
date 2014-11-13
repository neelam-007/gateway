package com.l7tech.external.assertions.retrieveservicewsdl.server;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.common.protocol.SecureSpanConstants;
import com.l7tech.external.assertions.retrieveservicewsdl.RetrieveServiceWsdlAssertion;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.message.Message;
import com.l7tech.objectmodel.Goid;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.variable.NoSuchVariableException;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.AbstractServerAssertion;
import com.l7tech.server.policy.assertion.AssertionStatusException;
import com.l7tech.server.policy.variable.ExpandVariables;
import com.l7tech.server.service.ServiceCache;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.wsdl.WsdlUtil;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.inject.Inject;
import java.io.IOException;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;

import static com.l7tech.gateway.common.audit.AssertionMessages.*;
import static com.l7tech.util.ExceptionUtils.getDebugException;
import static com.l7tech.util.ExceptionUtils.getMessage;

/**
 * Server side implementation of the RetrieveServiceWsdlAssertion.
 *
 * @see com.l7tech.external.assertions.retrieveservicewsdl.RetrieveServiceWsdlAssertion
 *
 * @author Jamie Williams - jamie.williams2@ca.com
 */
public class ServerRetrieveServiceWsdlAssertion extends AbstractServerAssertion<RetrieveServiceWsdlAssertion> {
    @Inject
    protected ServiceCache serviceCache;

    private final String[] variablesUsed;

    public ServerRetrieveServiceWsdlAssertion(final RetrieveServiceWsdlAssertion assertion) {
        super(assertion);

        this.variablesUsed = assertion.getVariablesUsed();
    }

    public AssertionStatus checkRequest(final PolicyEnforcementContext context)
            throws IOException, PolicyAssertionException {
        Map<String, Object> vars = context.getVariableMap(variablesUsed, getAudit());

        final String serviceIdString = ExpandVariables.process(assertion.getServiceId(), vars, getAudit(), true);
        final String host = ExpandVariables.process(assertion.getHost(), vars, getAudit(), true);
        final String portString = ExpandVariables.process(assertion.getPort(), vars, getAudit(), true);

        // get protocol
        final String protocol = getProtocol(context);

        if (null == protocol ||  protocol.isEmpty()) {
            logAndAudit(RETRIEVE_WSDL_NO_PROTOCOL);
            return AssertionStatus.FAILED;
        }

        // get port
        if (portString.isEmpty()) {
            logAndAudit(RETRIEVE_WSDL_NO_PORT);
            return AssertionStatus.FAILED;
        }

        final int port = getPort(portString);

        // get target message
        Message targetMessage;

        try {
            targetMessage = context.getOrCreateTargetMessage(assertion.getTargetMessage(), false);
        } catch (NoSuchVariableException e) {
            logAndAudit(NO_SUCH_VARIABLE_WARNING, new String[] {e.getMessage()}, getDebugException(e));
            return AssertionStatus.FAILED;
        }

        // parse service goid
        Goid serviceGoid;

        try {
            serviceGoid = Goid.parseGoid(serviceIdString);
        } catch (IllegalArgumentException e) {
            logAndAudit(RETRIEVE_WSDL_INVALID_SERVICE_ID,
                    new String[] {ExceptionUtils.getMessage(e)}, getDebugException(e));
            return AssertionStatus.FAILED;
        }

        // get published service by goid
        PublishedService service = serviceCache.getCachedService(serviceGoid);

        if (null == service) {
            logAndAudit(RETRIEVE_WSDL_SERVICE_NOT_FOUND, serviceIdString);
            return AssertionStatus.FAILED;
        }

        // does the service have a WSDL?
        if (!service.isSoap()) {
            logAndAudit(RETRIEVE_WSDL_SERVICE_NOT_SOAP);
            return AssertionStatus.FAILED;
        }

        // get & parse WSDL xml
        Document wsdlDoc = getWsdlDocument(service);

        // ---

        // rewrite references

        // TODO jwilliams: construct proxy url (pointing to internal service), rewrite imports. (make optional?)

        // ---

        // construct endpoint URL
        String routingUri = getRoutingUri(service);

        URL endpointUrl;

        try {
            endpointUrl = new URL(protocol, host, port, routingUri);
        } catch (MalformedURLException e) {
            logAndAudit(RETRIEVE_WSDL_INVALID_ENDPOINT_URL,
                    new String[] {ExceptionUtils.getMessage(e)}, getDebugException(e));
            return AssertionStatus.FAILED;
        }

        // add/update endpoints
        WsdlUtil.addOrUpdateEndpoints(wsdlDoc, endpointUrl, serviceIdString);

        // ---

        // add security policy?

        // TODO jwilliams: add security policy in the internal service policy?

        // ---

        // save wsdl to target message
        targetMessage.initialize(wsdlDoc, ContentTypeHeader.XML_DEFAULT);

        return AssertionStatus.NONE;
    }

    private String getProtocol(final PolicyEnforcementContext context) {
        if (null == assertion.getProtocolVariable()) {
            return assertion.getProtocol();
        }

        final Object value;
        final String protocolVariable = assertion.getProtocolVariable();

        try {
            value = context.getVariable(protocolVariable);
        } catch (NoSuchVariableException e) {
            logAndAudit(NO_SUCH_VARIABLE_WARNING, protocolVariable);
            throw new AssertionStatusException(AssertionStatus.FAILED);
        }

        if (value instanceof String) {
            return ((String) value).trim();
        } else {
            logAndAudit(VARIABLE_INVALID_VALUE, protocolVariable, "String");
            throw new AssertionStatusException(AssertionStatus.FAILED);
        }
    }

    private int getPort(String portString) {
        int port;

        try {
            port = Integer.parseInt(portString);
        } catch (NumberFormatException e) {
            logAndAudit(RETRIEVE_WSDL_INVALID_PORT, portString);
            throw new AssertionStatusException(AssertionStatus.FAILED);
        }

        if (port < RetrieveServiceWsdlAssertion.PORT_RANGE_START ||
                port > RetrieveServiceWsdlAssertion.PORT_RANGE_END) {
            logAndAudit(RETRIEVE_WSDL_INVALID_PORT, Integer.toString(port));
            throw new AssertionStatusException(AssertionStatus.FAILED);
        }

        return port;
    }

    private Document getWsdlDocument(PublishedService service) throws IOException {
        InputSource input = new InputSource();
        input.setSystemId(service.getWsdlUrl());
        input.setCharacterStream(new StringReader(service.getWsdlXml()));

        try {
            return XmlUtil.parse(input, false);
        } catch (SAXException e) {
            logAndAudit(RETRIEVE_WSDL_ERROR_PARSING_WSDL,
                    new String[] {ExceptionUtils.getMessage(e)}, getDebugException(e));
            throw new AssertionStatusException(AssertionStatus.SERVER_ERROR);
        }
    }

    private String getRoutingUri(PublishedService service) {
        String routingUri = service.getRoutingUri();

        if (null == routingUri || routingUri.isEmpty()) {
            routingUri = SecureSpanConstants.SERVICE_FILE + service.getId(); // refer to service by its ID
        }

        return routingUri;
    }
}
