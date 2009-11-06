package com.l7tech.gateway.common.uddi;

import org.hibernate.annotations.Proxy;

import javax.persistence.*;

import com.l7tech.objectmodel.imp.PersistentEntityImp;

/**
 * Copyright (C) 2008, Layer 7 Technologies Inc.
 *
 * Runtime information for a UDDIProxiedServiceInfo regarding its status
 * @author darmstrong
 */
@Entity
@Proxy(lazy=false)
@Table(name="uddi_publish_status")
public class UDDIPublishStatus extends PersistentEntityImp {

    public enum PublishStatus{
        /**
         * The UDDI information is being published. This means it is either waiting or in the middle of it
         */
        PUBLISHING,
        /**
         * The UDDI information has been published.
         */
        PUBLISHED,
        /**
         * The UDDI information is being deleted. The only state afer this is nothing. If the UDDI data is not in
         * one of these states, it does not exist. i.e. there is no 'NONE' state
         */
        DELETING
    }

    public UDDIPublishStatus() {
    }

    public UDDIPublishStatus(final UDDIProxiedServiceInfo uddiProxiedServiceInfo) {
        this.uddiProxiedServiceInfo = uddiProxiedServiceInfo;
        this.publishStatus = PublishStatus.PUBLISHING;
        this.lastStatusChange = System.currentTimeMillis();
    }

    @Override
    @Version
    @Column(name = "version")
    public int getVersion() {
        return super.getVersion();
    }

    @ManyToOne(optional=false)
    @JoinColumn(name="uddi_proxied_service_info_oid", nullable=false)
    public UDDIProxiedServiceInfo getUddiProxiedServiceInfo() {
        return uddiProxiedServiceInfo;
    }

    public void setUddiProxiedServiceInfo(UDDIProxiedServiceInfo uddiProxiedServiceInfo) {
        this.uddiProxiedServiceInfo = uddiProxiedServiceInfo;
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

    private UDDIProxiedServiceInfo uddiProxiedServiceInfo;
    private PublishStatus publishStatus;
    private long lastStatusChange;
}
