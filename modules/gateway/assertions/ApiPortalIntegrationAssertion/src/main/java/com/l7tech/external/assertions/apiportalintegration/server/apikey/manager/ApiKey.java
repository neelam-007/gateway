package com.l7tech.external.assertions.apiportalintegration.server.apikey.manager;

import com.l7tech.external.assertions.apiportalintegration.server.AbstractPortalGenericEntity;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Represents a portal API key where key is stored in the name field.
 */
public class ApiKey extends AbstractPortalGenericEntity {
    /**
     * Key = service id, Value = plan id.
     */
    private Map<String, String> serviceIds = new HashMap<String, String>();
    private String secret;
    private String status;
    private String label;
    private String platform;
    private String oauthCallbackUrl;
    private String oauthScope;
    private String oauthType;

    private Date lastUpdate;
    private String accountPlanMappingId;
    /**
     * Organization name.
     */
    private String accountPlanMappingName;
    //private String xmlRepresentation;
    private String customMetaData;
    private String applicationId;

    public Map<String, String> getServiceIds() {
        return serviceIds;
    }

    public void setServiceIds(final Map<String, String> serviceIds) {
        this.serviceIds = serviceIds;
    }

    public String getSecret() {
        return secret;
    }

    public void setSecret(final String secret) {
        this.secret = secret;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(final String status) {
        this.status = status;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(final String label) {
        this.label = label;
    }

    public String getPlatform() {
        return platform;
    }

    public void setPlatform(final String platform) {
        this.platform = platform;
    }

    public String getOauthCallbackUrl() {
        return oauthCallbackUrl;
    }

    public void setOauthCallbackUrl(final String oauthCallbackUrl) {
        this.oauthCallbackUrl = oauthCallbackUrl;
    }

    public String getOauthScope() {
        return oauthScope;
    }

    public void setOauthScope(final String oauthScope) {
        this.oauthScope = oauthScope;
    }

    public String getOauthType() {
        return oauthType;
    }

    public void setOauthType(final String oauthType) {
        this.oauthType = oauthType;
    }

    public Date getLastUpdate() {
        return lastUpdate;
    }

    public void setLastUpdate(Date lastUpdate) {
        this.lastUpdate = lastUpdate;
    }

    public String getAccountPlanMappingId() {
        return accountPlanMappingId;
    }

    public void setAccountPlanMappingId(final String accountPlanMappingId) {
        this.accountPlanMappingId = accountPlanMappingId;
    }

    public String getAccountPlanMappingName() {
        return accountPlanMappingName;
    }

    public void setAccountPlanMappingName(final String accountPlanMappingName) {
        this.accountPlanMappingName = accountPlanMappingName;
    }

    //    public String getXmlRepresentation() {
//        return xmlRepresentation;
//    }
//
//    public void setXmlRepresentation(String xmlRepresentation) {
//        this.xmlRepresentation = xmlRepresentation;
//    }

    public String getCustomMetaData() {
        return customMetaData;
    }

    public void setCustomMetaData(String customMetaData) {
        this.customMetaData = customMetaData;
    }

    public String getApplicationId() {
        return applicationId;
    }

    public void setApplicationId(String applicationId) {
        this.applicationId = applicationId;
    }

    @Override
    public ApiKey getReadOnlyCopy() {
        final ApiKey readOnly = new ApiKey();
        copyBaseFields(this, readOnly);
        readOnly.setName(this.getName());
        readOnly.setServiceIds(new HashMap<String, String>(this.getServiceIds()));
        readOnly.setSecret(this.getSecret());
        readOnly.setStatus(this.getStatus());
        readOnly.setLabel(this.getLabel());
        readOnly.setPlatform(this.getPlatform());
        readOnly.setOauthCallbackUrl(this.getOauthCallbackUrl());
        readOnly.setOauthScope(this.getOauthScope());
        readOnly.setOauthType(this.getOauthType());
        readOnly.setLastUpdate(this.getLastUpdate());
        readOnly.setAccountPlanMappingId(this.getAccountPlanMappingId());
//        readOnly.setXmlRepresentation(this.getXmlRepresentation());
        readOnly.setCustomMetaData(this.getCustomMetaData());
        readOnly.setApplicationId(this.getApplicationId());
        readOnly.lock();
        return readOnly;
    }
}
