package com.l7tech.server.event.system;

import java.util.logging.Level;

import com.l7tech.common.Component;

/**
 * Event generated by the Policy download service.
 *
 * @author $Author$
 * @version $Revision$
 */
public class PolicyServiceEvent extends SystemEvent {

    //- PUBLIC

    public PolicyServiceEvent(
            Object source,
            Level level,
            String ip,
            String message,
            long identityProviderOid,
            String userName,
            String userId) {
        super(source, Component.GW_POLICY_SERVICE, ip, level, message, identityProviderOid, userName, userId);
    }

    public String getAction() {
        return NAME;
    }

    //- PRIVATE

    private static final String NAME = "PolicyRequested";

}
