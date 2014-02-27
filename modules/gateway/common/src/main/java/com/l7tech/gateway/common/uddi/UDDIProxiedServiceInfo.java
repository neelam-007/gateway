package com.l7tech.gateway.common.uddi;

import com.l7tech.common.io.NonCloseableOutputStream;
import com.l7tech.objectmodel.Goid;
import com.l7tech.security.rbac.RbacAttribute;
import com.l7tech.objectmodel.imp.ZoneableEntityImp;
import com.l7tech.uddi.UDDIKeyedReference;
import com.l7tech.uddi.UDDIUtilities;
import com.l7tech.util.*;
import org.hibernate.annotations.*;

import javax.persistence.CascadeType;
import javax.persistence.*;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.beans.ExceptionListener;
import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

//TODO change to use NamedEntityWithPropertiesImp
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
public class UDDIProxiedServiceInfo extends ZoneableEntityImp {

    public static final String ATTR_SERVICE_GOID = "publishedServiceGoid";

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

    /**
     * This represents what keyed references we need to publish. This will differ from the KEYED_REFERENCES_RUNTIME
     * during an update and if an update publish fails, when modifications have been made by the user regarding what
     * references to publish.
     */
    public static final String KEYED_REFERENCES_CONFIG = "KEYED_REFERENCES_CONFIG";//DO NOT REFACTOR THEY ARE IN THE DB

    /**
     * This value cannot be updated by a client. If it is, it's ignored. This tracks what was published and enables
     * the gateway to track when something we published needs to be removed as it is no longer required.
     */
    public static final String KEYED_REFERENCES_RUNTIME = "KEYED_REFERENCES_RUNTIME";//DO NOT REFACTOR THEY ARE IN THE DB

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

    private static UDDIProxiedServiceInfo getUDDIProxiedServiceInfo(final Goid publishedServiceGoid,
                                                                    final Goid uddiRegistryGoid,
                                                                    final String uddiBusinessKey,
                                                                    final String uddiBusinessName,
                                                                    final String wsdlHash,
                                                                    final PublishType publishType) {
        final UDDIProxiedServiceInfo info = new UDDIProxiedServiceInfo();
        info.setPublishedServiceGoid(publishedServiceGoid);
        info.setUddiRegistryGoid(uddiRegistryGoid);
        info.setUddiBusinessKey(uddiBusinessKey);
        info.setUddiBusinessName(uddiBusinessName);
        info.setWsdlHash(wsdlHash);
        info.setPublishType(publishType);
        return info;
    }

    public static UDDIProxiedServiceInfo getGifEndPointPublishInfo(final Goid publishedServiceGoid,
                                                                final Goid uddiRegistryGoid,
                                                                final String uddiBusinessKey,
                                                                final String uddiBusinessName,
                                                                final String wsdlHash
                                                                ) {

        final UDDIProxiedServiceInfo info = getUDDIProxiedServiceInfo(
                publishedServiceGoid,
                uddiRegistryGoid,
                uddiBusinessKey,
                uddiBusinessName,
                wsdlHash,
                PublishType.ENDPOINT);

        info.setProperty(IS_GIF, Boolean.TRUE);
        return info;
    }

    public static UDDIProxiedServiceInfo getEndPointPublishInfo(final Goid publishedServiceGoid,
                                                                final Goid uddiRegistryGoid,
                                                                final String uddiBusinessKey,
                                                                final String uddiBusinessName,
                                                                final String wsdlHash,
                                                                final boolean removeOtherBindings) {

        final UDDIProxiedServiceInfo info = getUDDIProxiedServiceInfo(
                publishedServiceGoid,
                uddiRegistryGoid,
                uddiBusinessKey,
                uddiBusinessName,
                wsdlHash,
                PublishType.ENDPOINT);

        info.setRemoveOtherBindings(removeOtherBindings);
        return info;
    }

    public static UDDIProxiedServiceInfo getProxyServicePublishInfo(final Goid publishedServiceGoid,
                                                                    final Goid uddiRegistryGoid,
                                                                    final String uddiBusinessKey,
                                                                    final String uddiBusinessName,
                                                                    final String wsdlHash,
                                                                    final boolean updateProxyOnLocalChange) {

        final UDDIProxiedServiceInfo info = getUDDIProxiedServiceInfo(
                publishedServiceGoid,
                uddiRegistryGoid,
                uddiBusinessKey,
                uddiBusinessName,
                wsdlHash,
                PublishType.PROXY);

        info.setUpdateProxyOnLocalChange(updateProxyOnLocalChange);
        return info;
    }

    public static UDDIProxiedServiceInfo getOverwriteProxyServicePublishInfo(final Goid publishedServiceGoid,
                                                                             final Goid uddiRegistryGoid,
                                                                             final String uddiBusinessKey,
                                                                             final String uddiBusinessName,
                                                                             final String wsdlHash,
                                                                             final boolean updateProxyOnLocalChange) {

        final UDDIProxiedServiceInfo info = getUDDIProxiedServiceInfo(
                publishedServiceGoid,
                uddiRegistryGoid,
                uddiBusinessKey,
                uddiBusinessName,
                wsdlHash,
                PublishType.OVERWRITE);

        info.setUpdateProxyOnLocalChange(updateProxyOnLocalChange);
        return info;
    }

    /**
     * UDDIProxiedServiceInfo contains both runtime and config data. In lieu of refactoring this entity the current
     * approach is to minimize areas where hibernate update collisions occur.
     * //todo refactor out config versus runtime data if this scheme becomes too complicated. For now it can be managed.
     * The follow properties are config mutable:
     * updateProxyOnLocalChange
     * metricsEnabled
     * publishWsPolicyEnabled
     * publishWsPolicyFull
     * publishWsPolicyInlined
     * UDDIProxiedServiceInfo.KEYED_REFERENCES
     * securityZone
     *
     * This method will set the above properties from the 'proxiedServiceInfo' instance onto 'this'.
     * @param proxiedServiceInfo UDDIProxiedServiceInfo with modifications to persist.
     */
    public void copyConfigModifiableProperties(final UDDIProxiedServiceInfo proxiedServiceInfo) {
        this.setUpdateProxyOnLocalChange(proxiedServiceInfo.isUpdateProxyOnLocalChange());
        this.setMetricsEnabled(proxiedServiceInfo.isMetricsEnabled());
        this.setPublishWsPolicyEnabled(proxiedServiceInfo.isPublishWsPolicyEnabled());
        this.setPublishWsPolicyFull(proxiedServiceInfo.isPublishWsPolicyFull());
        this.setPublishWsPolicyInlined(proxiedServiceInfo.isPublishWsPolicyInlined());
        this.setSecurityZone(proxiedServiceInfo.getSecurityZone());
        final Set<UDDIKeyedReference> fromUiRefs = proxiedServiceInfo.getProperty(UDDIProxiedServiceInfo.KEYED_REFERENCES_CONFIG);
        this.setProperty(UDDIProxiedServiceInfo.KEYED_REFERENCES_CONFIG, fromUiRefs);//ok if it's null, it just gets removed
    }

    public boolean areKeyedReferencesDifferent(final Set<UDDIKeyedReference> incomingRefs){
        final Set<UDDIKeyedReference> origRefs = this.getProperty(UDDIProxiedServiceInfo.KEYED_REFERENCES_CONFIG);

        boolean refsHaveChanged;

        if (incomingRefs != null && origRefs != null) {
            refsHaveChanged = !origRefs.containsAll(incomingRefs) || origRefs.size() != incomingRefs.size();
            if(!refsHaveChanged) {
                Map<UDDIKeyedReference, UDDIKeyedReference> origRefMap = new HashMap<UDDIKeyedReference, UDDIKeyedReference>();
                for (UDDIKeyedReference origRef : origRefs) {
                    origRefMap.put(origRef, origRef);
                }
                //see if any keyNames were changed. They are not part of the equality test used in the above containsAll check
                for (UDDIKeyedReference incomingRef : incomingRefs) {
                    if (origRefMap.containsKey(incomingRef)) {
                        final UDDIKeyedReference origRef = origRefMap.get(incomingRef);
                        refsHaveChanged = UDDIUtilities.areNamesDifferent(origRef.getKeyName(), incomingRef.getKeyName());
                    }
                }
            }
        } else {
            final boolean noIncoming = incomingRefs == null;
            final boolean noOrig = origRefs == null;
            refsHaveChanged = noIncoming != noOrig;
        }

        return refsHaveChanged;
    }

    @Override
    @Version
    @Column(name = "version")
    public int getVersion() {
        return super.getVersion();
    }

    @Column(name = "published_service_goid", updatable = false)
    @Type(type = "com.l7tech.server.util.GoidType")
    public Goid getPublishedServiceGoid() {
        return publishedServiceGoid;
    }

    public void setPublishedServiceGoid(Goid serviceOid) {
        this.publishedServiceGoid = serviceOid;
    }

    @Column(name = "uddi_registry_goid", updatable = false)
    @Type(type = "com.l7tech.server.util.GoidType")
    public Goid getUddiRegistryGoid() {
        return uddiRegistryGoid;
    }

    public void setUddiRegistryGoid(Goid uddiRegistryGoid) {
        this.uddiRegistryGoid = uddiRegistryGoid;
    }

    @RbacAttribute
    @Column(name = "uddi_business_key", updatable = false)
    public String getUddiBusinessKey() {
        return uddiBusinessKey;
    }

    public void setUddiBusinessKey(String uddiBusinessKey) {
        this.uddiBusinessKey = uddiBusinessKey;
    }

    @RbacAttribute
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
    public boolean isCreatedFromExistingService() {     //todo delete
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

    @RbacAttribute
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
                PoolByteArrayOutputStream output = null;
                java.beans.XMLEncoder encoder = null;
                try {
                    output = new PoolByteArrayOutputStream();
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
            SafeXMLDecoder decoder = new SafeXMLDecoderBuilder(in)
                    .setExceptionListener(new ExceptionListener() {
                        @Override
                        public void exceptionThrown( final Exception e ) {
                            logger.log( Level.WARNING, "Error loading properties '"+ExceptionUtils.getMessage(e)+"'.", ExceptionUtils.getDebugException(e));
                        }
                    })
                    .build();
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
    private Goid publishedServiceGoid;

    /**
     * The UDDI Registry where this proxied service was published
     */
    private Goid uddiRegistryGoid;

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

