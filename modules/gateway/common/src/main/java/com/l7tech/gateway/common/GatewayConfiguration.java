package com.l7tech.gateway.common;

import java.io.Serializable;

/**
 * Gateway configuration bean which holds information required for assertion configuration.
 *
 * @author alee
 */
public class GatewayConfiguration implements Serializable{
    private int uuidQuantityMax;

    public int getUuidQuantityMax() {
        return uuidQuantityMax;
    }

    public void setUuidQuantityMax(final int uuidQuantityMax) {
        this.uuidQuantityMax = uuidQuantityMax;
    }
}
