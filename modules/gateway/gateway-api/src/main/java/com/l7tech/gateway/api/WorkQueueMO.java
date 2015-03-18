package com.l7tech.gateway.api;

import com.l7tech.gateway.api.impl.AccessorSupport;
import com.l7tech.gateway.api.impl.ElementExtendableAccessibleObject;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

/**
 * The CassandraConnectionMO managed object represents a Cassandra connection.
 *
 * <p>The Accessor for Cassandra connections supports read and write. Cassandra
 * connections can be accessed by name or identifier.</p>
 *
 * <p>Connection properties are passed to Cassandra.</p>

 * @see ManagedObjectFactory#createWorkQueueMO()
 */
@XmlRootElement(name = "WorkQueue")
@XmlType(name = "WorkQueueType",
        propOrder = {"name", "maxQueueSize", "threadPoolMax", "rejectPolicy", "extension", "extensions"})
@AccessorSupport.AccessibleResource(name = "WorkQueues")
public class WorkQueueMO extends ElementExtendableAccessibleObject {
    private String name;
    private int maxQueueSize;
    private int threadPoolMax;
    private String rejectPolicy;

    WorkQueueMO() {
    }

    /**
     * Get the name for the work queue (case insensitive, required)
     *
     * @return The name
     */
    @XmlElement(name = "Name", required = true)
    public String getName() {
        return name;
    }

    /**
     * Set the name for the work queue.
     *
     * @param name The name to use
     */
    public void setName(final String name) {
        this.name = name;
    }

    /**
     * Get the maximum work queue size
     *
     * @return The maximum queue size
     */
    @XmlElement(name = "MaxQueueSize", required = true)
    public int getMaxQueueSize() {
        return maxQueueSize;
    }

    /**
     * Set the maximum work queue size.
     *
     * @param maxQueueSize The maximum work queue size
     */
    public void setMaxQueueSize(final int maxQueueSize) {
        this.maxQueueSize = maxQueueSize;
    }

    /**
     * Get the maximum thread pool size for work queue.
     *
     * @return The maximum thread pool size for work queue.
     */
    @XmlElement(name = "ThreadPoolMax", required = true)
    public int getThreadPoolMax() {
        return threadPoolMax;
    }

    /**
     * Set the maximum thread pool size for work queue.
     *
     * @param threadPoolMax The maximum thread pool size for work queue.
     */
    public void setThreadPoolMax(final int threadPoolMax) {
        this.threadPoolMax = threadPoolMax;
    }

    /**
     * Get the rejection policy.
     *
     * @return The rejection policy.
     */
    @XmlElement(name = "RejectPolicy", required = true)
    public String getRejectPolicy() {
        return rejectPolicy;
    }

    /**
     * Set the rejection policy.
     *
     * @param rejectPolicy The rejection policy.
     */
    public void setRejectPolicy(final String rejectPolicy) {
        this.rejectPolicy = rejectPolicy;
    }
}
