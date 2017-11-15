package com.l7tech.external.assertions.portaldeployer.server.client;

/**
 * @author raqri01, 2017-10-31
 */
public class CallbackDetailedDto extends CallbackDto {
  private String targetLocation;

  public String getTargetLocation() {
    return targetLocation;
  }

  public void setTargetLocation(String targetLocation) {
    this.targetLocation = targetLocation;
  }
}
