package com.l7tech.gateway.common.uddi;

import com.l7tech.objectmodel.imp.PersistentEntityImp;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.Version;
import javax.persistence.Column;

import com.l7tech.util.BeanUtils;
import org.hibernate.annotations.Proxy;
/**
 * Represents the UDDI location from which a published service was created. 
 */
@Entity
@Proxy(lazy=false)
@Table(name="uddi_service_control")
public class UDDIServiceControl extends PersistentEntityImp {
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

    @Column(name = "metrics_enabled")//todo not needed as not supported right now with original service
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        UDDIServiceControl that = (UDDIServiceControl) o;

        if (disableServiceOnChange != that.disableServiceOnChange) return false;
        if (hasBeenOverwritten != that.hasBeenOverwritten) return false;
        if (hasHadEndpointRemoved != that.hasHadEndpointRemoved) return false;
        if (metricsEnabled != that.metricsEnabled) return false;
        if (monitoringEnabled != that.monitoringEnabled) return false;
        if (publishWsPolicyEnabled != that.publishWsPolicyEnabled) return false;
        if (publishWsPolicyFull != that.publishWsPolicyFull) return false;
        if (publishWsPolicyInlined != that.publishWsPolicyInlined) return false;
        if (publishedServiceOid != that.publishedServiceOid) return false;
        if (uddiRegistryOid != that.uddiRegistryOid) return false;
        if (underUddiControl != that.underUddiControl) return false;
        if (updateWsdlOnChange != that.updateWsdlOnChange) return false;
        if (!uddiBusinessKey.equals(that.uddiBusinessKey)) return false;
        if (!uddiBusinessName.equals(that.uddiBusinessName)) return false;
        if (!uddiServiceKey.equals(that.uddiServiceKey)) return false;
        if (!uddiServiceName.equals(that.uddiServiceName)) return false;
        if (!wsdlPortBinding.equals(that.wsdlPortBinding)) return false;
        if (wsdlPortBindingNamespace != null ? !wsdlPortBindingNamespace.equals(that.wsdlPortBindingNamespace) : that.wsdlPortBindingNamespace != null)
            return false;
        if (!wsdlPortName.equals(that.wsdlPortName)) return false;
        if (!wsdlServiceName.equals(that.wsdlServiceName)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (int) (publishedServiceOid ^ (publishedServiceOid >>> 32));
        result = 31 * result + (int) (uddiRegistryOid ^ (uddiRegistryOid >>> 32));
        result = 31 * result + uddiBusinessKey.hashCode();
        result = 31 * result + uddiBusinessName.hashCode();
        result = 31 * result + uddiServiceKey.hashCode();
        result = 31 * result + uddiServiceName.hashCode();
        result = 31 * result + wsdlServiceName.hashCode();
        result = 31 * result + wsdlPortName.hashCode();
        result = 31 * result + wsdlPortBinding.hashCode();
        result = 31 * result + (underUddiControl ? 1 : 0);
        result = 31 * result + (monitoringEnabled ? 1 : 0);
        result = 31 * result + (updateWsdlOnChange ? 1 : 0);
        result = 31 * result + (disableServiceOnChange ? 1 : 0);
        result = 31 * result + (metricsEnabled ? 1 : 0);
        result = 31 * result + (publishWsPolicyEnabled ? 1 : 0);
        result = 31 * result + (publishWsPolicyFull ? 1 : 0);
        result = 31 * result + (publishWsPolicyInlined ? 1 : 0);
        result = 31 * result + (hasHadEndpointRemoved ? 1 : 0);
        result = 31 * result + (hasBeenOverwritten ? 1 : 0);
        result = 31 * result + (wsdlPortBindingNamespace != null ? wsdlPortBindingNamespace.hashCode() : 0);
        return result;
    }

    public static UDDIServiceControl copyFrom(UDDIServiceControl toCopy){
        final UDDIServiceControl copy = new UDDIServiceControl();
        try {
            BeanUtils.copyProperties(toCopy, copy);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return copy;
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
