package com.l7tech.server.tokenservice;

import com.l7tech.gateway.common.service.ServiceDocumentWsdlStrategy;
import com.l7tech.gateway.common.service.ServiceDocumentWsdlStrategy.ServiceDocumentResources;
import com.l7tech.gateway.common.service.ServiceTemplate;
import com.l7tech.gateway.common.service.ServiceType;
import com.l7tech.server.service.ServiceTemplateManager;
import com.l7tech.util.SoapConstants;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.HashMap;
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
        if ( serviceTemplate != null ) {
            logger.info("Registering the '" + serviceTemplate.getName() + "' service with the gateway (Routing URI = " + serviceTemplate.getDefaultUriPrefix() + ")");
            serviceTemplateManager.register(serviceTemplate);
        }
    }

    @Override
    public void destroy() throws Exception {
        if ( serviceTemplate != null ) {
            logger.info("Unregistering the '" + serviceTemplate.getName() + "' service with the gateway (Routing URI = " + serviceTemplate.getDefaultUriPrefix() + ")");
            serviceTemplateManager.unregister(serviceTemplate);
        }
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
            final String policyContents = getDefaultPolicyXml();
            final ServiceDocumentResources resources = ServiceDocumentWsdlStrategy.loadResources(
                    SecurityTokenServiceTemplateRegistry.class.getPackage().getName().replace( '.', '/' ) + "/resources/",
                    FAKE_URL_PREFIX,
                    wsTrustNS_WSDL.get(wsTrustNamespace),
                    SecurityTokenServiceTemplateRegistry.class.getClassLoader() );

            template = new ServiceTemplate(
                    "Security Token Service",
                    "/tokenservice",
                    resources.getContent(),
                    resources.getUri(),
                    policyContents,
                    resources.getDependencies(),
                    ServiceType.OTHER_INTERNAL_SERVICE,
                    null);
        } catch (IOException e) {
            logger.log(Level.WARNING, "Can't load WSDL and/or Policy XML; service template will not be available", e);
        } catch ( URISyntaxException e ) {
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
