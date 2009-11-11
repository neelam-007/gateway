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

    public static final String ATTR_SERVICE_OID = "publishedServiceOid";

    //- PUBLIC

    public UDDIServiceControl(long publishedServiceOid,
                              long uddiRegistryOid,
                              String uddiBusinessKey,
                              String uddiServiceKey,
                              String uddiServiceName,
                              String wsdlServiceName,
                              String wsdlPortName,
                              String wsdlPortBinding,
                              String accessPointUrl,
                              boolean underUddiControl) {
        this.publishedServiceOid = publishedServiceOid;
        this.uddiRegistryOid = uddiRegistryOid;
        this.uddiBusinessKey = uddiBusinessKey;
        this.uddiServiceKey = uddiServiceKey;
        this.uddiServiceName = uddiServiceName;
        this.wsdlServiceName = wsdlServiceName;
        this.wsdlPortName = wsdlPortName;
        this.wsdlPortBinding = wsdlPortBinding;
        this.accessPointUrl = accessPointUrl;
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
     * Values like the wsdl port can change, as its still the same WSDL.
     * //todo go over this 
     * @param original UDDIProxiedService last known version of 'this' UDDIProxiedService used to compare what has
     * changed in 'this'
     */
    public void throwIfFinalPropertyModified(final UDDIServiceControl original){
        testProperty("published service oid", Long.toString(this.getPublishedServiceOid()), Long.toString(original.getPublishedServiceOid()));
        testProperty("registry oid", Long.toString(this.getUddiRegistryOid()), Long.toString(original.getUddiRegistryOid()));
        testProperty("business key", this.getUddiBusinessKey(), original.getUddiBusinessKey());
        testProperty("services key", this.getUddiServiceKey(), original.getUddiServiceKey());
    }

    private void testProperty(final String propName, final String propValue, final String lastKnownValue){
        if(propValue == null)
            throw new IllegalStateException(propName + " property must be set");
        //the service identifier is not allowed to be modified by client code once saved
        if(!lastKnownValue.equals(propValue)){
            throw new IllegalStateException("It is not possible to modify property " + propName);
        }
    }

    /**
     * Set the properties which can change in either UDDI or in the selection of wsdl:port from the existing
     * BusinessService in UDDI
     * @param portInfo WsdlPortInfo info from UDDI
     * @return true if a property was changed, false otherwise
     */
    public boolean setUddiModifiableProperties(WsdlPortInfo portInfo){
        boolean modified = false;

        if(!this.getUddiBusinessName().equals(portInfo.getBusinessServiceName())) modified = true;
        this.setUddiBusinessName(portInfo.getBusinessServiceName());

        if(!this.getUddiServiceName().equals(portInfo.getWsdlServiceName())) modified = true;
        this.setUddiServiceName(portInfo.getWsdlServiceName());

        if(!this.getWsdlPortName().equals(portInfo.getWsdlPortName())) modified = true;
        this.setWsdlPortName(portInfo.getWsdlPortName());

        if(!this.getWsdlPortBinding().equals(portInfo.getWsdlPortBinding())) modified = true;
        this.setWsdlPortBinding(portInfo.getWsdlPortBinding());

        if(!this.getAccessPointUrl().equals(portInfo.getAccessPointURL())) modified = true;
        this.setAccessPointUrl(portInfo.getAccessPointURL());

        return modified;
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

    @Column(name = "uddi_service_key", updatable = false)
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

    @Column(name = "metrics_enabled")
    public boolean isMetricsEnabled() {
        return metricsEnabled;
    }

    public void setMetricsEnabled( final boolean metricsEnabled ) {
        this.metricsEnabled = metricsEnabled;
    }

    @Column(name = "access_point_url")
    public String getAccessPointUrl() {
        return accessPointUrl;
    }

    public void setAccessPointUrl(String accessPointUrl) {
        this.accessPointUrl = accessPointUrl;
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
     * @return
     */
    @Column(name = "has_had_endpoints_removed")
    public boolean isHasHadEndpointRemoved() {
        return hasHadEndpointRemoved;
    }

    public void setHasHadEndpointRemoved(boolean hasHadEndpointRemoved) {
        this.hasHadEndpointRemoved = hasHadEndpointRemoved;
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
    private String accessPointUrl;
    private boolean underUddiControl;
    private boolean monitoringEnabled;
    private boolean updateWsdlOnChange;
    private boolean disableServiceOnChange;
    private boolean metricsEnabled;
    private boolean publishWsPolicyEnabled;
    private boolean publishWsPolicyFull;
    private boolean publishWsPolicyInlined;
    private boolean hasHadEndpointRemoved;
}
