package com.l7tech.server.uddi;

import com.l7tech.common.protocol.SecureSpanConstants;
import com.l7tech.server.ServerConfig;
import com.l7tech.objectmodel.FindException;

import java.util.logging.Level;
import java.util.logging.Logger;
import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Provide external facing Gateway URL for client which require this information
 * <p/>
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * @author flascell
 * @author darmstrong
 *
 */
public class ExternalGatewayURLManager {
    private static final Logger logger = Logger.getLogger(ExternalGatewayURLManager.class.getName());
    private final ServerConfig serverConfig;

    public ExternalGatewayURLManager(ServerConfig serverConfig) {
        this.serverConfig = serverConfig;
    }

//    public String getHostNameOnly() throws FindException {
//        final String hostName = getHostName();
//        final int firstIndex = hostName.indexOf(".");
//        return hostName.substring(0, (firstIndex == -1)? hostName.length(): firstIndex);
//    }
    
    public String getExternalWsdlUrlForService(String serviceoid)  {
        String query = SecureSpanConstants.HttpQueryParameters.PARAM_SERVICEOID + "=" + serviceoid;
        return getCompleteGatewayURL(SecureSpanConstants.WSDL_PROXY_FILE) + "?" + query;
    }

    /**
     * Get the base WSDL URL
     * @return String base WSDL URL
     */
    public String getBaseWsdlUrl(){
        return getCompleteGatewayURL(SecureSpanConstants.WSDL_PROXY_FILE);
    }

    /**
     * Get the query string portion of the WSDL URL
     * @return String query portion of WSDL URL
     */
    public String getQueryWsdlString(){
        return SecureSpanConstants.WSDL_PROXY_FILE + "?" + SecureSpanConstants.HttpQueryParameters.PARAM_SERVICEOID;
    }

    public String getExternalSSGPolicyURL(String serviceoid, final boolean fullPolicyURL){
        String query = SecureSpanConstants.HttpQueryParameters.PARAM_SERVICEOID +
                "=" + serviceoid +
                "&fulldoc=" + ((fullPolicyURL) ? "yes" : "no")+ "&" +
                SecureSpanConstants.HttpQueryParameters.PARAM_INLINE + "=no";

        return getCompleteGatewayURL(SecureSpanConstants.POLICY_SERVICE_FILE) + "?" + query;
    }

    public String getExternalSsgURLForService(String serviceoid) throws FindException {
        return getCompleteGatewayURL(SecureSpanConstants.SERVICE_FILE) + serviceoid;
    }

    private String getCompleteGatewayURL(String relativeUri) {
        return "http://" + getHostName() + ":" + getPortNumber() + relativeUri;
    }
    /**
     * Get the hostname from the clusterHost clsuter property. If it's not set, then the SSG's host name will
     * be returned
     * @return String cluster hostname or hostname if the cluster property "clusterHost" is not set
     * @throws FindException if the hostname cannot be found
     */
    private String getHostName() {
        final String hostName;
        final String clusterHost = serverConfig.getPropertyCached("clusterHost");
        if(clusterHost == null || clusterHost.trim().isEmpty()){
            hostName = serverConfig.getHostname();
        }else{
            hostName = clusterHost;
        }
        return hostName;
    }

    /**
     * Get the cluster property configured port number. Has a default which can be used.
     * @return
     */
    private String getPortNumber(){
        return serverConfig.getPropertyCached("clusterhttpport");
    }
}
