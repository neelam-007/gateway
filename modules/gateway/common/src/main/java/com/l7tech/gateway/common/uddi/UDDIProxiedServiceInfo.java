package com.l7tech.gateway.common.uddi;

import com.l7tech.common.io.NonCloseableOutputStream;
import com.l7tech.gateway.common.admin.UDDIRegistryAdmin;
import com.l7tech.objectmodel.imp.PersistentEntityImp;

import javax.persistence.*;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.CascadeType;

import com.l7tech.util.BufferPoolByteArrayOutputStream;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.HexUtils;
import com.l7tech.util.ResourceUtils;
import org.hibernate.annotations.*;

import java.beans.ExceptionListener;
import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.logging.Level;
import java.util.logging.Logger;

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

    /**
     * Collection<EndpointPair>. Do not modify (without adding an upgrade sql / task), used in upgrade 5.2 -> 5.3 script
     */
    public static final String ALL_ENDPOINT_PAIRS_KEY = "ALL_ENDPOINT_PAIRS_KEY";

    /**
     * Key to get the Set<String> of all bindingKeys published. Only exists when the type is ENDPOINT
     */
    public static final String ALL_BINDING_TEMPLATE_KEYS = "ALL_BINDING_TEMPLATE_KEYS";//DO NOT REFACTOR THEY ARE IN THE DB
    public static final String FUNCTIONAL_ENDPOINT_KEY = "FUNCTIONAL_ENDPOINT_KEY";//DO NOT REFACTOR THEY ARE IN THE DB
    public static final String GIF_SCHEME = "GIF_SCHEME";//DO NOT REFACTOR THEY ARE IN THE DB
    public static final String IS_GIF = "IS_GIF";//DO NOT REFACTOR THEY ARE IN THE DB

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

    private static UDDIProxiedServiceInfo getUDDIProxiedServiceInfo(final long publishedServiceOid,
                                                                    final long uddiRegistryOid,
                                                                    final String uddiBusinessKey,
                                                                    final String uddiBusinessName,
                                                                    final String wsdlHash,
                                                                    final PublishType publishType) {
        final UDDIProxiedServiceInfo info = new UDDIProxiedServiceInfo();
        info.setPublishedServiceOid(publishedServiceOid);
        info.setUddiRegistryOid(uddiRegistryOid);
        info.setUddiBusinessKey(uddiBusinessKey);
        info.setUddiBusinessName(uddiBusinessName);
        info.setWsdlHash(wsdlHash);
        info.setPublishType(publishType);
        return info;
    }

    public static UDDIProxiedServiceInfo getGifEndPointPublishInfo(final long publishedServiceOid,
                                                                final long uddiRegistryOid,
                                                                final String uddiBusinessKey,
                                                                final String uddiBusinessName,
                                                                final String wsdlHash,
                                                                final UDDIRegistryAdmin.EndpointScheme endpointScheme
                                                                ) {

        final UDDIProxiedServiceInfo info = getUDDIProxiedServiceInfo(
                publishedServiceOid,
                uddiRegistryOid,
                uddiBusinessKey,
                uddiBusinessName,
                wsdlHash,
                PublishType.ENDPOINT);

        info.setProperty(IS_GIF, Boolean.TRUE);
        info.setProperty(GIF_SCHEME, endpointScheme);
        return info;
    }

    public static UDDIProxiedServiceInfo getEndPointPublishInfo(final long publishedServiceOid,
                                                                final long uddiRegistryOid,
                                                                final String uddiBusinessKey,
                                                                final String uddiBusinessName,
                                                                final String wsdlHash,
                                                                final boolean removeOtherBindings) {

        final UDDIProxiedServiceInfo info = getUDDIProxiedServiceInfo(
                publishedServiceOid,
                uddiRegistryOid,
                uddiBusinessKey,
                uddiBusinessName,
                wsdlHash,
                PublishType.ENDPOINT);

        info.setRemoveOtherBindings(removeOtherBindings);
        return info;
    }

    public static UDDIProxiedServiceInfo getProxyServicePublishInfo(final long publishedServiceOid,
                                                                    final long uddiRegistryOid,
                                                                    final String uddiBusinessKey,
                                                                    final String uddiBusinessName,
                                                                    final String wsdlHash,
                                                                    final boolean updateProxyOnLocalChange) {

        final UDDIProxiedServiceInfo info = getUDDIProxiedServiceInfo(
                publishedServiceOid,
                uddiRegistryOid,
                uddiBusinessKey,
                uddiBusinessName,
                wsdlHash,
                PublishType.PROXY);

        info.setUpdateProxyOnLocalChange(updateProxyOnLocalChange);
        return info;
    }

    public static UDDIProxiedServiceInfo getOverwriteProxyServicePublishInfo(final long publishedServiceOid,
                                                                             final long uddiRegistryOid,
                                                                             final String uddiBusinessKey,
                                                                             final String uddiBusinessName,
                                                                             final String wsdlHash,
                                                                             final boolean updateProxyOnLocalChange) {

        final UDDIProxiedServiceInfo info = getUDDIProxiedServiceInfo(
                publishedServiceOid,
                uddiRegistryOid,
                uddiBusinessKey,
                uddiBusinessName,
                wsdlHash,
                PublishType.OVERWRITE);

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

    /**
     * Persistence of serialized value with Hibernate. To get the unserialized value, call getProperty
     */
    @Column(name="properties", length=Integer.MAX_VALUE)
    @Lob
    public String getSerializedProps() throws java.io.IOException {
        if (propsXml == null) {
            // if no props, return empty string
            if (props.size() < 1) {
                propsXml = "";
            } else {
                BufferPoolByteArrayOutputStream output = null;
                java.beans.XMLEncoder encoder = null;
                try {
                    output = new BufferPoolByteArrayOutputStream();
                    encoder = new java.beans.XMLEncoder(new NonCloseableOutputStream(output));
                    encoder.setExceptionListener( new ExceptionListener() {
                        @Override
                        public void exceptionThrown( final Exception e ) {
                            logger.log( Level.WARNING, "Error storing properties '"+ ExceptionUtils.getMessage(e)+"'.", ExceptionUtils.getDebugException(e));
                        }
                    });
                    encoder.writeObject(props);
                    encoder.close(); // writes closing XML tag
                    encoder = null;
                    propsXml = output.toString("UTF-8");
                }
                finally {
                    if(encoder!=null) encoder.close();
                    ResourceUtils.closeQuietly(output);
                }
            }
        }

        return propsXml;
    }

    /**
     * Persistence of serialized value with Hibernate. To get the unserialized value, call getProperty
     */
    public void setSerializedProps(String serializedProps) {
        propsXml = serializedProps;
        if (serializedProps == null || serializedProps.length() < 2) {
            props.clear();
        } else {
            ByteArrayInputStream in = new ByteArrayInputStream(HexUtils.encodeUtf8(serializedProps));
            java.beans.XMLDecoder decoder = new java.beans.XMLDecoder(in, null, new ExceptionListener() {
                @Override
                public void exceptionThrown( final Exception e ) {
                    logger.log( Level.WARNING, "Error loading properties '"+ExceptionUtils.getMessage(e)+"'.", ExceptionUtils.getDebugException(e));
                }
            });
            //noinspection unchecked
            props = (Map<String, Object>) decoder.readObject();
        }
    }

    @SuppressWarnings({ "unchecked" })
    public <T> T getProperty(String name) {
        return (T)props.get(name);
    }

    public void setProperty(String name, Object value) {
        if ( value == null ) {
            props.remove( name );
        } else {
            props.put(name, value);
        }
        propsXml = null;
    }

    // PRIVATE

    private static final Logger logger = Logger.getLogger(UDDIProxiedServiceInfo.class.getName());
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

    private boolean publishWsPolicyEnabled;

    private boolean publishWsPolicyFull;
    
    private boolean publishWsPolicyInlined;
    
    private String wsdlHash;

    private boolean removeOtherBindings;

    private String propsXml;

    /**
     * props is backed by propsXml, which is persisted. This property is populated with the unserialized value of
     * propsXml. Runtime values are added to this property, which will be serialized and persisted via propsXml when
     * this entity is saved.
     */
    private Map<String, Object> props = new HashMap<String, Object>();
}

