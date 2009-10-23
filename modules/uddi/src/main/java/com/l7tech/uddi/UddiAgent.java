package com.l7tech.uddi;

import java.util.Properties;

import com.l7tech.uddi.WsdlInfo;
import com.l7tech.uddi.UDDIRegistryInfo;

/**
 * For UDDI agent implementations.
 *
 * @author Steve Jones, $Author$
 * @version $Revision$
 */
public interface UddiAgent {

    /**
     * Property key for UDDI Urls (use .1 .2 suffix)
     */
    static final String PROP_INQUIRY_URLS = "uddi.url.inquiry";

    /**
     * Property key for the maximum rows to retrieve from the registry.
     */
    static final String PROP_RESULT_ROWS_MAX = "uddi.result.max_rows";

    /**
     * Property key for the number of rows to retrieve from the registry with each call.
     */
    static final String PROP_RESULT_BATCH_SIZE = "uddi.result.batch_size";

    /**
     * Initialize the agent with the given properties.
     *
     * <p>This should be called only once for any agent instance.</p>
     *
     * @param props The properties to use.
     * @see #PROP_RESULT_ROWS_MAX
     * @see #PROP_RESULT_BATCH_SIZE
     */
    void init(Properties props);

    /**
     * Query the given registry for WSDL information with the given parameters.
     *
     * @param uddiClient    UDDIClient to query registry with
     * @param namePattern   The wsdl pattern
     * @param caseSensitive Case sensitivity flag
     * @return The array of WsdlInfo objects (not null)
     * @throws UddiAgentException if an error occurs.
     */
    WsdlInfo[] getWsdlByServiceName(UDDIClient uddiClient,
                                    String namePattern,
                                    boolean caseSensitive) throws UddiAgentException;

    /**
     * Query the given registry for BusinessService information with the given parameters.
     *
     * @param uddiClient    UDDIClient to query registry with
     * @param namePattern   The business name pattern
     * @param caseSensitive Case sensitivity flag
     * @return The array of WsdlInfo objects (not null)
     * @throws UddiAgentException if an error occurs.
     */
    UDDINamedEntity[] getMatchingBusinesses(UDDIClient uddiClient,
                                    String namePattern,
                                    boolean caseSensitive) throws UddiAgentException;

}
