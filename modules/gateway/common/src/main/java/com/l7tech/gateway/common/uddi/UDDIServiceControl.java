package com.l7tech.gateway.common.uddi;

import com.l7tech.objectmodel.imp.PersistentEntityImp;

import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.Version;
import javax.persistence.Column;

import org.hibernate.annotations.Proxy;

/**
 * Represents the UDDI location from which a published service was created.
 */
@Entity
@Proxy(lazy=false)
@Table(name="uddi_service_control")
public class UDDIServiceControl extends PersistentEntityImp {

    //- PUBLIC

    @Column(name = "published_service_oid")
    public long getPublishedServiceOid() {
        return publishedServiceOid;
    }

    public void setPublishedServiceOid( final long publishedServiceOid ) {
        this.publishedServiceOid = publishedServiceOid;
    }

    @Column(name = "uddi_registry_oid")
    public long getUddiRegistryOid() {
        return uddiRegistryOid;
    }

    public void setUddiRegistryOid( final long uddiRegistryOid ) {
        this.uddiRegistryOid = uddiRegistryOid;
    }

    @Column(name = "uddi_business_key")
    public String getUddiBusinessKey() {
        return uddiBusinessKey;
    }

    public void setUddiBusinessKey( final String uddiBusinessKey ) {
        this.uddiBusinessKey = uddiBusinessKey;
    }

    @Column(name = "uddi_business_name")
    public String getUddiBusinessName() {
        return uddiBusinessName;
    }

    public void setUddiBusinessName( final String uddiBusinessName ) {
        this.uddiBusinessName = uddiBusinessName;
    }

    @Column(name = "uddi_service_key")
    public String getUddiServiceKey() {
        return uddiServiceKey;
    }

    public void setUddiServiceKey( final String uddiServiceKey ) {
        this.uddiServiceKey = uddiServiceKey;
    }

    @Column(name = "uddi_service_name")
    public String getUddiServiceName() {
        return uddiServiceName;
    }

    public void setUddiServiceName( final String uddiServiceName ) {
        this.uddiServiceName = uddiServiceName;
    }

    @Column(name = "wsdl_service_name")
    public String getWsdlServiceName() {
        return wsdlServiceName;
    }

    public void setWsdlServiceName( final String wsdlServiceName ) {
        this.wsdlServiceName = wsdlServiceName;
    }

    @Column(name = "wsdl_port_name")
    public String getWsdlPortName() {
        return wsdlPortName;
    }

    public void setWsdlPortName( final String wsdlPortName ) {
        this.wsdlPortName = wsdlPortName;
    }

    @Column(name = "wsdl_port_binding")
    public String getWsdlPortBinding() {
        return wsdlPortBinding;
    }

    public void setWsdlPortBinding( final String wsdlPortBinding ) {
        this.wsdlPortBinding = wsdlPortBinding;
    }

    @Column(name = "wsdl_port_binding_namespace")
    public String getWsdlPortBindingNamespace() {
        return wsdlPortBindingNamespace;
    }

    public void setWsdlPortBindingNamespace( final String wsdlPortBindingNamespace ) {
        this.wsdlPortBindingNamespace = wsdlPortBindingNamespace;
    }

    @Column(name = "under_uddi_control")
    public boolean isUnderUddiControl() {
        return underUddiControl;
    }

    public void setUnderUddiControl( final boolean underUddiControl ) {
        this.underUddiControl = underUddiControl;
    }

    @Column(name = "proxy_binding_key")
    public String getProxyBindingKey() {
        return proxyBindingKey;
    }

    public void setProxyBindingKey( final String proxyBindingKey ) {
        this.proxyBindingKey = proxyBindingKey;
    }

    @Column(name = "monitoring_enabled")
    public boolean isMonitoringEnabled() {
        return monitoringEnabled;
    }

    public void setMonitoringEnabled( final boolean monitoringEnabled ) {
        this.monitoringEnabled = monitoringEnabled;
    }

    @Column(name = "disable_service_on_change")
    public boolean isDisableServiceOnChange() {
        return disableServiceOnChange;
    }

    public void setDisableServiceOnChange( final boolean disableServiceOnChange ) {
        this.disableServiceOnChange = disableServiceOnChange;
    }

    @Column(name = "metrics_enabled")
    public boolean isMetricsEnabled() {
        return metricsEnabled;
    }

    public void setMetricsEnabled( final boolean metricsEnabled ) {
        this.metricsEnabled = metricsEnabled;
    }

    @Column(name = "wspolicy_tmodel_key")
    public String getWsPolicyTModelKey() {
        return wsPolicyTModelKey;
    }

    public void setWsPolicyTModelKey( final String wsPolicyTModelKey ) {
        this.wsPolicyTModelKey = wsPolicyTModelKey;
    }

    @Override
    @Version
    @Column(name = "version")
    public int getVersion() {
        return super.getVersion();
    }

    //- PRIVATE

    private long publishedServiceOid;
    private long uddiRegistryOid;
    private String uddiBusinessKey;
    private String uddiBusinessName;
    private String uddiServiceKey;
    private String uddiServiceName;
    private String wsdlServiceName;
    private String wsdlPortName;
    private String wsdlPortBinding;
    private String wsdlPortBindingNamespace;
    private boolean underUddiControl;
    private String proxyBindingKey;
    private boolean monitoringEnabled;
    private boolean disableServiceOnChange;
    private boolean metricsEnabled;
    private String wsPolicyTModelKey;
}
