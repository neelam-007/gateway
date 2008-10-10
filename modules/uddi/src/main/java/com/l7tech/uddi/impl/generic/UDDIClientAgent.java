package com.l7tech.uddi.impl.generic;

import java.util.Properties;
import java.util.Collection;
import java.util.ArrayList;
import java.util.List;

import com.l7tech.uddi.WsdlInfo;
import com.l7tech.uddi.UDDIClientFactory;
import com.l7tech.uddi.UDDIClient;
import com.l7tech.uddi.UDDIException;
import com.l7tech.uddi.UDDINamedEntity;
import com.l7tech.uddi.UDDIRegistryInfo;
import com.l7tech.uddi.UddiAgent;
import com.l7tech.uddi.UddiAgentException;

/**
 * Agent implemented using the UDDIClient interface/factory.
 *
 * @author steve
 */
public class UDDIClientAgent implements UddiAgent {

    private int resultRowsMax;
    private int resultBatchSize;

    /**
     * Constructor
     */
    public UDDIClientAgent() {
        resultRowsMax = -1;
        resultBatchSize = -1;
    }

    /**
     *
     * @param props The properties of the UDDI Agent.
     */
    public void init(Properties props) {
        if (resultRowsMax != -1) throw new IllegalStateException("already initialized");

        String rowsMax = props.getProperty(PROP_RESULT_ROWS_MAX, "100");     // default 100 rows max
        resultRowsMax = Integer.parseInt(rowsMax);
        String batchSize = props.getProperty(PROP_RESULT_BATCH_SIZE, "100");
        resultBatchSize = Integer.parseInt(batchSize);

        if(resultBatchSize > resultRowsMax){
            resultBatchSize = resultRowsMax;
        }
    }

    /**
     * Get the WSDL info given the service name pattern.
     *
     * @param namePattern The exact name or part of the name
     * @param info Type info for the UDDI Registry (optional if auth not present)
     * @param username The user account name (optional)
     * @param password The user account password (optional)
     * @param caseSensitive  true if case sensitive, false otherwise.
     * @return WsdlInfo[] an array of WSDL info.
     * @throws UddiAgentException   if there was a problem accessing the requested information.
     */
    public WsdlInfo[] getWsdlByServiceName(final String inquiryUrl,
                                           final UDDIRegistryInfo info,
                                           final String username,
                                           final char[] password,
                                           final String namePattern,
                                           final boolean caseSensitive) throws UddiAgentException {
        checkInit();
        // % denotes wildcard of string (any number of characters), underscore denotes wildcard of a single character

        String pwStr = password == null ? null : new String(password);
        UDDIClient uddi = info == null ?
                UDDIClientFactory.getInstance().newUDDIClient(inquiryUrl, null, null, username, pwStr, null) :
                UDDIClientFactory.getInstance().newUDDIClient(inquiryUrl, info, username, pwStr, null);
        List<WsdlInfo> wsdlInfos = new ArrayList<WsdlInfo>();
        try {
            for (int i=0; wsdlInfos.size() < resultRowsMax; i++) {
                int head = i*resultBatchSize;
                if (head > 0) head++; // one based

                Collection<UDDINamedEntity> services =
                        uddi.listServiceWsdls(namePattern, caseSensitive, head, resultBatchSize);

                addWsdlInfos(wsdlInfos, services);

                if (!uddi.listMoreAvailable())
                    break;
            }
        } catch (UDDIException iue) {
            throw new UddiAgentException(iue.getMessage(), iue);
        }

        boolean maxedOutSearch = wsdlInfos.size()>=resultRowsMax || uddi.listMoreAvailable();

        if (maxedOutSearch) {
            wsdlInfos.subList(resultRowsMax, wsdlInfos.size()).clear();
            wsdlInfos.add( WsdlInfo.MAXED_OUT_SEARCH_RESULT );
        }

        return wsdlInfos.toArray(new WsdlInfo[wsdlInfos.size()]);
    }

    /**
     * Check if initialized.
     *
     * @throws UddiAgentException if not initialized.
     */
    private void checkInit() throws UddiAgentException  {
        if (resultRowsMax == -1) throw new UddiAgentException("Not initialized.");
    }

    /**
     * Process the URLs of the given services.
     */
    private void addWsdlInfos(List<WsdlInfo> wsdlList, Collection<UDDINamedEntity> services) {
        for ( UDDINamedEntity info : services ) {
            wsdlList.add(new WsdlInfo(info.getName(), info.getWsdlUrl()));    
        }
    }
}
