package com.l7tech.external.assertions.apiportalintegration.server;

import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public class ApiKeyData extends AbstractPortalGenericEntity {
    /**
     * Key = service id, Value = plan id.
     */
    private Map<String, String> serviceIds;
    private String secret;
    private String status;
    private String xmlRepresentation;
    private String label;
    private String platform;
    private String oauthCallbackUrl;
    private String oauthScope;
    private String oauthType;
    private String accountPlanMappingId;
    private String customMetaData;

    public ApiKeyData() {
        this(null);
    }

    public ApiKeyData(@Nullable String xmlRepresentation) {
        this.serviceIds = new HashMap<String, String>();
        this.xmlRepresentation = xmlRepresentation;
    }

    public String getKey() {
        return getName();
    }

    public void setKey(final String key) {
        setName(key);
    }

    public Map<String, String> getServiceIds() {
        return serviceIds;
    }

    public void setServiceIds(final Map<String, String> serviceIds) {
        this.serviceIds = serviceIds;
    }

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getXmlRepresentation() {
        return xmlRepresentation;
    }

    public void setXmlRepresentation(String xmlRepresentation) {
        this.xmlRepresentation = xmlRepresentation;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getPlatform() {
        return platform;
    }

    public void setPlatform(String platform) {
        this.platform = platform;
    }

    public String getOauthCallbackUrl() {
        return oauthCallbackUrl;
    }

    public void setOauthCallbackUrl(String oauthCallbackUrl) {
        this.oauthCallbackUrl = oauthCallbackUrl;
    }

    public String getOauthScope() {
        return oauthScope;
    }

    public void setOauthScope(String oauthScope) {
        this.oauthScope = oauthScope;
    }

    public String getOauthType() {
        return oauthType;
    }

    public void setOauthType(String oauthType) {
        this.oauthType = oauthType;
    }

    public String getAccountPlanMappingId() {
        return accountPlanMappingId;
    }

    public void setAccountPlanMappingId(final String accountPlanMappingId) {
        this.accountPlanMappingId = accountPlanMappingId;
    }

    public String getCustomMetaData() {
        return customMetaData;
    }

    public void setCustomMetaData(String customMetaData) {
        this.customMetaData = customMetaData;
    }

    @Override
    public ApiKeyData getReadOnlyCopy() {
        final ApiKeyData readOnly = new ApiKeyData();
        copyBaseFields(this, readOnly);
        readOnly.setKey(this.getKey());
        readOnly.setServiceIds(new HashMap<String, String>(this.getServiceIds()));
        readOnly.setSecret(this.getSecret());
        readOnly.setStatus(this.getStatus());
        readOnly.setXmlRepresentation(this.getXmlRepresentation());
        readOnly.setLabel(this.getLabel());
        readOnly.setPlatform(this.getPlatform());
        readOnly.setOauthCallbackUrl(this.getOauthCallbackUrl());
        readOnly.setOauthScope(this.getOauthScope());
        readOnly.setOauthType(this.getOauthType());
        readOnly.setAccountPlanMappingId(this.getAccountPlanMappingId());
        readOnly.setCustomMetaData(this.getCustomMetaData());
        readOnly.lock();
        return readOnly;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        ApiKeyData that = (ApiKeyData) o;

        if (_name != null ? !_name.equals(that._name) : that._name != null) return false;
        if (label != null ? !label.equals(that.label) : that.label != null) return false;
        if (oauthCallbackUrl != null ? !oauthCallbackUrl.equals(that.oauthCallbackUrl) : that.oauthCallbackUrl != null)
            return false;
        if (oauthScope != null ? !oauthScope.equals(that.oauthScope) : that.oauthScope != null) return false;
        if (oauthType != null ? !oauthType.equals(that.oauthType) : that.oauthType != null) return false;
        if (platform != null ? !platform.equals(that.platform) : that.platform != null) return false;
        if (secret != null ? !secret.equals(that.secret) : that.secret != null) return false;
        if (serviceIds != null ? !serviceIds.equals(that.serviceIds) : that.serviceIds != null) return false;
        if (status != null ? !status.equals(that.status) : that.status != null) return false;
        if (xmlRepresentation != null ? !xmlRepresentation.equals(that.xmlRepresentation) : that.xmlRepresentation != null)
            return false;
        if (accountPlanMappingId != null ? !accountPlanMappingId.equals(that.accountPlanMappingId) : that.accountPlanMappingId != null) return false;
        if (customMetaData != null ? !customMetaData.equals(that.customMetaData) : that.customMetaData != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (_name != null ? _name.hashCode() : 0);
        result = 31 * result + (serviceIds != null ? serviceIds.hashCode() : 0);
        result = 31 * result + (secret != null ? secret.hashCode() : 0);
        result = 31 * result + (status != null ? status.hashCode() : 0);
        result = 31 * result + (xmlRepresentation != null ? xmlRepresentation.hashCode() : 0);
        result = 31 * result + (label != null ? label.hashCode() : 0);
        result = 31 * result + (platform != null ? platform.hashCode() : 0);
        result = 31 * result + (oauthCallbackUrl != null ? oauthCallbackUrl.hashCode() : 0);
        result = 31 * result + (oauthScope != null ? oauthScope.hashCode() : 0);
        result = 31 * result + (oauthType != null ? oauthType.hashCode() : 0);
        result = 31 * result + (accountPlanMappingId != null ? accountPlanMappingId.hashCode() : 0);
        result = 31 * result + (customMetaData != null ? customMetaData.hashCode() : 0);
        return result;
    }
}
