package com.l7tech.external.assertions.apiportalintegration.server.resource;

import org.codehaus.jackson.annotate.JsonProperty;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by karra14 on 2017-04-25.
 */
public class ApiV2BulkJson {

  private List<ApiV2Entity> apis = new ArrayList<>();

  @JsonProperty(value = "api2.0s")
  public List<ApiV2Entity> getApis() {
    return apis;
  }

  public void setApis(List<ApiV2Entity> apis) {
    this.apis = apis;
  }
}
