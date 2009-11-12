package com.l7tech.gateway.common.uddi;

import org.hibernate.annotations.Proxy;

import javax.persistence.*;

import com.l7tech.objectmodel.imp.PersistentEntityImp;

/**
 * Copyright (C) 2008, Layer 7 Technologies Inc.
 *
 * Runtime information for a UDDIProxiedServiceInfo regarding its status
 *
 * This entiy can be made available via the admin api's but cannot be allowed to be saved by a user action. It is
 * read only outside of the gateway.
 * @author darmstrong
 */
@Entity
@Proxy(lazy=false)
@Table(name="uddi_publish_status")
public class UDDIPublishStatus extends PersistentEntityImp {

    /**
     * This enum is the life cycle of published proxied information in UDDI
     */
    public enum PublishStatus {
        /**
         * Publishing (or update) is required.
         */
        PUBLISH,

        /**
         * The last attempt to publish failed.
         */
        PUBLISH_FAILED,

        /**
         * Status when we cannot publish to UDDI. Final state
         */
        CANNOT_PUBLISH,

        /**
         * Published
         */
        PUBLISHED,

        /**
         * Deletion is required
         */
        DELETE,

        /**
         * Delete from UDDI failed. Like PUBLISH_FAILED, the delete can be reattempted
         */
        DELETE_FAILED,

        /**
         * Like CANNOT_PUBLISH, all delete attemps have been tried. This is a final state.
         */
        CANNOT_DELETE
    }

    public UDDIPublishStatus() {
        this.publishStatus = PublishStatus.PUBLISH;
    }

    public UDDIPublishStatus(long uddiProxiedServiceInfoOid, PublishStatus publishStatus) {
        this.publishStatus = publishStatus;
        this.uddiProxiedServiceInfoOid = uddiProxiedServiceInfoOid;
    }

    @Override
    @Version
    @Column(name = "version")
    public int getVersion() {
        return super.getVersion();
    }

    @Column(name="uddi_proxied_service_info_oid", nullable=false)
    public long getUddiProxiedServiceInfoOid() {
        return uddiProxiedServiceInfoOid;
    }

    public void setUddiProxiedServiceInfoOid(long uddiProxiedServiceInfoOid) {
        this.uddiProxiedServiceInfoOid = uddiProxiedServiceInfoOid;
    }

    @Column(name = "publish_status")
    @Enumerated(EnumType.STRING)
    public PublishStatus getPublishStatus() {
        return publishStatus;
    }

    public void setPublishStatus(PublishStatus publishStatus) {
        this.publishStatus = publishStatus;
    }

    @Column(name = "last_status_change")
    public long getLastStatusChange() {
        return lastStatusChange;
    }

    public void setLastStatusChange(long lastStatusChange) {
        this.lastStatusChange = lastStatusChange;
    }

    /**
     * When the status is PUBLISH_FAILED we will track how many attempts have been made
     * When we reach the maximum amount of attempts, we will stop trying and change the status to CANNOT_PUBLISH
     *
     * @return int the number of times the publish attempt has failed since the attempt to publish started
     */
    @Column(name = "fail_count")
    public int getFailCount() {
        return failCount;
    }

    public void setFailCount(int failCount) {
        this.failCount = failCount;
    }

    private long uddiProxiedServiceInfoOid;
    private PublishStatus publishStatus;
    private long lastStatusChange;
    private int failCount;
}
