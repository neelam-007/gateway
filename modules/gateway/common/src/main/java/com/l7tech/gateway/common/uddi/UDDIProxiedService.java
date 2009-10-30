/**
 * Copyright (C) 2008, Layer 7 Technologies Inc.
 * @author darmstrong
 */
package com.l7tech.gateway.common.uddi;

import org.hibernate.annotations.Proxy;

import javax.persistence.*;

import com.l7tech.objectmodel.imp.PersistentEntityImp;

@Entity
@Proxy(lazy=false)
@Table(name="uddi_proxied_service")
public class UDDIProxiedService extends PersistentEntityImp {

    public static final String ATTR_SERVICE_OID = "serviceOid";

    /**
     * Which published service this proxied service was published for
     */
    private long serviceOid;

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

    // -- MISC information

    /**
     * All BusinessServices published to UDDI from the WSDL contained by the PublishedService referenced from
     * this UDDIProxiedService will contain the same generalKeyword. This will be represented as a keyedReference
     * in the CateogryBag of the BusinessService. The keyedReference will following the general keyword system.
     * See http://www.uddi.org/pubs/uddi_v3.htm#_Toc85908318
     */
    private String generalKeywordServiceIdentifier;

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

    public UDDIProxiedService() {
    }

    public UDDIProxiedService(long serviceOid, long uddiRegistryOid, String uddiBusinessKey, String uddiBusinessName, boolean updateProxyOnLocalChange) {
        super();
        this.serviceOid = serviceOid;
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
    public void throwIfFinalPropertyModified(final UDDIProxiedService original){
        testProperty("general keyword servce identifier", this.getGeneralKeywordServiceIdentifier(), original.getGeneralKeywordServiceIdentifier());
        testProperty("business key", this.getUddiBusinessKey(), original.getUddiBusinessKey());
        testProperty("business name", this.getUddiBusinessName(), original.getUddiBusinessName());
        testProperty("registry oid", Long.toString(this.getUddiRegistryOid()), Long.toString(original.getUddiRegistryOid()));
        testProperty("created from existing", Boolean.toString(this.isCreatedFromExistingService()), Boolean.toString(original.isCreatedFromExistingService()));
    }

    private void testProperty(final String propName, final String propValue, final String lastKnownValue){
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
    public long getServiceOid() {
        return serviceOid;
    }

    public void setServiceOid(long serviceOid) {
        this.serviceOid = serviceOid;
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

    @Column(name = "general_keyword_service_identifier", updatable = false)
    public String getGeneralKeywordServiceIdentifier() {
        return generalKeywordServiceIdentifier;
    }

    public void setGeneralKeywordServiceIdentifier(String generalKeywordServiceIdentifier) {
        this.generalKeywordServiceIdentifier = generalKeywordServiceIdentifier;
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
}
