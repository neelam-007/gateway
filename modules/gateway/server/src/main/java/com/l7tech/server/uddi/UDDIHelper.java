package com.l7tech.server.uddi;

import com.l7tech.uddi.WsdlPortInfoImpl;
import com.l7tech.uddi.*;
import com.l7tech.gateway.common.uddi.UDDIRegistry;
import com.l7tech.server.ServerConfig;
import com.l7tech.common.protocol.SecureSpanConstants;

import java.util.Properties;
import java.util.List;
import java.util.ArrayList;
import java.util.Collection;

/**
 * UDDI utility methods.
 */
public class UDDIHelper {

    //- PUBLIC

    /**
     * Property key for UDDI Urls (use .1 .2 suffix)
     */
    public static final String PROP_INQUIRY_URLS = "uddi.url.inquiry";

    /**
     * Property key for the maximum rows to retrieve from the registry.
     */
    public static final String PROP_RESULT_ROWS_MAX = "uddi.result.max_rows";

    /**
     * Property key for the number of rows to retrieve from the registry with each call.
     */
    public static final String PROP_RESULT_BATCH_SIZE = "uddi.result.batch_size";

    public UDDIHelper( final ServerConfig serverConfig,
                       final Properties properties ) {
        this.serverConfig = serverConfig;

        String rowsMax = properties.getProperty(PROP_RESULT_ROWS_MAX, "100");     // default 100 rows max
        int resultRowsMax = Integer.parseInt(rowsMax);
        String batchSize = properties.getProperty(PROP_RESULT_BATCH_SIZE, "100");
        int resultBatchSize = Integer.parseInt(batchSize);

        if(resultBatchSize > resultRowsMax){
            resultBatchSize = resultRowsMax;
        }

        this.defaultResultBatchSize = resultBatchSize;
        this.defaultResultRowsMax = resultRowsMax;
    }

    /**
     * Get the WSDL download URL for the given service identifier (OID)
     *
     * @param serviceOid The oid for the service
     * @return The URL
     */
    public String getExternalWsdlUrlForService( final long serviceOid )  {
        String query = SecureSpanConstants.HttpQueryParameters.PARAM_SERVICEOID + "=" + serviceOid;
        return buildCompleteGatewayUrl(SecureSpanConstants.WSDL_PROXY_FILE) + "?" + query;
    }

    /**
     * Get the base WSDL URL
     *
     * @return String base WSDL URL
     */
    public String getBaseWsdlUrl(){
        return buildCompleteGatewayUrl(SecureSpanConstants.WSDL_PROXY_FILE);
    }

    /**
     * Get the query string portion of the WSDL URL
     *
     * @return String query portion of WSDL URL
     */
    public String getQueryWsdlString(){
        return SecureSpanConstants.WSDL_PROXY_FILE + "?" + SecureSpanConstants.HttpQueryParameters.PARAM_SERVICEOID;
    }

    public String getExternalPolicyUrlForService( final long serviceOid, final boolean fullPolicyURL ){
        String query = SecureSpanConstants.HttpQueryParameters.PARAM_SERVICEOID +
                "=" + serviceOid +
                "&fulldoc=" + ((fullPolicyURL) ? "yes" : "no")+ "&" +
                SecureSpanConstants.HttpQueryParameters.PARAM_INLINE + "=no";

        return buildCompleteGatewayUrl(SecureSpanConstants.POLICY_SERVICE_FILE) + "?" + query;
    }

    public String getExternalUrlForService( final long serviceOid ) {
        return buildCompleteGatewayUrl(SecureSpanConstants.SERVICE_FILE) + serviceOid;
    }

    public UDDINamedEntity[] getMatchingBusinesses( final UDDIClient uddiClient,
                                                    final String namePattern,
                                                    final boolean caseSensitive ) throws UDDIException {
        final List<UDDINamedEntity> uddiNamedEntities = new ArrayList<UDDINamedEntity>();
        final int resultRowsMax = getResultRowsMax();
        final int resultBatchSize = getResultBatchSize();

        for (int i=0; uddiNamedEntities.size() < resultRowsMax; i++) {
            int head = i*resultBatchSize;
            if (head > 0) head++; // one based

            Collection<UDDINamedEntity> businesses =
                    uddiClient.listBusinessEntities(namePattern, caseSensitive, head, resultBatchSize);

            uddiNamedEntities.addAll(businesses);

            if (!uddiClient.listMoreAvailable())
                break;
        }

        boolean maxedOutSearch = uddiNamedEntities.size()>=resultRowsMax || uddiClient.listMoreAvailable();

        if (maxedOutSearch) {
            uddiNamedEntities.subList(resultRowsMax, uddiNamedEntities.size()).clear();
            uddiNamedEntities.add( WsdlPortInfoImpl.MAXED_OUT_SEARCH_RESULT_ENTITY );
        }

        return uddiNamedEntities.toArray(new UDDINamedEntity[uddiNamedEntities.size()]);
    }

    public WsdlPortInfo[] getWsdlByServiceName(final UDDIClient uddiClient,
                                               final String namePattern,
                                               final boolean caseSensitive) throws UDDIException {
        // % denotes wildcard of string (any number of characters), underscore denotes wildcard of a single character

        final List<WsdlPortInfo> wsdlPortInfos = new ArrayList<WsdlPortInfo>();
        final int resultRowsMax = getResultRowsMax();
        final int resultBatchSize = getResultBatchSize();

        final String wsdlUrl = getBaseWsdlUrl();
        final String queryUrl = getQueryWsdlString();
        final String hostName = ServerConfig.getInstance().getHostname();
        final String hName;
        if(hostName.indexOf("[") != -1){
            hName = hostName.substring(0, hostName.indexOf("["));
        }else if(hostName.indexOf(".") != -1){
            hName = hostName.substring(0, hostName.indexOf("."));
        }else{
            hName = hostName;
        }

        for (int i = 0; wsdlPortInfos.size() < resultRowsMax; i++) {
            int head = i * resultBatchSize;
            if (head > 0) head++; // one based

            Collection<WsdlPortInfo> foundWsdlPortInfos =
                    uddiClient.listServiceWsdls(namePattern, caseSensitive, head, resultBatchSize);
            for(WsdlPortInfo wsdlPortInfo: foundWsdlPortInfos){
                final String wsdl = wsdlPortInfo.getWsdlUrl();
                if(wsdl.indexOf(wsdlUrl) != -1){
                    if(wsdlPortInfo instanceof WsdlPortInfoImpl){
                        WsdlPortInfoImpl impl = (WsdlPortInfoImpl) wsdlPortInfo;
                        impl.setGatewayWsdl(true);//ssm will not allow this wsdl to be used
                    }
                } else if(wsdl.indexOf(queryUrl) != -1){
                    if(wsdlPortInfo instanceof WsdlPortInfoImpl){
                        WsdlPortInfoImpl impl = (WsdlPortInfoImpl) wsdlPortInfo;
                        impl.setLikelyGatewayWsdl(true);//ssm will warn the user
                    }
                }else if(wsdl.indexOf(hName) != -1){
                    if(wsdlPortInfo instanceof WsdlPortInfoImpl){
                        WsdlPortInfoImpl impl = (WsdlPortInfoImpl) wsdlPortInfo;
                        impl.setLikelyGatewayWsdl(true);//ssm will warn the user
                    }
                }
            }

            wsdlPortInfos.addAll(foundWsdlPortInfos);

            if (!uddiClient.listMoreAvailable())
                break;
        }


        boolean maxedOutSearch = wsdlPortInfos.size() >= resultRowsMax || uddiClient.listMoreAvailable();

        if (maxedOutSearch) {
            wsdlPortInfos.subList(resultRowsMax, wsdlPortInfos.size()).clear();
            wsdlPortInfos.add(WsdlPortInfoImpl.MAXED_OUT_SEARCH_RESULT);
        }

        return wsdlPortInfos.toArray(new WsdlPortInfo[wsdlPortInfos.size()]);
    }

    /**
     * Create a UDDIClient for accessing the given registry.
     *
     * @param uddiRegistry The registry to access.
     * @return The UDDIClient
     */
    public static UDDIClient newUDDIClient( final UDDIRegistry uddiRegistry ) {
        UDDIClientFactory factory = UDDIClientFactory.getInstance();
        return factory.newUDDIClient( uddiRegistry.getInquiryUrl(),
                        uddiRegistry.getPublishUrl(), uddiRegistry.getSubscriptionUrl(), uddiRegistry.getSecurityUrl(),
                        uddiRegistry.getRegistryAccountUserName(), uddiRegistry.getRegistryAccountPassword(), null );
    }

    //- PRIVATE

    private final ServerConfig serverConfig;
    private final int defaultResultRowsMax;
    private final int defaultResultBatchSize;

    private int getResultRowsMax() {
        return serverConfig.getIntProperty( PROP_RESULT_ROWS_MAX, defaultResultRowsMax );
    }

    private int getResultBatchSize() {
        return serverConfig.getIntProperty( PROP_RESULT_BATCH_SIZE, defaultResultBatchSize );
    }

    private String buildCompleteGatewayUrl( final String relativeUri ) {
        return "http://" + getExternalHostName() + getPrefixedExternalPort() + relativeUri;
    }

    /**
     * Get the hostname from the clusterHost cluster property.
     *
     * <p>If it's not set, then the SSG's host name will be returned</p>
     *
     * @return String cluster hostname or hostname if the cluster property "clusterHost" is not set
     * @throws com.l7tech.objectmodel.FindException if the hostname cannot be found
     */
    private String getExternalHostName() {
        final String hostName;
        final String clusterHost = serverConfig.getProperty("clusterHost");
        if(clusterHost == null || clusterHost.trim().isEmpty()){
            hostName = serverConfig.getHostname();
        }else{
            hostName = clusterHost;
        }
        return hostName;
    }

    private String getPrefixedExternalPort() {
        String port = serverConfig.getProperty("clusterhttpport", "");
        if ( port.length() > 0 ) {
            port = ":" + port;
        }
        return port;
    }
}
