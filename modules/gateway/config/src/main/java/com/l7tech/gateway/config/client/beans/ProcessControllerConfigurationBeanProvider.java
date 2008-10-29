package com.l7tech.gateway.config.client.beans;

import com.l7tech.server.management.api.node.NodeManagementApi;
import com.l7tech.objectmodel.FindException;

import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.xml.ws.soap.SOAPFaultException;
import java.net.URL;
import java.net.MalformedURLException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.cxf.endpoint.Client;
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.apache.cxf.transport.http.HTTPConduit;
import org.apache.cxf.configuration.jsse.TLSClientParameters;

/**
 * ConfigurationBeanProvider that is backed by the Process Controller
 */
public abstract class ProcessControllerConfigurationBeanProvider implements ConfigurationBeanProvider {

    //- PUBLIC

    public ProcessControllerConfigurationBeanProvider( final URL nodeManagementUrl ) {
        this.nodeManagementUrl = nodeManagementUrl;
    }

    public ProcessControllerConfigurationBeanProvider( final String nodeManagementUrl ) {
        try {
            this.nodeManagementUrl = new URL(nodeManagementUrl);
        } catch ( MalformedURLException murle ) {
            throw new IllegalArgumentException("Invalid URL '"+nodeManagementUrl+"'", murle);
        }
    }

    public boolean isValid() {
        boolean valid = false;

        NodeManagementApi managementService = getManagementService();
        try {
            managementService.listNodes();
            valid = true;
        } catch ( FindException fe ) {
            logger.log(Level.WARNING, "Error listing nodes", fe );
        } catch ( SOAPFaultException sf ) {
            logger.log(Level.WARNING, "Error listing nodes", sf );
        }

        return valid;
    }

    //- PROTECTED

    protected final Logger logger = Logger.getLogger( getClass().getName() );

    protected final static String DEFAULT_NODE_NAME = "default";

    protected NodeManagementApi getManagementService() {
        NodeManagementApi managementService = this.managementService;

        if ( managementService == null ) {
            JaxWsProxyFactoryBean factory = new JaxWsProxyFactoryBean();
            factory.setServiceClass(NodeManagementApi.class);
            factory.setAddress(nodeManagementUrl.toString());
            Client c = factory.getClientFactoryBean().create();
            HTTPConduit hc = (HTTPConduit)c.getConduit();
            hc.setTlsClientParameters(new TLSClientParameters() {
                @Override
                public TrustManager[] getTrustManagers() {
                    return new TrustManager[] { new X509TrustManager() {
                        public void checkClientTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {}
                        public void checkServerTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {}
                        public X509Certificate[] getAcceptedIssuers() {return new X509Certificate[0];}
                    }};
                }

                @Override
                public boolean isDisableCNCheck() {
                    return true;
                }
            });
            managementService = (NodeManagementApi) factory.create();
            this.managementService = managementService;
         }

        return managementService;
    }    

    //- PRIVATE

    private final URL nodeManagementUrl;
    private NodeManagementApi managementService;
}
