package com.l7tech.gateway.common.uddi;

import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.imp.PersistentEntityImp;
import org.hibernate.annotations.Proxy;
import org.hibernate.annotations.Type;

import javax.persistence.*;

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
         * Like CANNOT_PUBLISH, all delete attempts have been tried. This is a final state.
         */
        CANNOT_DELETE
    }

    public UDDIPublishStatus() {
        this.publishStatus = PublishStatus.PUBLISH;
    }

    public UDDIPublishStatus(Goid uddiProxiedServiceInfoGoid, PublishStatus publishStatus) {
        this.publishStatus = publishStatus;
        this.uddiProxiedServiceInfoGoid = uddiProxiedServiceInfoGoid;
    }

    @Override
    @Version
    @Column(name = "version")
    public int getVersion() {
        return super.getVersion();
    }

    @Column(name="uddi_proxied_service_info_goid", nullable=false)
    @Type(type = "com.l7tech.server.util.GoidType")
    public Goid getUddiProxiedServiceInfoGoid() {
        return uddiProxiedServiceInfoGoid;
    }

    public void setUddiProxiedServiceInfoGoid(Goid uddiProxiedServiceInfoGoid) {
        this.uddiProxiedServiceInfoGoid = uddiProxiedServiceInfoGoid;
    }

    @Column(name = "publish_status")
    @Enumerated(EnumType.STRING)
    public PublishStatus getPublishStatus() {
        return publishStatus;
    }

    public void setPublishStatus(PublishStatus publishStatus) {
        this.publishStatus = publishStatus;

        //if the status is published, then reset the fail count, as the status may be reset to publish, in which case
        //we want to follow through with the same fail count logic
        if(publishStatus == PublishStatus.PUBLISHED){
            setFailCount(0);
        }
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

    private Goid uddiProxiedServiceInfoGoid;
    private PublishStatus publishStatus;
    private int failCount;
}
