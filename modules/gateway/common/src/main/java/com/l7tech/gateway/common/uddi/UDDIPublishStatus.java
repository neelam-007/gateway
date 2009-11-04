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

    //todo find out if there is a way to just get hibernate to save enum types
    public enum PublishStatus{
        PUBLISHING(0),
        PUBLISHED(1),
        DELETING(2);

        private int status;

        private PublishStatus(int status) {
            this.status = status;
        }

        public int getStatus() {
            return status;
        }

        /**
         * Convert the int into a PublishStatus.
         * @param status int represening the enum value
         * @return the PublishStatus which matches the type, or an IllegalStateException
         */
        public static PublishStatus findStatus(final int status){
            for(PublishStatus regType: values()){
                if(regType.getStatus() == status) return regType;
            }
            throw new IllegalStateException("Unknown publish status requested: " + status);
        }
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
    public int getPublishStatus() {
        return publishStatus.getStatus();
    }

    public void setPublishStatus(int publishStatus) {
        this.publishStatus = PublishStatus.findStatus(publishStatus);
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
