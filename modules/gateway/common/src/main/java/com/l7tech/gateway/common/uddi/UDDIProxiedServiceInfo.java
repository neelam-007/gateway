package com.l7tech.gateway.common.uddi;

import com.l7tech.objectmodel.imp.PersistentEntityImp;

import javax.persistence.*;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.CascadeType;

import org.hibernate.annotations.*;

import java.util.Set;
import java.util.HashSet;

/**
 * Copyright (C) 2008, Layer 7 Technologies Inc.
 *
 * Entity which represents the fact that a PublishedService has been published as a proxy BusinessService
 * to UDDI. This stores the common information. The actual wsdl:service information is stored in
 * UDDIProxiedService
 *
 * @author darmstrong
 */
@Entity
@Proxy(lazy=false)
@Table(name="uddi_proxied_service_info")
public class UDDIProxiedServiceInfo extends PersistentEntityImp {

    public static final String ATTR_SERVICE_OID = "publishedServiceOid";

    public UDDIProxiedServiceInfo() {
    }

    public UDDIProxiedServiceInfo(long publishedServiceOid, long uddiRegistryOid, String uddiBusinessKey, String uddiBusinessName, boolean updateProxyOnLocalChange) {
        super();
        this.publishedServiceOid = publishedServiceOid;
        this.uddiRegistryOid = uddiRegistryOid;
        this.uddiBusinessKey = uddiBusinessKey;
        this.uddiBusinessName = uddiBusinessName;
        this.updateProxyOnLocalChange = updateProxyOnLocalChange;
    }

    /**
     * Used to determine if 'this' UDDIProxiedService has had a property modified which should not be modified once
     * the entity has been created based on application logic
     *
     * @param original UDDIProxiedService last known version of 'this' UDDIProxiedService used to compare what has
     * changed in 'this'
     */
    public void throwIfFinalPropertyModified(final UDDIProxiedServiceInfo original){
        testProperty("business key", this.getUddiBusinessKey(), original.getUddiBusinessKey());
        testProperty("business name", this.getUddiBusinessName(), original.getUddiBusinessName());
        testProperty("registry oid", Long.toString(this.getUddiRegistryOid()), Long.toString(original.getUddiRegistryOid()));
        testProperty("created from existing", Boolean.toString(this.isCreatedFromExistingService()), Boolean.toString(original.isCreatedFromExistingService()));
    }

    static void testProperty(final String propName, final String propValue, final String lastKnownValue){
        if(propValue == null)
            throw new IllegalStateException(propName + " property must be set");
        //the service identifier is not allowed to be modified by client code once saved
        if(!lastKnownValue.equals(propValue)){
            throw new IllegalStateException("It is not possible to modify property " + propName);
        }
    }

    @Override
    @Version
    @Column(name = "version")
    public int getVersion() {
        return super.getVersion();
    }

    @Column(name = "published_service_oid", updatable = false)
    public long getPublishedServiceOid() {
        return publishedServiceOid;
    }

    public void setPublishedServiceOid(long serviceOid) {
        this.publishedServiceOid = serviceOid;
    }

    @Column(name = "uddi_registry_oid", updatable = false)
    public long getUddiRegistryOid() {
        return uddiRegistryOid;
    }

    public void setUddiRegistryOid(long uddiRegistryOid) {
        this.uddiRegistryOid = uddiRegistryOid;
    }

    @Column(name = "uddi_business_key", updatable = false)
    public String getUddiBusinessKey() {
        return uddiBusinessKey;
    }

    public void setUddiBusinessKey(String uddiBusinessKey) {
        this.uddiBusinessKey = uddiBusinessKey;
    }

    @Column(name = "uddi_business_name", updatable = false)
    public String getUddiBusinessName() {
        return uddiBusinessName;
    }

    public void setUddiBusinessName(String uddiBusinessName) {
        this.uddiBusinessName = uddiBusinessName;
    }

    @Column(name = "update_proxy_on_local_change")
    public boolean isUpdateProxyOnLocalChange() {
        return updateProxyOnLocalChange;
    }

    public void setUpdateProxyOnLocalChange(boolean updateProxyOnLocalChange) {
        this.updateProxyOnLocalChange = updateProxyOnLocalChange;
    }

    @Column(name = "created_from_existing", updatable = false)
    public boolean isCreatedFromExistingService() {
        return createdFromExistingService;
    }

    public void setCreatedFromExistingService(boolean createdFromExistingService) {
        this.createdFromExistingService = createdFromExistingService;
    }

    @Column(name = "metrics_enabled")
    public boolean isMetricsEnabled() {
        return metricsEnabled;
    }

    public void setMetricsEnabled(boolean metricsEnabled) {
        this.metricsEnabled = metricsEnabled;
    }

    @Column(name = "wspolicy_tmodel_key")
    public String getWsPolicyTModelKey() {
        return wsPolicyTModelKey;
    }

    public void setWsPolicyTModelKey(String wsPolicyTModelKey) {
        this.wsPolicyTModelKey = wsPolicyTModelKey;
    }

    @OneToMany(cascade= CascadeType.ALL, fetch=FetchType.EAGER, mappedBy="proxiedServiceInfo")
    @Fetch(FetchMode.SUBSELECT)
    @Cascade({org.hibernate.annotations.CascadeType.DELETE_ORPHAN, org.hibernate.annotations.CascadeType.ALL})
    @OnDelete(action= OnDeleteAction.CASCADE)
    public Set<UDDIProxiedService> getProxiedServices() {
        return proxiedServices;
    }

    public void setProxiedServices(Set<UDDIProxiedService> proxiedServices) {
        this.proxiedServices = proxiedServices;
    }
    // PRIVATE

    /**
     * Which published service this proxied service was published for
     */
    private long publishedServiceOid;

    /**
     * The UDDI Registry where this proxied service was published
     */
    private long uddiRegistryOid;

    // -- UDDI information

    /**
     * businessKey of owning business entity of proxied service in UDDI registry
     */
    private String uddiBusinessKey;

    /**
     * Name of the Business Entity. Persisted to show in the UI
     * //todo we should probably just retrieve this as required
     */
    private String uddiBusinessName;

    /**
     * If the gateway WSDL changes, update UDDI
     */
    private boolean updateProxyOnLocalChange;

    /**
     * Did we take over an existing Business Service? Persisted for the UI
     */
    private boolean createdFromExistingService;

    /**
     * Should metrics be published? Depends on the UDDI also being enabled for metrics
     */
    private boolean metricsEnabled;

    /**
     * If a WS Policy was published at any point, this is the tModelKey for it
     */
    private String wsPolicyTModelKey;

    private Set<UDDIProxiedService> proxiedServices = new HashSet<UDDIProxiedService>();

}

