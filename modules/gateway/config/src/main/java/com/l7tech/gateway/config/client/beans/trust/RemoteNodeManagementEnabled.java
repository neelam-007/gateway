/**
 * Copyright (C) 2008 Layer 7 Technologies Inc.
 */
package com.l7tech.gateway.config.client.beans.trust;

import com.l7tech.gateway.config.client.beans.BooleanConfigurableBean;

/** @author alex */
public class RemoteNodeManagementEnabled extends BooleanConfigurableBean {

    public RemoteNodeManagementEnabled( boolean value ) {
        super("host.controller.remoteNodeManagement.enabled", "Remote Node Management Enabled", false);
        this.configValue = value;
    }
}
