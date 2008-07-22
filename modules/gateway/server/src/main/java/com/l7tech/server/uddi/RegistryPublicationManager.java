package com.l7tech.server.uddi;

import com.l7tech.server.ServerConfig;
import com.l7tech.server.KeystoreUtils;
import com.l7tech.common.protocol.SecureSpanConstants;

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
public class RegistryPublicationManager {
    private static final Logger logger = Logger.getLogger(RegistryPublicationManager.class.getName());
    private ServerConfig serverConfig;
    private KeystoreUtils keystoreUtils;
    private String myhostname;

    public String publishServiceWSDLAndPolicy(String serviceid) {
        // todo, the real thing
        return getExternalSSGWSDLURLForPublishedService(serviceid);
    }

    public RegistryPublicationManager() {}

    public void setServerConfig(ServerConfig serverConfig) {
        this.serverConfig = serverConfig;
    }

    public void setKeystore(KeystoreUtils keystore) {
        this.keystoreUtils = keystore;
    }

    private String getMyHostName() {
        // caching this. no reason this should change without a reboot anyway
        if (myhostname == null) {
            // most reliable hostname referencable from the outside is probably the subject of
            // the ssl cert for this server
            try {
                myhostname = keystoreUtils.getSslCert().getSubjectDN().getName();
            } catch (IOException e) {
                logger.log(Level.WARNING, "cannot get hostname from ssl cert", e);
                myhostname = serverConfig.getHostname();
            } catch (CertificateException e) {
                logger.log(Level.WARNING, "cannot get hostname from ssl cert", e);
                myhostname = serverConfig.getHostname();
            }
            if (myhostname.startsWith("CN=") || myhostname.startsWith("cn=")) myhostname = myhostname.substring(3);
            myhostname = myhostname.trim();
        }
        assert(myhostname != null);
        return myhostname;
    }

    private String getExternalSSGWSDLURLForPublishedService(String serviceoid) {
        String port = serverConfig.getPropertyCached("clusterhttpport");
        String uri = SecureSpanConstants.WSDL_PROXY_FILE;
        String query = SecureSpanConstants.HttpQueryParameters.PARAM_SERVICEOID + "=" + serviceoid;
        return "http://" + getMyHostName() + ":" + port + uri + "?" + query;
    }

    public String getExternalSSGPolicyURL(String serviceoid) {
        String port = serverConfig.getPropertyCached("clusterhttpport");
        String uri = SecureSpanConstants.POLICY_SERVICE_FILE;
        String query = SecureSpanConstants.HttpQueryParameters.PARAM_SERVICEOID + "=" + serviceoid + "&fulldoc=yes";
        return "http://" + getMyHostName() + ":" + port + uri + "?" + query;
    }

    public String getExternalSSGConsumptionURL(String serviceoid) {
        String port = serverConfig.getPropertyCached("clusterhttpport");
        String uri = SecureSpanConstants.SERVICE_FILE + serviceoid;
        return "http://" + getMyHostName() + ":" + port + uri;
    }
}
