package com.l7tech.gateway.common.workqueue;

import com.l7tech.objectmodel.imp.ZoneableNamedEntityImp;
import com.l7tech.security.rbac.RbacAttribute;
import org.hibernate.annotations.Proxy;
import org.jetbrains.annotations.Nullable;

import javax.persistence.*;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;


/**
 * Work Queue entity class that stores work queue properties.
 */

@XmlAccessorType(XmlAccessType.PROPERTY)
@XmlRootElement
@Entity
@Proxy(lazy = false)
@Table(name = "work_queue")
public class WorkQueue extends ZoneableNamedEntityImp implements Comparable {
    public static final String REJECT_POLICY_FAIL_IMMEDIATELY = "FAIL_IMMEDIATELY";
    public static final String REJECT_POLICY_WAIT_FOR_ROOM = "WAIT_FOR_ROOM";

    private int maxQueueSize;
    private int threadPoolMax;
    private String rejectPolicy = REJECT_POLICY_WAIT_FOR_ROOM;

    public WorkQueue() {
        _name = "";
    }

    @RbacAttribute
    @Size(min = 1, max = 128)
    @Override
    @Transient
    public String getName() {
        return super.getName();
    }

    @NotNull
    @Min(1)
    @Max(1000000)
    @Column(name = "max_queue_size", nullable = false)
    public int getMaxQueueSize() {
        return maxQueueSize;
    }

    public void setMaxQueueSize(int maxQueueSize) {
        this.maxQueueSize = maxQueueSize;
    }

    @NotNull
    @Min(1)
    @Max(10000)
    @Column(name = "thread_pool_max", nullable = false)
    public int getThreadPoolMax() {
        return threadPoolMax;
    }

    public void setThreadPoolMax(int threadPoolMax) {
        this.threadPoolMax = threadPoolMax;
    }

    @RbacAttribute
    @NotNull
    @Size(min = 1, max = 255)
    @Column(name = "reject_policy", nullable = false)
    public String getRejectPolicy() {
        return rejectPolicy;
    }

    public void setRejectPolicy(String rejectPolicy) {
        this.rejectPolicy = rejectPolicy;
    }

    public void copyFrom(WorkQueue other) {
        this.setGoid(other.getGoid());
        this.setName(other.getName());
        this.setMaxQueueSize(other.getMaxQueueSize());
        this.setThreadPoolMax(other.getThreadPoolMax());
        this.setRejectPolicy(other.getRejectPolicy());
        this.setSecurityZone(other.getSecurityZone());
    }

    @Override
    public int compareTo(@Nullable Object o) {
        if (o == null || !(o instanceof WorkQueue))
            throw new IllegalArgumentException("The compared object must be a WorkQueue.");

        String originalConfigurationName = getName();
        String comparedConfigurationName = ((WorkQueue) o).getName();

        if (originalConfigurationName == null || comparedConfigurationName == null)
            throw new NullPointerException("WorkQueue name must not be null.");
        return originalConfigurationName.compareToIgnoreCase(comparedConfigurationName);
    }
}
