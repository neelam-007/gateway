package com.l7tech.server.uddi;

import com.l7tech.uddi.WsdlPortInfoImpl;
import com.l7tech.uddi.*;
import com.l7tech.util.Config;
import com.l7tech.gateway.common.uddi.UDDIRegistry;
import com.l7tech.server.ServerConfig;

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

    public UDDIHelper( final Config config,
                       final Properties properties ) {
        this.config = config;

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

        ExternalGatewayURLManager gatewayURLManager = new ExternalGatewayURLManager(ServerConfig.getInstance());
        final String wsdlUrl = gatewayURLManager.getBaseWsdlUrl();
        final String queryUrl = gatewayURLManager.getQueryWsdlString();
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

        return wsdlPortInfos.toArray(new WsdlPortInfoImpl[wsdlPortInfos.size()]);
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

    private final Config config;
    private final int defaultResultRowsMax;
    private final int defaultResultBatchSize;

    /**
     * Process the URLs of the given services.
     */
    private void addWsdlInfos( final List<WsdlPortInfo> wsdlListInfo,
                               final Collection<UDDINamedEntity> services) {
        for ( UDDINamedEntity info : services ) {
            wsdlListInfo.add(new WsdlPortInfoImpl(info.getName(), info.getWsdlUrl()));
        }
    }

    private int getResultRowsMax() {
        return config.getIntProperty( PROP_RESULT_ROWS_MAX, defaultResultRowsMax );
    }

    private int getResultBatchSize() {
        return config.getIntProperty( PROP_RESULT_BATCH_SIZE, defaultResultBatchSize );
    }
}
