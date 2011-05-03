package com.l7tech.server.tokenservice;

import com.l7tech.common.io.DocumentReferenceProcessor;
import com.l7tech.gateway.common.service.ServiceDocument;
import com.l7tech.gateway.common.service.ServiceDocumentWsdlStrategy;
import com.l7tech.gateway.common.service.ServiceTemplate;
import com.l7tech.gateway.common.service.ServiceType;
import com.l7tech.server.service.ServiceTemplateManager;
import com.l7tech.util.HexUtils;
import com.l7tech.util.IOUtils;
import com.l7tech.util.ResourceUtils;
import com.l7tech.util.SoapConstants;
import com.l7tech.wsdl.ResourceTrackingWSDLLocator;
import com.l7tech.wsdl.WsdlEntityResolver;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class is to register a service template for the internal service of Security Token Service.
 *
 * @author ghuang
 */
public class SecurityTokenServiceTemplateRegistry implements InitializingBean, DisposableBean {
    private static final Logger logger = Logger.getLogger(SecurityTokenServiceTemplateRegistry.class.getName());

    private static final String DEFAULT_POLICY_FILE = "com/l7tech/server/tokenservice/resources/sts_default_policy.xml";
    private static final String DEFAULT_WSTRUST_NAMESPACE = SoapConstants.WST_NAMESPACE4;  // WS-Trust v1.4 is the default.
    private static final String FAKE_URL_PREFIX = "file://__ssginternal/";

    private Map<String, String> wsTrustNS_WSDL = new HashMap<String, String>();
    private ServiceTemplateManager serviceTemplateManager;
    private ServiceTemplate serviceTemplate;

    public SecurityTokenServiceTemplateRegistry(ServiceTemplateManager serviceTemplateManager) {
        this.serviceTemplateManager = serviceTemplateManager;

        initialize();
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        logger.info("Registering the '" + serviceTemplate.getName() + "' service with the gateway (Routing URI = " + serviceTemplate.getDefaultUriPrefix() + ")");
        serviceTemplateManager.register(serviceTemplate);
    }

    @Override
    public void destroy() throws Exception {
        logger.info("Unregistering the '" + serviceTemplate.getName() + "' service with the gateway (Routing URI = " + serviceTemplate.getDefaultUriPrefix() + ")");
        serviceTemplateManager.unregister(serviceTemplate);
    }

    /**
     * According to the given WS-Trust namespace, create a corresponding service template of Security Token Service.
     *
     * @param wsTrustNamespace: the namespace of a chosen WS-Trust version.
     * @return a service template associated with the corresponding WS-Trust version.
     */
    public ServiceTemplate createServiceTemplate(String wsTrustNamespace) {
        ServiceTemplate template = null;

        try {
            String url = FAKE_URL_PREFIX + wsTrustNS_WSDL.get(wsTrustNamespace);

            final WsdlEntityResolver entityResolver = new WsdlEntityResolver(true);
            final DocumentReferenceProcessor processor = new DocumentReferenceProcessor();
            final Map<String,String> contents = processor.processDocument(url, new DocumentReferenceProcessor.ResourceResolver() {
                @Override
                public String resolve(final String resourceUrl) throws IOException {
                    String resource = resourceUrl;
                    if ( resource.startsWith(FAKE_URL_PREFIX) ) {
                        resource = resourceUrl.substring(FAKE_URL_PREFIX.length());
                    }
                    String content = loadMyResource( resource, entityResolver );
                    return ResourceTrackingWSDLLocator.processResource(resourceUrl, content, entityResolver.failOnMissing(), false, true);
                }
            });

            final Collection<ResourceTrackingWSDLLocator.WSDLResource> sourceDocs =
                ResourceTrackingWSDLLocator.toWSDLResources(url, contents, false, false, false);

            final List<ServiceDocument> svcDocs = ServiceDocumentWsdlStrategy.fromWsdlResources( sourceDocs );

            String policyContents = getDefaultPolicyXml();
            template = new ServiceTemplate("Security Token Service", "/tokenservice", contents.get(url), url, policyContents, svcDocs, ServiceType.OTHER_INTERNAL_SERVICE, null);
        } catch (IOException e) {
            logger.log(Level.WARNING, "Can't load WSDL and/or Policy XML; service template will not be available", e);
        }

        return template;
    }

    private void initialize() {
        wsTrustNS_WSDL.put(SoapConstants.WST_NAMESPACE, "ws-trust-200404-1.1.wsdl");
        wsTrustNS_WSDL.put(SoapConstants.WST_NAMESPACE2, "ws-trust-200502-1.2.wsdl");
        wsTrustNS_WSDL.put(SoapConstants.WST_NAMESPACE3, "ws-trust-200512-1.3.wsdl");
        wsTrustNS_WSDL.put(SoapConstants.WST_NAMESPACE4, "ws-trust-200802-1.4.wsdl");

        serviceTemplate = createServiceTemplate(DEFAULT_WSTRUST_NAMESPACE);
    }

    private String loadMyResource( final String resource, final EntityResolver resolver ) throws IOException {
        byte[] bytes = null;

        InputSource in = null;
        try {
            in = resolver.resolveEntity( null, resource );
            if ( in != null ) {
                bytes = IOUtils.slurpStream( in.getByteStream() );
            }
        } catch ( SAXException e) {
            throw new IOException("Cannot load resource '"+resource+"'.", e);
        } finally {
            if ( in != null ) {
                ResourceUtils.closeQuietly( in.getByteStream() );
            }
        }

        if ( bytes == null ) {
            String resourcePath = resource;
            int dirIndex = resource.lastIndexOf( '/' );
            if ( dirIndex > 0 ) {
                resourcePath = resource.substring( dirIndex+1 );
            }

            String logMsg = "Loading a resource";
            String pathPrefix= "";
            if (resourcePath.endsWith(".wsdl")) {
                logMsg = "Loading wsdl resource";
                pathPrefix = "resources/wsdl/";
            } else if (resourcePath.endsWith(".xsd")) {
                logMsg = "Loading schema resource";
                pathPrefix = "resources/schema/";
            }

            logger.fine(logMsg + " '" + resource + "' as '" + resourcePath +"'.");
            bytes = IOUtils.slurpUrl(getClass().getResource(pathPrefix + resourcePath));
        }

        return HexUtils.decodeUtf8(bytes);
    }

    private String getDefaultPolicyXml() throws IOException {
        InputStream inStream = SecurityTokenServiceTemplateRegistry.class.getClassLoader().getResourceAsStream(DEFAULT_POLICY_FILE);
        byte[] bytes = new byte[65536];
        int len = inStream.read(bytes);
        return fixLines(new String(bytes, 0, len));
    }

    private String fixLines(String input) {
        return input.replaceAll("\\r\\n", "\n").replaceAll("\\n\\r", "\n").replaceAll("\\r", "\n");
    }
}
