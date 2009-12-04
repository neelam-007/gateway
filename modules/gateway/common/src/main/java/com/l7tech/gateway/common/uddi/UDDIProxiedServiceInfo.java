package com.l7tech.gateway.common.uddi;

import com.l7tech.objectmodel.imp.PersistentEntityImp;

import javax.persistence.*;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.CascadeType;

import org.hibernate.annotations.*;

import java.util.Set;
import java.util.HashSet;

//todo UDDIProxiedServiceInfo contains both runtime and configuration data. Need to be refactored.
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

    public enum PublishType{
        /**
         * We have published the Gateway WSDL as a proxy
         */
        PROXY,
        /**
         * We have published a bindingTemplate as a new endpoint to an existing BusinessService, now owned by
         * the cluster this SSG is part of
         */
        ENDPOINT,
        /**
         * We have published to an existing BusinessService and have overwritten all of it's URLs to point to the
         * Gateway. When this happens we now own the BusinessService.
         */
        OVERWRITE
    }


    public UDDIProxiedServiceInfo() {
    }

    public static UDDIProxiedServiceInfo getEndPointPublishInfo(final long publishedServiceOid,
                                                                final long uddiRegistryOid,
                                                                final String uddiBusinessKey,
                                                                final String uddiBusinessName,
                                                                final String wsdlHash,
                                                                final String publishedHostname,
                                                                final boolean removeOtherBindings) {

        final UDDIProxiedServiceInfo info = new UDDIProxiedServiceInfo();
        info.setPublishedServiceOid(publishedServiceOid);
        info.setUddiRegistryOid(uddiRegistryOid);
        info.setUddiBusinessKey(uddiBusinessKey);
        info.setUddiBusinessName(uddiBusinessName);
        info.setPublishType(PublishType.ENDPOINT);
        info.setWsdlHash(wsdlHash);
        info.setPublishedHostname(publishedHostname);
        info.setRemoveOtherBindings(removeOtherBindings);
        return info;
    }

    public static UDDIProxiedServiceInfo getProxyServicePublishInfo(final long publishedServiceOid,
                                                                    final long uddiRegistryOid,
                                                                    final String uddiBusinessKey,
                                                                    final String uddiBusinessName,
                                                                    final String wsdlHash,
                                                                    final String publishedHostname,
                                                                    final boolean updateProxyOnLocalChange
    ) {

        final UDDIProxiedServiceInfo info = new UDDIProxiedServiceInfo();
        info.setPublishedServiceOid(publishedServiceOid);
        info.setUddiRegistryOid(uddiRegistryOid);
        info.setUddiBusinessKey(uddiBusinessKey);
        info.setUddiBusinessName(uddiBusinessName);
        info.setPublishType(PublishType.PROXY);
        info.setWsdlHash(wsdlHash);
        info.setPublishedHostname(publishedHostname);
        info.setUpdateProxyOnLocalChange(updateProxyOnLocalChange);
        return info;
    }

    public static UDDIProxiedServiceInfo getOverwriteProxyServicePublishInfo(final long publishedServiceOid,
                                                                             final long uddiRegistryOid,
                                                                             final String uddiBusinessKey,
                                                                             final String uddiBusinessName,
                                                                             final String wsdlHash,
                                                                             final String publishedHostname,
                                                                             final boolean updateProxyOnLocalChange) {

        final UDDIProxiedServiceInfo info = new UDDIProxiedServiceInfo();
        info.setPublishedServiceOid(publishedServiceOid);
        info.setUddiRegistryOid(uddiRegistryOid);
        info.setUddiBusinessKey(uddiBusinessKey);
        info.setUddiBusinessName(uddiBusinessName);
        info.setPublishType(PublishType.OVERWRITE);
        info.setWsdlHash(wsdlHash);
        info.setPublishedHostname(publishedHostname);
        info.setUpdateProxyOnLocalChange(updateProxyOnLocalChange);
        return info;
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

    @OneToMany(cascade= CascadeType.ALL, fetch=FetchType.EAGER, mappedBy="uddiProxiedServiceInfo")
    @Fetch(FetchMode.SUBSELECT)
    @Cascade({org.hibernate.annotations.CascadeType.DELETE_ORPHAN, org.hibernate.annotations.CascadeType.ALL})
    @OnDelete(action= OnDeleteAction.CASCADE)
    public Set<UDDIProxiedService> getProxiedServices() {
        return proxiedServices;
    }

    public void setProxiedServices(Set<UDDIProxiedService> proxiedServices) {
        this.proxiedServices = proxiedServices;
    }

    @Column(name = "publish_type")
    @Enumerated(EnumType.STRING)
    public PublishType getPublishType() {
        return publishType;
    }

    public void setPublishType(PublishType publishType) {
        this.publishType = publishType;
    }

    @Column(name = "published_hostname", updatable = false)
    public String getPublishedHostname() {
        return publishedHostname;
    }

    public void setPublishedHostname(String publishedHostname) {
        this.publishedHostname = publishedHostname;
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

    @Column(name = "wsdl_hash")
    public String getWsdlHash() {
        return wsdlHash;
    }

    public void setWsdlHash(String wsdlHash) {
        this.wsdlHash = wsdlHash;
    }

    @Column(name = "remove_other_bindings", updatable = false)
    public boolean isRemoveOtherBindings() {
        return removeOtherBindings;
    }

    public void setRemoveOtherBindings(boolean removeOtherBindings) {
        this.removeOtherBindings = removeOtherBindings;
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

    private Set<UDDIProxiedService> proxiedServices = new HashSet<UDDIProxiedService>();

    private PublishType publishType;

    /**
     * This field is the hostname which was used when publishing information to UDDI. This is used when publishing
     * bindingTemplates. To find if a business service contains binding templates published by the ssg, the hostname
     * of the bindingTemplates accesspoint in combination with the oid of the published service are compared.
     * The bindingKey is no longer persisted as we can publish more than one bindingTemplate for a service, http and
     * https. 
     */
    private String publishedHostname;

    private boolean publishWsPolicyEnabled;

    private boolean publishWsPolicyFull;
    
    private boolean publishWsPolicyInlined;
    
    private String wsdlHash;

    private boolean removeOtherBindings;
}

