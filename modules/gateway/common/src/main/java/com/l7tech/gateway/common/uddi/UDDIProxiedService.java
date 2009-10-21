/**
 * Copyright (C) 2008, Layer 7 Technologies Inc.
 * @author darmstrong
 */
package com.l7tech.gateway.common.uddi;

import org.hibernate.annotations.Proxy;

import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.Column;

import com.l7tech.objectmodel.imp.NamedEntityImp;

@Entity
@Proxy(lazy=false)
@Table(name="uddi_proxied_service")
public class UDDIProxiedService extends NamedEntityImp {

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
    private String generalKeyword;

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

    public UDDIProxiedService(long serviceOid, String name, long uddiRegistryOid, String uddiBusinessKey, String uddiBusinessName, String generalKeyword, boolean updateProxyOnLocalChange, boolean createdFromExistingService, boolean metricsEnabled, String wsPolicyTModelKey) {
        super();
        setOid(serviceOid);
        setName(name);
        this.serviceOid = serviceOid;
        this.uddiRegistryOid = uddiRegistryOid;
        this.uddiBusinessKey = uddiBusinessKey;
        this.uddiBusinessName = uddiBusinessName;
        this.generalKeyword = generalKeyword;
        this.updateProxyOnLocalChange = updateProxyOnLocalChange;
        this.createdFromExistingService = createdFromExistingService;
        this.metricsEnabled = metricsEnabled;
        this.wsPolicyTModelKey = wsPolicyTModelKey;
    }

    @Column(name = "published_service_oid")
    public long getServiceOid() {
        return serviceOid;
    }

    public void setServiceOid(long serviceOid) {
        this.serviceOid = serviceOid;
    }

    @Column(name = "uddi_registry_oid")
    public long getUddiRegistryOid() {
        return uddiRegistryOid;
    }

    public void setUddiRegistryOid(long uddiRegistryOid) {
        this.uddiRegistryOid = uddiRegistryOid;
    }

    @Column(name = "uddi_business_key")
    public String getUddiBusinessKey() {
        return uddiBusinessKey;
    }

    public void setUddiBusinessKey(String uddiBusinessKey) {
        this.uddiBusinessKey = uddiBusinessKey;
    }

    @Column(name = "uddi_business_name")
    public String getUddiBusinessName() {
        return uddiBusinessName;
    }

    public void setUddiBusinessName(String uddiBusinessName) {
        this.uddiBusinessName = uddiBusinessName;
    }

    @Column(name = "general_keyword")
    public String getGeneralKeyword() {
        return generalKeyword;
    }

    public void setGeneralKeyword(String generalKeyword) {
        this.generalKeyword = generalKeyword;
    }

    @Column(name = "update_proxy_on_local_change")
    public boolean isUpdateProxyOnLocalChange() {
        return updateProxyOnLocalChange;
    }

    public void setUpdateProxyOnLocalChange(boolean updateProxyOnLocalChange) {
        this.updateProxyOnLocalChange = updateProxyOnLocalChange;
    }

    @Column(name = "created_from_existing")
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
