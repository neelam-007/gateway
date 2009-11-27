
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

    public UDDIProxiedService(String uddiServiceKey, String uddiServiceName, String wsdlServiceName, String wsdlServiceNamespace) {
        this.uddiServiceKey = uddiServiceKey;
        this.uddiServiceName = uddiServiceName;
        this.wsdlServiceName = wsdlServiceName;
        if(wsdlServiceNamespace == null){
            this.wsdlServiceNamespace = NULL_NAMESPACE;
        }else{
            this.wsdlServiceNamespace = wsdlServiceNamespace;
        }
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

    @Column(name = "wsdl_service_namespace", updatable = false)
    public String getWsdlServiceNamespace() {
        return wsdlServiceNamespace;
    }

    public void setWsdlServiceNamespace(String wsdlServiceNamespace) {
        this.wsdlServiceNamespace = wsdlServiceNamespace;
    }

    @ManyToOne(optional=false)
    @JoinColumn(name="uddi_proxied_service_info_oid", nullable=false)
    public UDDIProxiedServiceInfo getUddiProxiedServiceInfo() {
        return uddiProxiedServiceInfo;
    }

    public void setUddiProxiedServiceInfo(UDDIProxiedServiceInfo uddiProxiedServiceInfo) {
        this.uddiProxiedServiceInfo = uddiProxiedServiceInfo;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UDDIProxiedService)) return false;
        if (!super.equals(o)) return false;

        UDDIProxiedService that = (UDDIProxiedService) o;

        if (!uddiServiceKey.equals(that.uddiServiceKey)) return false;
        if (!uddiServiceName.equals(that.uddiServiceName)) return false;
        if (!wsdlServiceName.equals(that.wsdlServiceName)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + uddiServiceKey.hashCode();
        result = 31 * result + uddiServiceName.hashCode();
        result = 31 * result + wsdlServiceName.hashCode();
        return result;
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
     * Namespace of the service if any is defined. See NULL_NAMESPACE which represents a null namespace
     */
    private String wsdlServiceNamespace;

    /**
     * We cannot allow null values for wsdlServiceNamespace as the unique key in the database allows multiple null
     * values and it will renender the unique key useless
     */
    private static final String NULL_NAMESPACE = "NULL_NAMESPACE";

    /**
     * Parent which contains general information about the WSDL and BusinessEntity this individual service
     * is related to
     */
    private UDDIProxiedServiceInfo uddiProxiedServiceInfo;
}
