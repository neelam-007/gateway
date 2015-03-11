package com.l7tech.gateway.common.cluster;

import com.l7tech.gateway.common.service.ServiceStatistics;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.NameableEntity;
import org.hibernate.annotations.Proxy;
import org.hibernate.annotations.Type;

import javax.persistence.*;
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
@Entity
@Proxy(lazy=false)
@Table(name="service_usage")
@IdClass(ServiceUsage.ServiceUsagePK.class)
public class ServiceUsage implements NameableEntity, Serializable {
    public static final String ATTR_SERVICE_GOID = "serviceid";

    /**
     * Util method to go from ServiceStatistics to ServiceUsage
     * @param stat
     * @param nodeid
     */
    public static ServiceUsage fromStat(ServiceStatistics stat, String nodeid) {
        ServiceUsage output = new ServiceUsage();
        output.setServiceid(stat.getServiceGoid());
        output.setNodeid(nodeid);
        output.setAuthorized(stat.getAuthorizedRequestCount());
        output.setCompleted(stat.getCompletedRequestCount());
        output.setRequests(stat.getAttemptedRequestCount());
        return output;
    }

    /**
     * id of the service this stat is applicable to
     */
    @Id
    @Column(name="serviceid")
    @Type(type = "com.l7tech.server.util.GoidType")
    public Goid getServiceid() {
        return serviceid;
    }

    /**
     * id of the service this stat is applicable to
     */
    public void setServiceid(Goid serviceid) {
        this.serviceid = serviceid;
    }

    /**
     * id of the node this stat is applicable to
     */
    @Id
    @Column(name="nodeid")
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
    @Column(name="requestnr")
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
    @Column(name="authorizedreqnr")
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
    @Column(name="completedreqnr")
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
     * Overridden so that it won't be registered as an @RbacAttribute
     */
    @Transient
    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    @Transient
    public String getId() {
        return nodeid + ":" + serviceid;
    }

    /**
     * this must be overriden (hibernate requirement for composite id classes)
     */
    public boolean equals(Object obj) {
        if (!(obj instanceof ServiceUsage)) return false;
        ServiceUsage jghmx = (ServiceUsage)obj;
        if (!Goid.equals(serviceid, jghmx.serviceid)) return false;
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
        int result = super.hashCode();
        result = 31 * result + (serviceid != null ? serviceid.hashCode() : 0);
        result = 31 * result + (nodeid != null ? nodeid.hashCode() : 0);
        return result;
    }

    private static final long serialVersionUID = 7989882004981151855L;

    private Goid serviceid;
    private String nodeid;
    private String name;
    private long requests;
    private long authorized;
    private long completed;

    public static class ServiceUsagePK implements Serializable {
        private Goid serviceid;
        private String nodeid;

        public ServiceUsagePK() {}

        public ServiceUsagePK(Goid serviceid, String nodeid) {
            this.serviceid = serviceid;
            this.nodeid = nodeid;
        }

        public Goid getServiceid() {
            return serviceid;
        }

        public void setServiceid(Goid serviceid) {
            this.serviceid = serviceid;
        }

        public String getNodeid() {
            return nodeid;
        }

        public void setNodeid(String nodeid) {
            this.nodeid = nodeid;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            ServiceUsagePK that = (ServiceUsagePK) o;

            if (nodeid != null ? !nodeid.equals(that.nodeid) : that.nodeid != null) return false;
            if (serviceid != null ? !serviceid.equals(that.serviceid) : that.serviceid != null) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = serviceid != null ? serviceid.hashCode() : 0;
            result = 31 * result + (nodeid != null ? nodeid.hashCode() : 0);
            return result;
        }
    }
}
