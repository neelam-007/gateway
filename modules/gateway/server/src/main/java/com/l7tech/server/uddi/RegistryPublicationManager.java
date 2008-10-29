package com.l7tech.server.uddi;

import com.l7tech.common.protocol.SecureSpanConstants;
import com.l7tech.server.DefaultKey;
import com.l7tech.server.ServerConfig;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

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
    private DefaultKey defaultKey;
    private String myhostname;

    public String publishServiceWSDLAndPolicy(String serviceid) {
        // todo, the real thing
        return getExternalSSGWSDLURLForPublishedService(serviceid);
    }

    public RegistryPublicationManager() {}

    public void setServerConfig(ServerConfig serverConfig) {
        this.serverConfig = serverConfig;
    }

    public void setKeystore(DefaultKey keystore) {
        this.defaultKey = keystore;
    }

    private String getMyHostName() {        
        return serverConfig.getPropertyCached("clusterHost");
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
        String query = SecureSpanConstants.HttpQueryParameters.PARAM_SERVICEOID + "=" + serviceoid + "&fulldoc=yes" + "&" +
                SecureSpanConstants.HttpQueryParameters.PARAM_INLINE + "=no";
        return "http://" + getMyHostName() + ":" + port + uri + "?" + query;
    }

    public String getExternalSSGConsumptionURL(String serviceoid) {
        String port = serverConfig.getPropertyCached("clusterhttpport");
        String uri = SecureSpanConstants.SERVICE_FILE + serviceoid;
        return "http://" + getMyHostName() + ":" + port + uri;
    }
}
