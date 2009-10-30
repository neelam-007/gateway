package com.l7tech.gateway.common.uddi;

import com.l7tech.objectmodel.imp.PersistentEntityImp;
import com.l7tech.objectmodel.PersistentEntity;

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

    public UDDIServiceControl(long publishedServiceOid,
                              long uddiRegistryOid,
                              String uddiBusinessKey,
                              String uddiServiceKey,
                              String uddiServiceName,
                              String wsdlServiceName,
                              String wsdlPortName,
                              String wsdlPortBinding,
                              boolean underUddiControl) {
        this.publishedServiceOid = publishedServiceOid;
        this.uddiRegistryOid = uddiRegistryOid;
        this.uddiBusinessKey = uddiBusinessKey;
        this.uddiServiceKey = uddiServiceKey;
        this.uddiServiceName = uddiServiceName;
        this.wsdlServiceName = wsdlServiceName;
        this.wsdlPortName = wsdlPortName;
        this.wsdlPortBinding = wsdlPortBinding;
        this.underUddiControl = underUddiControl;
    }

    public UDDIServiceControl() {
    }

    /**
     * Used to determine if 'this' UDDIProxiedService has had a property modified which should not be modified once
     * the entity has been created based on application logic
     *
     * All properties can be modified except those which define the WSDL at a high level - the uddi registry
     * and the business entity. As there is a 1:1 mapping from UDDIServiceControl there is no use case where the
     * published service oid can change.
     * Values like the business service key and wsdl port can change, as its still the same WSDL.
     * //todo go over this 
     * @param original UDDIProxiedService last known version of 'this' UDDIProxiedService used to compare what has
     * changed in 'this'
     */
    public void throwIfFinalPropertyModified(final UDDIServiceControl original){
        testProperty("published service oid", Long.toString(this.getPublishedServiceOid()), Long.toString(original.getPublishedServiceOid()));
        testProperty("registry oid", Long.toString(this.getUddiRegistryOid()), Long.toString(original.getUddiRegistryOid()));
        testProperty("business key", this.getUddiBusinessKey(), original.getUddiBusinessKey());
    }

    private void testProperty(final String propName, final String propValue, final String lastKnownValue){
        if(propValue == null)
            throw new IllegalStateException(propName + " property must be set");
        //the service identifier is not allowed to be modified by client code once saved
        if(!lastKnownValue.equals(propValue)){
            throw new IllegalStateException("It is not possible to modify property " + propName);
        }
    }

    @Column(name = "published_service_oid", updatable = false)
    public long getPublishedServiceOid() {
        return publishedServiceOid;
    }

    public void setPublishedServiceOid( final long publishedServiceOid ) {
        this.publishedServiceOid = publishedServiceOid;
    }

    @Column(name = "uddi_registry_oid", updatable = false)
    public long getUddiRegistryOid() {
        return uddiRegistryOid;
    }

    public void setUddiRegistryOid( final long uddiRegistryOid ) {
        this.uddiRegistryOid = uddiRegistryOid;
    }

    @Column(name = "uddi_business_key", updatable = false)
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
    private boolean underUddiControl;
    private String proxyBindingKey;
    private boolean monitoringEnabled;
    private boolean disableServiceOnChange;
    private boolean metricsEnabled;
    private String wsPolicyTModelKey;
}
