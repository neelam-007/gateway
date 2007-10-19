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
import com.l7tech.common.uddi.PolicyAttachmentVersion;

/**
 * Agent implemented using the UDDIClient interface/factory.
 *
 * @author steve
 */
public class UDDIClientAgent implements UddiAgent {

    private int resultRowsMax;
    private int resultBatchSize; //TODO use this when querying UDDI as the max rows ...

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
        List<WsdlInfo> wsdlInfos = null;
        try {
            Collection<UDDINamedEntity> services =
                    uddi.listServiceWsdls(namePattern, caseSensitive, 0, resultRowsMax);
            wsdlInfos = toWsdlInfos(services);
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
    private List<WsdlInfo> toWsdlInfos(Collection<UDDINamedEntity> services) {
        List<WsdlInfo> wsdlList = new ArrayList(services.size());
        
        for ( UDDINamedEntity info : services ) {
            wsdlList.add(new WsdlInfo(info.getName(), info.getWsdlUrl()));    
        }

        return wsdlList;
    }
}
