package com.l7tech.external.assertions.retrieveservicewsdl.server;

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
import com.l7tech.util.Pair;
import com.l7tech.wsdl.WsdlUtil;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.inject.Inject;
import java.io.IOException;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.Collection;
import java.util.HashMap;
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

    public AssertionStatus checkRequest(final PolicyEnforcementContext context) throws PolicyAssertionException {
        Map<String, Object> vars = context.getVariableMap(variablesUsed, getAudit());

        // parse service goid
        final PublishedService service = getService(vars);

        // construct endpoint URL
        final URL endpointUrl = getEndpointUrl(context, vars, service);

        // retrieve the requested document
        Document document;

        if (assertion.isRetrieveDependency()) {
            document = retrieveWsdlDependencyDocument(context, vars, service, endpointUrl);
        } else {
            document = retrieveWsdlDocument(context, service, endpointUrl);
        }

        // save document to target message
        Message targetMessage = getTargetMessage(context);

        targetMessage.initialize(document, ContentTypeHeader.XML_DEFAULT);

        return AssertionStatus.NONE;
    }

    private Document retrieveWsdlDependencyDocument(PolicyEnforcementContext context, Map<String, Object> vars,
                                                    PublishedService service, URL endpointUrl) {
        Document document = null;

        // get service document goid
        Goid serviceDocumentGoid;

        if (StringUtils.isBlank(assertion.getServiceDocumentId())) {
            logAndAudit(RETRIEVE_WSDL_NO_SERVICE_DOCUMENT_ID);
            throw new AssertionStatusException(AssertionStatus.FAILED);
        }

        String serviceDocumentIdString = ExpandVariables.process(assertion.getServiceDocumentId(), vars, getAudit(), true);

        if (StringUtils.isBlank(serviceDocumentIdString)) {
            logAndAudit(RETRIEVE_WSDL_SERVICE_DOCUMENT_ID_BLANK);
            throw new AssertionStatusException(AssertionStatus.FAILED);
        }

        try {
            serviceDocumentGoid = Goid.parseGoid(serviceDocumentIdString);
        } catch (IllegalArgumentException e) {
            logAndAudit(RETRIEVE_WSDL_INVALID_SERVICE_DOCUMENT_ID,
                    new String[]{ExceptionUtils.getMessage(e)}, getDebugException(e));
            throw new AssertionStatusException(AssertionStatus.FAILED);
        }

        // check assertion is properly configured - proxying must be enabled for dependency requests
        if (!assertion.isProxyDependencies()) {
            logAndAudit(RETRIEVE_WSDL_PROXYING_DISABLED_FOR_DEPENDENCY);
            throw new AssertionStatusException(AssertionStatus.FAILED);
        }

        // get any dependency service documents
        final Collection<ServiceDocument> dependencyDocuments = getImportedDocumentsToProxy(service);

        for (ServiceDocument dependency : dependencyDocuments) {
            if (dependency.getGoid().equals(serviceDocumentGoid)) {
                try {
                    document = parseDocument(dependency.getUri(), dependency.getContents());
                    break;
                } catch (IOException | SAXException e) {
                    logAndAudit(RETRIEVE_WSDL_ERROR_PARSING_SERVICE_DOCUMENT,
                            new String[]{ExceptionUtils.getMessage(e)}, getDebugException(e));
                    throw new AssertionStatusException(AssertionStatus.SERVER_ERROR);
                }
            }
        }

        if (null == document) {
            logAndAudit(RETRIEVE_WSDL_SERVICE_DOCUMENT_NOT_FOUND, serviceDocumentIdString);
            throw new AssertionStatusException(AssertionStatus.FAILED);
        }

        // get a routing uri for the service we are running in the context of, e.g. the WSDL Query Handler service
        String proxyUri = getDependencyProxyUri(context);

        // rewrite references
        rewriteReferences(service, document, dependencyDocuments, proxyUri, new Functions.UnaryVoid<Exception>() {
            @Override
            public void call(Exception e) {
                logAndAudit(AssertionMessages.RETRIEVE_WSDL_PROXY_URL_CREATION_FAILURE,
                        new String[] {ExceptionUtils.getMessage(e)}, getDebugException(e));
            }
        });

        // update endpoints - specifying null for the service ID means endpoints should not be added
        WsdlUtil.addOrUpdateEndpoints(document, endpointUrl, null);

        return document;
    }

    private Document retrieveWsdlDocument(PolicyEnforcementContext context,
                                          PublishedService service, URL endpointUrl) {
        Document document;

        // parse service WSDL xml
        try {
            document = parseDocument(service.getWsdlUrl(), service.getWsdlXml());
        } catch (IOException | SAXException e) {
            logAndAudit(RETRIEVE_WSDL_ERROR_PARSING_WSDL,
                    new String[] {ExceptionUtils.getMessage(e)}, getDebugException(e));
            throw new AssertionStatusException(AssertionStatus.SERVER_ERROR);
        }

        // perform reference rewriting, if proxying is necessary or enabled
        if (isProxyingRequired(service)) {
            // get any dependency service documents
            final Collection<ServiceDocument> dependencyDocuments = getImportedDocumentsToProxy(service);

            // get a routing uri for the service we are running in the context of, e.g. the WSDL Query Handler service
            String proxyUri = getDependencyProxyUri(context);

            // rewrite references
            rewriteReferences(service, document, dependencyDocuments, proxyUri, new Functions.UnaryVoid<Exception>() {
                        @Override
                        public void call(Exception e) {
                            logAndAudit(AssertionMessages.RETRIEVE_WSDL_PROXY_URL_CREATION_FAILURE,
                                    new String[] {ExceptionUtils.getMessage(e)}, getDebugException(e));
                        }
                    });
        }

        // add/update endpoints
        WsdlUtil.addOrUpdateEndpoints(document, endpointUrl, service.getId());

        return document;
    }

    private String getDependencyProxyUri(PolicyEnforcementContext context) {
        String proxyUri;

        try {
            String wsdlHandlerServiceRoutingUri = getRoutingUri(context.getService());

            proxyUri = new URI(context.getRequest().getHttpRequestKnob().getRequestUrl())
                    .resolve(wsdlHandlerServiceRoutingUri).toString();
        } catch (Exception e) {
            logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO,
                    new String[] {"Unable to determine absolute URL for WSDL Proxy: " + ExceptionUtils.getMessage(e)},
                    ExceptionUtils.getDebugException(e));
            throw new AssertionStatusException(AssertionStatus.SERVER_ERROR);
        }

        return proxyUri;
    }

    private PublishedService getService(Map<String, Object> vars) {
        String serviceIdString = ExpandVariables.process(assertion.getServiceId(), vars, getAudit(), true);

        if (StringUtils.isBlank(serviceIdString)) {
            logAndAudit(RETRIEVE_WSDL_SERVICE_ID_BLANK);
            throw new AssertionStatusException(AssertionStatus.FAILED);
        }

        Goid serviceGoid;

        try {
            serviceGoid = Goid.parseGoid(serviceIdString);
        } catch (IllegalArgumentException e) {
            logAndAudit(RETRIEVE_WSDL_INVALID_SERVICE_ID,
                    new String[] {ExceptionUtils.getMessage(e)}, getDebugException(e));
            throw new AssertionStatusException(AssertionStatus.FAILED);
        }

        // get published service by goid
        PublishedService service = serviceCache.getCachedService(serviceGoid);

        if (null == service) {
            logAndAudit(RETRIEVE_WSDL_SERVICE_NOT_FOUND, serviceGoid.toString());
            throw new AssertionStatusException(AssertionStatus.FAILED);
        }

        // does the service have a WSDL?
        if (!service.isSoap()) {
            logAndAudit(RETRIEVE_WSDL_SERVICE_NOT_SOAP);
            throw new AssertionStatusException(AssertionStatus.FAILED);
        }

        return service;
    }

    private URL getEndpointUrl(PolicyEnforcementContext context, Map<String, Object> vars, PublishedService service) {
        URL endpointUrl;

        String protocol = getProtocol(context);
        String host = getHost(vars);
        int port = getPort(vars);
        String path = getRoutingUri(service);

        try {
            endpointUrl = new URL(protocol, host, port, path);
        } catch (MalformedURLException e) {
            logAndAudit(RETRIEVE_WSDL_INVALID_ENDPOINT_URL,
                    new String[] {ExceptionUtils.getMessage(e)}, getDebugException(e));
            throw new AssertionStatusException(AssertionStatus.FAILED);
        }

        return endpointUrl;
    }

    private @NotNull String getProtocol(final PolicyEnforcementContext context) {
        final String protocol;

        if (null == assertion.getProtocolVariable()) {
            protocol = assertion.getProtocol();
        } else {
            final Object value;
            final String protocolVariable = assertion.getProtocolVariable();

            try {
                value = context.getVariable(protocolVariable);
            } catch (NoSuchVariableException e) {
                logAndAudit(NO_SUCH_VARIABLE_WARNING, protocolVariable);
                throw new AssertionStatusException(AssertionStatus.FAILED);
            }

            if (value instanceof String) {
                protocol = ((String) value).trim();
            } else {
                logAndAudit(VARIABLE_INVALID_VALUE, protocolVariable, "String");
                throw new AssertionStatusException(AssertionStatus.FAILED);
            }
        }

        if (StringUtils.isBlank(protocol)) {
            logAndAudit(RETRIEVE_WSDL_NO_PROTOCOL);
            throw new AssertionStatusException(AssertionStatus.FAILED);
        }

        return protocol;
    }

    private String getHost(Map<String, Object> vars) {
        final String host = ExpandVariables.process(assertion.getHost(), vars, getAudit(), true);

        if (StringUtils.isBlank(host)) {
            logAndAudit(RETRIEVE_WSDL_NO_HOSTNAME);
            throw new AssertionStatusException(AssertionStatus.FAILED);
        }

        return host;
    }

    private int getPort(Map<String, Object> vars) {
        final String portString = ExpandVariables.process(assertion.getPort(), vars, getAudit(), true);

        if (StringUtils.isBlank(portString)) {
            logAndAudit(RETRIEVE_WSDL_NO_PORT);
            throw new AssertionStatusException(AssertionStatus.FAILED);
        }

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

    private String getRoutingUri(PublishedService service) {
        return SecureSpanConstants.SERVICE_FILE + service.getId(); // refer to service by its ID
    }

    private Message getTargetMessage(PolicyEnforcementContext context) {
        Message targetMessage;

        try {
            targetMessage = context.getOrCreateTargetMessage(assertion.getMessageTarget(), false);
        } catch (NoSuchVariableException e) {
            logAndAudit(NO_SUCH_VARIABLE_WARNING, new String[] {e.getMessage()}, getDebugException(e));
            throw new AssertionStatusException(AssertionStatus.FAILED);
        }

        return targetMessage;
    }

    private void rewriteReferences(final PublishedService service,
                                   final Document wsdlDoc,
                                   final Collection<ServiceDocument> documents,
                                   final String wsdlProxyUri,
                                   final Functions.UnaryVoid<Exception> errorHandler) {
        final HashMap<String, Pair<String,String>> dependencies = new HashMap<>();

        for (ServiceDocument document : documents) {
            dependencies.put(document.getUri(), new Pair<>(document.getId(), document.getContents()));
        }

        WsdlUtil.rewriteReferences(service.getId(), service.getWsdlUrl(),
                wsdlDoc, dependencies, wsdlProxyUri, errorHandler, false);
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

    private Document parseDocument(String url, String xml) throws IOException, SAXException {
        InputSource input = new InputSource();
        input.setSystemId(url);
        input.setCharacterStream(new StringReader(xml));

        return XmlUtil.parse(input, false);
    }
}
