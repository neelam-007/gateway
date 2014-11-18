package com.l7tech.external.assertions.retrieveservicewsdl.server;

import com.l7tech.common.io.DocumentReferenceProcessor;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.common.protocol.SecureSpanConstants;
import com.l7tech.external.assertions.retrieveservicewsdl.RetrieveServiceWsdlAssertion;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.gateway.common.service.ServiceDocument;
import com.l7tech.message.Message;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.variable.NoSuchVariableException;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.AbstractServerAssertion;
import com.l7tech.server.policy.assertion.AssertionStatusException;
import com.l7tech.server.policy.variable.ExpandVariables;
import com.l7tech.server.service.ServiceCache;
import com.l7tech.server.service.ServiceDocumentManager;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.Functions;
import com.l7tech.util.ValidationUtils;
import com.l7tech.wsdl.WsdlUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.inject.Inject;
import java.io.IOException;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.Collection;
import java.util.Map;

import static com.l7tech.gateway.common.audit.AssertionMessages.*;
import static com.l7tech.util.ExceptionUtils.getDebugException;

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
    
    @Inject
    protected ServiceDocumentManager serviceDocumentManager;

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

        if (null == protocol || protocol.isEmpty()) {
            logAndAudit(RETRIEVE_WSDL_NO_PROTOCOL);
            return AssertionStatus.FAILED;
        }

        // check host
        if (null == host || host.isEmpty()) {
            logAndAudit(RETRIEVE_WSDL_NO_HOSTNAME);
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
            targetMessage = context.getOrCreateTargetMessage(assertion.getMessageTarget(), false);
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

        // perform reference rewriting, if proxying is necessary or enabled
        if (isProxyingRequired(service)) {
            // get any dependency service documents
            final Collection<ServiceDocument> documents = getImportedDocumentsToProxy(service);

            // get the routing uri for the service we are running in the context of, e.g. the WSDL Query Handler service
            String wsdlProxyUri;

            try {
                String wsdlHandlerServiceRoutingUri = getRoutingUri(context.getService());

                wsdlProxyUri = new URI(context.getRequest().getHttpRequestKnob().getRequestUrl())
                        .resolve(wsdlHandlerServiceRoutingUri).toString();
            } catch (Exception e) {
                logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO,
                        new String[] {"Unable to determine absolute URL for WSDL Proxy: " + ExceptionUtils.getMessage(e)},
                        ExceptionUtils.getDebugException(e));
                throw new AssertionStatusException(AssertionStatus.SERVER_ERROR);
            }

            // rewrite references
            rewriteReferences(service.getId(), service.getWsdlUrl(), wsdlDoc, documents, wsdlProxyUri,
                    new Functions.UnaryVoid<Exception>() {
                        @Override
                        public void call(Exception e) {
                            logAndAudit(AssertionMessages.RETRIEVE_WSDL_PROXY_URL_CREATION_FAILURE,
                                    new String[] {ExceptionUtils.getMessage(e)}, getDebugException(e));
                        }
                    }
            );
        }

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

        // save wsdl to target message
        targetMessage.initialize(wsdlDoc, ContentTypeHeader.XML_DEFAULT);

        return AssertionStatus.NONE;
    }

    private Collection<ServiceDocument> getImportedDocumentsToProxy(PublishedService service) {
        Collection<ServiceDocument> documents;

        try {
            documents = serviceDocumentManager.findByServiceIdAndType(service.getGoid(), "WSDL-IMPORT");
        } catch (FindException e) {
            logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO,
                    new String[] {ExceptionUtils.getMessage(e)}, ExceptionUtils.getDebugException(e));
            throw new AssertionStatusException(AssertionStatus.SERVER_ERROR);
        }

        return documents;
    }

    private boolean isProxyingRequired(PublishedService service) {
        return service.isInternal()
                || (service.getWsdlUrl() != null && service.getWsdlUrl().startsWith("file:"))
                || assertion.isProxyDependencies();
    }

    /**
     * Rewrite any dependency references (schema/wsdl) in the given doc to the given request URI
     */
    public void rewriteReferences(final String serviceId,
                                    final String serviceWsdlUrl,
                                    final Document wsdlDoc,
                                    final Collection<ServiceDocument> documents,
                                    final String requestUri,
                                    final Functions.UnaryVoid<Exception> errorHandler) {
        final String NOOP_WSDL = "<wsdl:definitions xmlns:wsdl=\"http://schemas.xmlsoap.org/wsdl/\"/>";

        if (!documents.isEmpty()) {
            DocumentReferenceProcessor documentReferenceProcessor = new DocumentReferenceProcessor();
            documentReferenceProcessor.processDocumentReferences(wsdlDoc, new DocumentReferenceProcessor.ReferenceCustomizer() {
                @Override
                public String customize(final Document document,
                                        final Node node,
                                        final String documentUrl,
                                        final DocumentReferenceProcessor.ReferenceInfo referenceInfo) {
                    String uri = null;

                    if (documentUrl != null && referenceInfo.getReferenceUrl() != null) {
                        try {
                            URI base = new URI(documentUrl);
                            String docUrl = base.resolve(new URI(referenceInfo.getReferenceUrl())).toString();
                            if (docUrl.equals(serviceWsdlUrl)) {
                                uri = requestUri + "?" + SecureSpanConstants.HttpQueryParameters.PARAM_SERVICEOID + "=" + serviceId;
                            } else {
                                for (ServiceDocument serviceDocument : documents) {
                                    if (docUrl.equals(serviceDocument.getUri())) {
                                        // Don't proxy WSDL if we generated it in place of a directly imported XSD
                                        // This occurred prior to 4.5 when we stripped XSDs on import since we only
                                        // used the WSDL documents.
                                        if (!NOOP_WSDL.equals(serviceDocument.getContents())) {
                                            uri = requestUri + "/" + getName(serviceDocument) + "?" +
                                                    SecureSpanConstants.HttpQueryParameters.PARAM_SERVICEOID + "=" + serviceId + "&" +
                                                    SecureSpanConstants.HttpQueryParameters.PARAM_SERVICEDOCOID + "=" + serviceDocument.getId();
                                        }
                                        break;
                                    }
                                }
                            }
                        } catch (Exception e) {
                            errorHandler.call(e);
                        }
                    }

                    return uri;
                }
            });
        }
    }

    /**
     * Create a "user-friendly" display name for the document.
     */
    private String getName(final ServiceDocument serviceDocument) {
        String name = serviceDocument.getUri();

        int index = name.lastIndexOf('/');
        if (index >= 0) {
            name = name.substring(index+1);
        }

        index = name.indexOf('?');
        if (index >= 0) {
            name = name.substring(0, index);
        }

        index = name.indexOf('#');
        if (index >= 0) {
            name = name.substring(0, index);
        }

        String permittedCharacters = ValidationUtils.ALPHA_NUMERIC  + "_-.";
        StringBuilder nameBuilder = new StringBuilder();
        for (char nameChar : name.toCharArray()) {
            if (permittedCharacters.indexOf(nameChar) >= 0) {
                nameBuilder.append(nameChar);
            }
        }

        return nameBuilder.toString();
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
