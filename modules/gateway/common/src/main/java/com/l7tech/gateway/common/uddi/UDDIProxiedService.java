
package com.l7tech.gateway.common.uddi;

import org.hibernate.annotations.Proxy;

import javax.persistence.*;

import com.l7tech.objectmodel.imp.PersistentEntityImp;

/**
 * Copyright (C) 2008, Layer 7 Technologies Inc.
 *
 * Entity which represents an individual wsdl:service which was published as a proxy BusinessService to UDDI
 * 
 * @author darmstrong
 */
@Entity
@Proxy(lazy=false)
@Table(name="uddi_proxied_service")
public class UDDIProxiedService extends PersistentEntityImp {

    public UDDIProxiedService() {
    }

    public UDDIProxiedService(String uddiServiceKey, String uddiServiceName, String wsdlServiceName) {
        this.uddiServiceKey = uddiServiceKey;
        this.uddiServiceName = uddiServiceName;
        this.wsdlServiceName = wsdlServiceName;
    }


    /**
     * Used to determine if 'this' UDDIProxiedService has had a property modified which should not be modified once
     * the entity has been created based on application logic
     *
     * @param original UDDIProxiedService last known version of 'this' UDDIProxiedService used to compare what has
     * changed in 'this'
     */
    public void throwIfFinalPropertyModified(final UDDIProxiedService original){
        UDDIProxiedServiceInfo.testProperty("service key", this.getUddiServiceKey(), original.getUddiServiceKey());
        UDDIProxiedServiceInfo.testProperty("service name", this.getUddiServiceName(), original.getUddiServiceName());
        UDDIProxiedServiceInfo.testProperty("wsdl service name", this.getWsdlServiceName(), original.getWsdlServiceName());
    }

    @Override
    @Version
    @Column(name = "version")
    public int getVersion() {
        return super.getVersion();
    }
    
    @Column(name = "uddi_service_key", updatable = false)
    public String getUddiServiceKey() {
        return uddiServiceKey;
    }

    public void setUddiServiceKey(String uddiServiceKey) {
        this.uddiServiceKey = uddiServiceKey;
    }

    @Column(name = "uddi_service_name", updatable = false)
    public String getUddiServiceName() {
        return uddiServiceName;
    }

    public void setUddiServiceName(String uddiServiceName) {
        this.uddiServiceName = uddiServiceName;
    }

    @Column(name = "wsdl_service_name", updatable = false)    
    public String getWsdlServiceName() {
        return wsdlServiceName;
    }

    public void setWsdlServiceName(String wsdlServiceName) {
        this.wsdlServiceName = wsdlServiceName;
    }

    @ManyToOne(optional=false)
    @JoinColumn(name="uddi_proxied_service_info_oid", nullable=false)
    public UDDIProxiedServiceInfo getProxiedServiceInfo() {
        return proxiedServiceInfo;
    }

    public void setProxiedServiceInfo(UDDIProxiedServiceInfo proxiedServiceInfo) {
        this.proxiedServiceInfo = proxiedServiceInfo;
    }

    /**
     * Serivce key of the proxied business service in UDDI. Always created by the UDDI Registry
     */
    private String uddiServiceKey;

    /**
     * Name of the proxied business service in UDDI. This is a synthetic name generated automatically by the gateway
     */
    private String uddiServiceName;

    /**
     * wsdlServiceName represents the original unmodified unique wsdl:service name from the Gateway's WSDL
     */
    private String wsdlServiceName;

    /**
     * Parent which contains general information about the WSDL and BusinessEntity this individual service
     * is related to
     */
    private UDDIProxiedServiceInfo proxiedServiceInfo;
}
