package com.l7tech.external.assertions.apiportalintegration.server.resource;

import java.util.ArrayList;
import java.util.List;

/**
 * @author chean22, 1/22/2016
 */
public class ApplicationEntity {
    private String id;
    private String key;
    private String secret;
    private String status;
    private String organizationId;
    private String organizationName;
    private String label;
    private String oauthCallbackUrl;
    private String oauthScope;
    private String oauthType;
    private List<ApplicationApi> apis = new ArrayList<>();

    public ApplicationEntity() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
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

    public String getOrganizationId() {
        return organizationId;
    }

    public void setOrganizationId(String organizationId) {
        this.organizationId = organizationId;
    }

    public String getOrganizationName() {
        return organizationName;
    }

    public void setOrganizationName(String organizationName) {
        this.organizationName = organizationName;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
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

    public List<ApplicationApi> getApis() {
        return apis;
    }

    public void setApis(List<ApplicationApi> apis) {
        this.apis = apis;
    }
}
