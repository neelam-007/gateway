package com.l7tech.server.service.uddi.impl.generic;

import java.util.Properties;
import java.util.Collection;
import java.util.ArrayList;
import java.util.List;

import com.l7tech.server.service.uddi.UddiAgent;
import com.l7tech.server.service.uddi.UddiAgentException;
import com.l7tech.common.uddi.WsdlInfo;
import com.l7tech.common.uddi.UDDIClientFactory;
import com.l7tech.common.uddi.UDDIClient;
import com.l7tech.common.uddi.UDDIException;
import com.l7tech.common.uddi.UDDINamedEntity;

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
    }

    /**
     * Get the WSDL info given the service name pattern.
     *
     * @param namePattern   the exact name or part of the name
     * @param caseSensitive  true if case sensitive, false otherwise.
     * @return WsdlInfo[] an array of WSDL info.
     * @throws com.l7tech.server.service.uddi.UddiAgentException   if there was a problem accessing the requested information.
     */
    public WsdlInfo[] getWsdlByServiceName(String inquiryUrl, String namePattern, boolean caseSensitive) throws UddiAgentException {
        checkInit();
        // % denotes wildcard of string (any number of characters), underscore denotes wildcard of a single character

        UDDIClient uddi = UDDIClientFactory.getInstance().newUDDIClient(inquiryUrl, null, null, null, null, null);
        List<WsdlInfo> wsdlInfos = new ArrayList();
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

        boolean maxedOutSearch = wsdlInfos.size()==resultRowsMax && uddi.listMoreAvailable();

        if (maxedOutSearch) {
            wsdlInfos.add( WsdlInfo.MAXED_OUT_SEARCH_RESULT );
        }

        return wsdlInfos.toArray(new WsdlInfo[wsdlInfos.size()]);
    }

    /**
     * Check if initialized.
     *
     * @throws com.l7tech.server.service.uddi.UddiAgentException if not initialized.
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
