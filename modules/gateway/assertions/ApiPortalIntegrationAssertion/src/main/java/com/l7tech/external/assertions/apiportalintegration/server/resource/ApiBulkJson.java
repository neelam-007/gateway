package com.l7tech.external.assertions.apiportalintegration.server.resource;

import org.codehaus.jackson.annotate.JsonProperty;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by karra14 on 2017-04-25.
 */
public class ApiBulkJson {

  private List<ApiEntity> apis = new ArrayList<>();

  @JsonProperty(value = "apis")
  public List<ApiEntity> getApis() {
    return apis;
  }

  public void setApis(List<ApiEntity> apis) {
    this.apis = apis;
  }
}
