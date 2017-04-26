package com.l7tech.external.assertions.apiportalintegration.server.resource;

import org.codehaus.jackson.annotate.JsonProperty;

import java.util.ArrayList;
import java.util.List;

public class ApiV2Entity {

  private String uuid;
  private String name;
  private Boolean serviceEnabled;
  private Boolean publishedByPortal;
  private String ssgUrl;
  private String apiLocationUrl;
  private List<CustomFieldValueEntity> customFieldValueEntities = new ArrayList<>();
  private List<PolicyEntity> policyEntities = new ArrayList<>();

  @JsonProperty( value = "Name")
  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  @JsonProperty( value = "SsgUrl")
  public String getSsgUrl() {
    return ssgUrl;
  }

  public void setSsgUrl(String ssgUrl) {
    this.ssgUrl = ssgUrl;
  }

  @JsonProperty( value = "Uuid")
  public String getUuid() {
    return uuid;
  }

  public void setUuid(String uuid) {
    this.uuid = uuid;
  }

  public Boolean getServiceEnabled() {
    return serviceEnabled;
  }

  public void setServiceEnabled(Boolean serviceEnabled) {
    this.serviceEnabled = serviceEnabled;
  }

  @JsonProperty( value = "PublishByPortal")
  public Boolean getPublishedByPortal() {
    return publishedByPortal;
  }

  public void setPublishedByPortal(Boolean publishedByPortal) {
    this.publishedByPortal = publishedByPortal;
  }

  @JsonProperty( value = "ApiLocationUrl")
  public String getApiLocationUrl() {
    return apiLocationUrl;
  }

  public void setApiLocationUrl(String apiLocationUrl) {
    this.apiLocationUrl = apiLocationUrl;
  }

  @JsonProperty( value = "CustomFields")
  public List<CustomFieldValueEntity> getCustomFieldValueEntities() {
    return customFieldValueEntities;
  }

  public void setCustomFieldValueEntities(List<CustomFieldValueEntity> customFieldValueEntities) {
    this.customFieldValueEntities = customFieldValueEntities;
  }

  @JsonProperty( value = "PolicyEntities")
  public List<PolicyEntity> getPolicyEntities() {
    return policyEntities;
  }

  public void setPolicyEntities(List<PolicyEntity> policyEntities) {
    this.policyEntities = policyEntities;
  }
}
