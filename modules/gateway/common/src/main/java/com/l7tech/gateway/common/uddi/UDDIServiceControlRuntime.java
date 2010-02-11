/**
 * Copyright (C) 2008, Layer 7 Technologies Inc.
 * @author darmstrong
 */
package com.l7tech.gateway.common.uddi;

import com.l7tech.objectmodel.imp.PersistentEntityImp;

import javax.persistence.Version;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

import org.hibernate.annotations.Proxy;

/**
 * Represents the runtime information for a UDDI BusinessService which we are monitoring.
 * Stores the last time it was modified. This helps us processing notifications, so we can ignore duplicates
 */
@Entity
@Proxy(lazy=false)
@Table(name="uddi_service_control_monitor_runtime")
public class UDDIServiceControlRuntime extends PersistentEntityImp{

    // - PUBLIC

    public UDDIServiceControlRuntime() {
    }

    /**
     *
     * @param accessPointURL String must not be null or empty.
     */
    public UDDIServiceControlRuntime(final long uddiServiceControlOid,
                                     final long lastUDDIModifiedTimeStamp,
                                     final String accessPointURL) {
        if(accessPointURL == null || accessPointURL.trim().isEmpty()) throw new IllegalArgumentException("accessPoint must not be null or empty");
        this.accessPointURL = accessPointURL;
        
        this.uddiServiceControlOid = uddiServiceControlOid;
        this.lastUDDIModifiedTimeStamp = lastUDDIModifiedTimeStamp;
    }

    @Column(name = "uddi_service_control_oid")
    public long getUddiServiceControlOid() {
        return uddiServiceControlOid;
    }

    public void setUddiServiceControlOid(long uddiServiceControlOid) {
        this.uddiServiceControlOid = uddiServiceControlOid;
    }

    /**
     * This is never a timestamp generated from the SSG. It is always a timestamp reported from the UDDI Registry
     * This information is found from the operation info for a BusinessService
     */
    @Column(name = "last_uddi_modified_timestamp")
    public long getLastUDDIModifiedTimeStamp() {
        return lastUDDIModifiedTimeStamp;
    }

    public void setLastUDDIModifiedTimeStamp(long lastUDDIModifiedTimeStamp) {
        this.lastUDDIModifiedTimeStamp = lastUDDIModifiedTimeStamp;
    }

    @Override
    @Version
    @Column(name = "version")
    public int getVersion() {
        return super.getVersion();
    }

    @Column(name = "access_point_url")
    public String getAccessPointURL() {
        return accessPointURL;
    }

    public void setAccessPointURL(String accessPointURL) {
        this.accessPointURL = accessPointURL;
    }
    
    // - PRIVATE

    private long uddiServiceControlOid;
    private long lastUDDIModifiedTimeStamp;
    private String accessPointURL;
}
