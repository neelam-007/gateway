package com.l7tech.server.uddi;

import com.l7tech.uddi.UDDINamedEntity;
import com.l7tech.uddi.UDDIClient;
import com.l7tech.uddi.UDDIException;
import com.l7tech.uddi.WsdlInfo;
import com.l7tech.util.Config;

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
            uddiNamedEntities.add( WsdlInfo.MAXED_OUT_SEARCH_RESULT_ENTITY );
        }

        return uddiNamedEntities.toArray(new UDDINamedEntity[uddiNamedEntities.size()]);
    }

    public WsdlInfo[] getWsdlByServiceName( final UDDIClient uddiClient,
                                            final String namePattern,
                                            final boolean caseSensitive ) throws UDDIException {
        // % denotes wildcard of string (any number of characters), underscore denotes wildcard of a single character

        final List<WsdlInfo> wsdlInfos = new ArrayList<WsdlInfo>();
        final int resultRowsMax = getResultRowsMax();
        final int resultBatchSize = getResultBatchSize();

        for (int i=0; wsdlInfos.size() < resultRowsMax; i++) {
            int head = i*resultBatchSize;
            if (head > 0) head++; // one based

            Collection<UDDINamedEntity> services =
                    uddiClient.listServiceWsdls(namePattern, caseSensitive, head, resultBatchSize);

            addWsdlInfos(wsdlInfos, services);

            if (!uddiClient.listMoreAvailable())
                break;
        }

        boolean maxedOutSearch = wsdlInfos.size()>=resultRowsMax || uddiClient.listMoreAvailable();

        if (maxedOutSearch) {
            wsdlInfos.subList(resultRowsMax, wsdlInfos.size()).clear();
            wsdlInfos.add( WsdlInfo.MAXED_OUT_SEARCH_RESULT );
        }

        return wsdlInfos.toArray(new WsdlInfo[wsdlInfos.size()]);
    }

    //- PRIVATE

    private final Config config;
    private final int defaultResultRowsMax;
    private final int defaultResultBatchSize;

    /**
     * Process the URLs of the given services.
     */
    private void addWsdlInfos( final List<WsdlInfo> wsdlList,
                               final Collection<UDDINamedEntity> services) {
        for ( UDDINamedEntity info : services ) {
            wsdlList.add(new WsdlInfo(info.getName(), info.getWsdlUrl()));
        }
    }

    private int getResultRowsMax() {
        return config.getIntProperty( PROP_RESULT_ROWS_MAX, defaultResultRowsMax );
    }

    private int getResultBatchSize() {
        return config.getIntProperty( PROP_RESULT_BATCH_SIZE, defaultResultBatchSize );
    }
}
