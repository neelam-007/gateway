package com.l7tech.cluster;

import com.l7tech.service.ServiceStatistics;

import java.io.Serializable;

/**
 * Bean representation of a row in the service_usage table.
 *
 * This table is used to record usage statistics for a particular service on a particular node.
 *
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Dec 22, 2003<br/>
 * $Id$<br/>
 *
 */
public class ServiceUsage implements Serializable {

    /**
     * Util method to go from ServiceStatistics to ServiceUsage
     * @param stat
     * @param nodeid
     * @return
     */
    public static ServiceUsage fromStat(ServiceStatistics stat, String nodeid) {
        ServiceUsage output = new ServiceUsage();
        output.setServiceid(stat.getServiceOid());
        output.setNodeid(nodeid);
        output.setAuthorized(stat.getAuthorizedRequestCount());
        output.setCompleted(stat.getCompletedRequestCount());
        output.setRequests(stat.getAttemptedRequestCount());
        return output;
    }

    /**
     * id of the service this stat is applicable to
     */
    public long getServiceid() {
        return serviceid;
    }

    /**
     * id of the service this stat is applicable to
     */
    public void setServiceid(long serviceid) {
        this.serviceid = serviceid;
    }

    /**
     * id of the node this stat is applicable to
     */
    public String getNodeid() {
        return nodeid;
    }

    /**
     * id of the node this stat is applicable to
     */
    public void setNodeid(String nodeid) {
        this.nodeid = nodeid;
    }

    /**
     * the total number of requests for this service handled for this service
     */
    public long getRequests() {
        return requests;
    }

    /**
     * the total number of requests for this service handled for this service
     */
    public void setRequests(long requests) {
        this.requests = requests;
    }

    /**
     * the number of requests that were not subject to an authorization failure
     */
    public long getAuthorized() {
        return authorized;
    }

    /**
     * the number of requests that were not subject to an authorization failure
     */
    public void setAuthorized(long authorized) {
        this.authorized = authorized;
    }

    /**
     * the number of requests that were completly processed by the ssg without any error
     */
    public long getCompleted() {
        return completed;
    }

    /**
     * the number of requests that were completly processed by the ssg without any error
     */
    public void setCompleted(long completed) {
        this.completed = completed;
    }

    /**
     * Get the service name
     * @return
     */
    public String getServiceName() {
        return serviceName;
    }

    /**
     * Set the service name
     * @param name
     */
    public void setServiceName(String name) {
        serviceName = name;
    }
    /**
     * this must be overriden (hibernate requirement for composite id classes)
     */
    public boolean equals(Object obj) {
        if (!(obj instanceof ServiceUsage)) return false;
        ServiceUsage jghmx = (ServiceUsage)obj;
        if (serviceid != jghmx.serviceid) return false;
        if (!nodeid.equals(jghmx.nodeid)) return false;
        /*if (requests != jghmx.requests) return false;
        if (authorized != jghmx.authorized) return false;
        if (completed != jghmx.completed) return false;
        if (serviceName != jghmx.serviceName) return false;*/
        return true;
    }

    /**
     * this must be overriden (hibernate requirement for composite id classes)
     */
    public int hashCode() {
	return com.l7tech.common.util.HashCode.compute(serviceid, nodeid);
    }

    private long serviceid;
    private String nodeid;
    private long requests;
    private long authorized;
    private long completed;
    private String serviceName;
}
