package com.l7tech.external.assertions.apiportalintegration.server.resource;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.map.ObjectMapper;

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
  private List<ApplicationApi> apis;
  private ApplicationMag mag;
  private Map<String, String> customFields;
  private String createdBy;
  private String modifiedBy;

  public ApplicationEntity() {
    apis = new ArrayList<>();
    customFields = new HashMap<>();
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

  public ApplicationMag getMag() {
    return mag;
  }

  public void setMag(ApplicationMag mag) {
    this.mag = mag;
  }

  public String getCustom() throws IOException {
    ObjectMapper mapper = new ObjectMapper();
    return mapper.writeValueAsString(customFields);
  }

  public void setCustom(String custom) {
    //DO NOTHING
  }

  @JsonIgnore
  public Map<String, String> getCustomFields() {
    return customFields;
  }

  public void setCustomFields(Map<String, String> customFields) {
    this.customFields = customFields;
  }

  public String getCreatedBy() {
    return createdBy;
  }

  public void setCreatedBy(String createdBy) {
    this.createdBy = createdBy;
  }

  public String getModifiedBy() {
    return modifiedBy;
  }

  public void setModifiedBy(String modifiedBy) {
    this.modifiedBy = modifiedBy;
  }
}
