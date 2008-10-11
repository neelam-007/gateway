package com.l7tech.gateway.config.client.beans;

import com.l7tech.server.management.api.node.NodeManagementApi;
import org.apache.cxf.configuration.jsse.TLSClientParameters;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.frontend.ClientProxyFactoryBean;
import org.apache.cxf.transport.http.HTTPConduit;

import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.net.URL;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

/**
 * ConfigurationBeanProvider that is backed by the Process Controller
 */
public abstract class ProcessControllerConfigurationBeanProvider implements ConfigurationBeanProvider {
    //- PUBLIC

    public ProcessControllerConfigurationBeanProvider( final URL nodeManagementUrl ) {
        this.nodeManagementUrl = nodeManagementUrl;
    }

    //- PRIVATE

    private final URL nodeManagementUrl;
    private NodeManagementApi managementService;

    protected NodeManagementApi getManagementService() {
        NodeManagementApi managementService = this.managementService;

        if ( managementService == null ) {
            ClientProxyFactoryBean factory = new ClientProxyFactoryBean();
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
}
