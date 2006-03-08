package com.l7tech.server.systinet;

import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationContext;
import org.springframework.beans.BeansException;
import com.l7tech.server.ServerConfig;
import com.l7tech.objectmodel.FindException;
import com.l7tech.common.protocol.SecureSpanConstants;
import com.l7tech.common.util.KeystoreUtils;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.security.cert.CertificateException;

/**
 * server side component that acts as a systinet registry client publishing wsdl and service policies.
 * <p/>
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Mar 8, 2006<br/>
 */
public class RegistryPublicationManager implements ApplicationContextAware {
    private static final Logger logger = Logger.getLogger(RegistryPublicationManager.class.getName());
    private ApplicationContext applicationContext;
    private ServerConfig serverConfig;
    private KeystoreUtils keystoreUtils;

    public String publishServiceWSDLAndPolicy(String serviceid) throws FindException {
        // todo, the real thing
        return getExternalSSGWSDLURLForPublishedService(serviceid);
    }

    public RegistryPublicationManager() {}

    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    public void setServerConfig(ServerConfig serverConfig) {
        this.serverConfig = serverConfig;
    }

    public void setKeystore(KeystoreUtils keystore) {
        this.keystoreUtils = keystore;
    }

    private String getExternalSSGWSDLURLForPublishedService(String serviceoid) throws FindException {
        try {
            String hostname = keystoreUtils.getSslCert().getSubjectDN().getName();
            if (hostname.startsWith("CN=") || hostname.startsWith("cn=")) hostname = hostname.substring(3);
            hostname = hostname.trim();
            String port = serverConfig.getProperty("clusterhttpport");
            String uri = SecureSpanConstants.WSDL_PROXY_FILE;
            String query = SecureSpanConstants.HttpQueryParameters.PARAM_SERVICEOID + "=" + serviceoid;
            return "http://" + hostname + ":" + port + uri + "?" + query;
        } catch (IOException e) {
            logger.log(Level.WARNING, "cannot get hostname from ssl cert", e);
            throw new FindException("cannot get hostname from ssl cert", e);
        } catch (CertificateException e) {
            logger.log(Level.WARNING, "cannot get hostname from ssl cert", e);
            throw new FindException("cannot get hostname from ssl cert", e);
        }
    }
}
