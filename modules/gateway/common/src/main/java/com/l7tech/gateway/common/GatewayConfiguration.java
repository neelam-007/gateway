package com.l7tech.gateway.common;

import java.io.Serializable;

/**
 * Gateway configuration bean which holds information required for assertion configuration.
 *
 * @author alee
 */
public class GatewayConfiguration implements Serializable{
    private int uuidAmountMax;

    public int getUuidAmountMax() {
        return uuidAmountMax;
    }

    public void setUuidAmountMax(final int uuidAmountMax) {
        this.uuidAmountMax = uuidAmountMax;
    }
}
