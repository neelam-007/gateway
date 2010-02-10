package com.l7tech.gateway.common.uddi;

import com.l7tech.objectmodel.imp.PersistentEntityImp;
import com.l7tech.uddi.WsdlPortInfo;

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
//TODO rename to the UDDIOriginalService - as it may not be under UDDI Control - rename associated managers also. Not done for Bondo as too large a changeset
    public static final String ATTR_SERVICE_OID = "publishedServiceOid";

    //- PUBLIC

    public UDDIServiceControl(long publishedServiceOid,
                              long uddiRegistryOid,
                              String uddiBusinessKey,
                              String uddiBusinessName,
                              String uddiServiceKey,
                              String uddiServiceName,
                              String wsdlServiceName,
                              String wsdlPortName,
                              String wsdlPortBinding,
                              String wsdlPortBindingNamespace,
                              boolean underUddiControl) {
        this.publishedServiceOid = publishedServiceOid;
        this.uddiRegistryOid = uddiRegistryOid;
        this.uddiBusinessKey = uddiBusinessKey;
        this.uddiBusinessName = uddiBusinessName;
        this.uddiServiceKey = uddiServiceKey;
        this.uddiServiceName = uddiServiceName;
        this.wsdlServiceName = wsdlServiceName;
        this.wsdlPortName = wsdlPortName;
        this.wsdlPortBinding = wsdlPortBinding;
        this.wsdlPortBindingNamespace = wsdlPortBindingNamespace;
        this.underUddiControl = underUddiControl;
    }

    public UDDIServiceControl() {
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

    @Column(name = "uddi_business_name", updatable = false)
    public String getUddiBusinessName() {
        return uddiBusinessName;
    }

    public void setUddiBusinessName( final String uddiBusinessName ) {
        this.uddiBusinessName = uddiBusinessName;
    }

    @Column(name = "uddi_service_key", updatable = false)
    public String getUddiServiceKey() {
        return uddiServiceKey;
    }

    public void setUddiServiceKey( final String uddiServiceKey ) {
        this.uddiServiceKey = uddiServiceKey;
    }

    @Column(name = "uddi_service_name", updatable = false)
    public String getUddiServiceName() {
        return uddiServiceName;
    }

    public void setUddiServiceName( final String uddiServiceName ) {
        this.uddiServiceName = uddiServiceName;
    }

    @Column(name = "wsdl_service_name", updatable = false)
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

    @Column(name = "wsdl_port_binding", updatable = false)
    public String getWsdlPortBinding() {
        return wsdlPortBinding;
    }

    public void setWsdlPortBinding( final String wsdlPortBinding ) {
        this.wsdlPortBinding = wsdlPortBinding;
    }

    @Column(name = "wsdl_port_binding_namespace", updatable = false)
    public String getWsdlPortBindingNamespace() {
        return wsdlPortBindingNamespace;
    }

    public void setWsdlPortBindingNamespace(String wsdlPortBindingNamespace) {
        this.wsdlPortBindingNamespace = wsdlPortBindingNamespace;
    }

    @Column(name = "under_uddi_control")
    public boolean isUnderUddiControl() {
        return underUddiControl;
    }

    public void setUnderUddiControl( final boolean underUddiControl ) {
        this.underUddiControl = underUddiControl;
    }

    @Column(name = "monitoring_enabled")
    public boolean isMonitoringEnabled() {
        return monitoringEnabled;
    }

    public void setMonitoringEnabled( final boolean monitoringEnabled ) {
        this.monitoringEnabled = monitoringEnabled;
    }

    @Column(name = "update_wsdl_on_change")
    public boolean isUpdateWsdlOnChange() {
        return updateWsdlOnChange;
    }

    public void setUpdateWsdlOnChange( final boolean updateWsdlOnChange ) {
        this.updateWsdlOnChange = updateWsdlOnChange;
    }

    @Column(name = "disable_service_on_change")
    public boolean isDisableServiceOnChange() {
        return disableServiceOnChange;
    }

    public void setDisableServiceOnChange( final boolean disableServiceOnChange ) {
        this.disableServiceOnChange = disableServiceOnChange;
    }

    @Column(name = "metrics_enabled")//todo not needed as not supported right now with original service - remove
    public boolean isMetricsEnabled() {
        return metricsEnabled;
    }

    public void setMetricsEnabled( final boolean metricsEnabled ) {
        this.metricsEnabled = metricsEnabled;
    }

    @Column(name = "publish_wspolicy_enabled")
    public boolean isPublishWsPolicyEnabled() {
        return publishWsPolicyEnabled;
    }

    public void setPublishWsPolicyEnabled( final boolean publishWsPolicyEnabled ) {
        this.publishWsPolicyEnabled = publishWsPolicyEnabled;
    }

    @Column(name = "publish_wspolicy_full")
    public boolean isPublishWsPolicyFull() {
        return publishWsPolicyFull;
    }

    public void setPublishWsPolicyFull( final boolean publishWsPolicyFull ) {
        this.publishWsPolicyFull = publishWsPolicyFull;
    }

    @Column(name = "publish_wspolicy_inlined")
    public boolean isPublishWsPolicyInlined() {
        return publishWsPolicyInlined;
    }

    public void setPublishWsPolicyInlined( final boolean publishWsPolicyInlined ) {
        this.publishWsPolicyInlined = publishWsPolicyInlined;
    }

    /**
     * If the BusinessService that a PublishedService was created for is known to ever had it's end points removed
     * via the publish end point feature in the 'Publish' tab, then the service can never be under uddi control
     * again, as we know that the read endpoints have been deleted, so there is nothing to monitor. We could
     * publish metrics still to it, but that is not currently supported
     */
    @Column(name = "has_had_endpoints_removed")
    public boolean isHasHadEndpointRemoved() {
        return hasHadEndpointRemoved;
    }

    public void setHasHadEndpointRemoved(boolean hasHadEndpointRemoved) {
        this.hasHadEndpointRemoved = hasHadEndpointRemoved;
    }

    /**
     * If a business service created from UDDI has had the 'overwrite service' feature used, then this is a record
     * of it. When this happens we can never monitor the service again as it will route back to the SSG.
     */
    @Column(name = "has_been_overwritten")
    public boolean isHasBeenOverwritten() {
        return hasBeenOverwritten;
    }

    public void setHasBeenOverwritten(boolean hasBeenOverwritten) {
        this.hasBeenOverwritten = hasBeenOverwritten;
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
    private boolean monitoringEnabled;
    private boolean updateWsdlOnChange;
    private boolean disableServiceOnChange;
    private boolean metricsEnabled;
    private boolean publishWsPolicyEnabled;
    private boolean publishWsPolicyFull;
    private boolean publishWsPolicyInlined;
    private boolean hasHadEndpointRemoved;
    private boolean hasBeenOverwritten;
    private String wsdlPortBindingNamespace;
}
