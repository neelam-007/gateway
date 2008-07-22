package com.l7tech.server.event.system;

import com.l7tech.gateway.common.Component;

import java.util.logging.Level;

/**
 * [todo jdoc this class]
 * <p/>
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Aug 20, 2007<br/>
 */
public class FailedAdminLoginEvent extends SystemEvent {
    public FailedAdminLoginEvent(Object source, String ip, String msg) {
        super(source, Component.GATEWAY, ip, Level.WARNING, msg);
    }

    public String getAction() {
        return NAME;
    }

    private static final String NAME = "Failed Admin Login";
}
