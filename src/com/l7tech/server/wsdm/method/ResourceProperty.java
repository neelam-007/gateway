package com.l7tech.server.wsdm.method;

import com.l7tech.server.wsdm.faults.InvalidResourcePropertyQNameFault;

import java.util.logging.Logger;

/**
 * Abstraction for the supported values of wsrf-rp:ResourceProperty
 * <p/>
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Nov 2, 2007<br/>
 */
public class ResourceProperty {
    public static ResourceProperty RESOURCE_ID                  = new ResourceProperty("ResourceId", true);
    public static ResourceProperty CURRENT_TIME                 = new ResourceProperty("CurrentTime", false);
    public static ResourceProperty OPERATIONAL_STATUS           = new ResourceProperty("OperationalStatus", true);
    public static ResourceProperty NUMBER_OF_REQUESTS           = new ResourceProperty("NumberOfRequests", true);
    public static ResourceProperty NUMBER_OF_FAILEDREQUESTS     = new ResourceProperty("NumberOfFailedRequests", true);
    public static ResourceProperty NUMBER_OF_SUCCESSFULREQUESTS = new ResourceProperty("NumberOfSuccessfulRequests", true);
    public static ResourceProperty SERVICE_TIME                 = new ResourceProperty("ServiceTime", true);
    public static ResourceProperty MAX_RESPONSETIME             = new ResourceProperty("MaxResponseTime", true);
    public static ResourceProperty LAST_RESPONSETIME            = new ResourceProperty("LastResponseTime", true);
    public static ResourceProperty THROUGHPUT                   = new ResourceProperty("Throughput", true);
    public static ResourceProperty FAULTRATE                    = new ResourceProperty("FaultRate", true);
    public static ResourceProperty AVG_RESPONSETIME             = new ResourceProperty("AvgResponseTime", true);

    public static ResourceProperty MANAGEABILITY_CAPABILITY     = new ResourceProperty("ManageabilityCapability", false);
    public static ResourceProperty TOPIC                        = new ResourceProperty("Topic", false);

    public static ResourceProperty fromValue(String value) throws InvalidResourcePropertyQNameFault {
        if (value == null) return null;
        if (value.contains(RESOURCE_ID.val)) return RESOURCE_ID;
        if (value.contains(CURRENT_TIME.val)) return CURRENT_TIME;
        if (value.contains(OPERATIONAL_STATUS.val)) return OPERATIONAL_STATUS;
        if (value.contains(NUMBER_OF_REQUESTS.val)) return NUMBER_OF_REQUESTS;
        if (value.contains(NUMBER_OF_FAILEDREQUESTS.val)) return NUMBER_OF_FAILEDREQUESTS;
        if (value.contains(NUMBER_OF_SUCCESSFULREQUESTS.val)) return NUMBER_OF_SUCCESSFULREQUESTS;
        if (value.contains(SERVICE_TIME.val)) return SERVICE_TIME;
        if (value.contains(MAX_RESPONSETIME.val)) return MAX_RESPONSETIME;
        if (value.contains(LAST_RESPONSETIME.val)) return LAST_RESPONSETIME;
        if (value.contains(THROUGHPUT.val)) return THROUGHPUT;
        if (value.contains(FAULTRATE.val)) return FAULTRATE;
        if (value.contains(AVG_RESPONSETIME.val)) return AVG_RESPONSETIME;
        if (value.contains(MANAGEABILITY_CAPABILITY.val)) return MANAGEABILITY_CAPABILITY;
        if (value.contains(TOPIC.val)) return TOPIC;

        logger.warning("Could not identify resource property of value not supported: " + value);
        throw new InvalidResourcePropertyQNameFault("Resource property name not recognized " + value);
    }

    private static Logger logger = Logger.getLogger(ResourceProperty.class.getName());

    private ResourceProperty(String val, boolean serviceSpecific) {
        this.val = val;
        this.serviceSpecific = serviceSpecific;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ResourceProperty that = (ResourceProperty) o;
        return !(val != null ? !val.equals(that.val) : that.val != null);
    }

    public int hashCode() {
        return (val != null ? val.hashCode() : 0);
    }

    private String val;
    private boolean serviceSpecific;

    public boolean serviceSpecific() {
        return serviceSpecific;
    }
}
