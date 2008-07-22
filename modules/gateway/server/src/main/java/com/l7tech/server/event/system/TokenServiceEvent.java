package com.l7tech.server.event.system;

import java.util.logging.Level;

import com.l7tech.gateway.common.Component;

/**
 * Event class for use by the STS.
 *
 * @author $Author$
 * @version $Revision$
 */
public class TokenServiceEvent extends SystemEvent {

    //- PUBLIC

    public TokenServiceEvent(
            Object source,
            Level level,
            String ip,
            String message,
            long identityProviderOid,
            String userName,
            String userId) {
        super(source, Component.GW_TOKEN_SERVICE, ip, level, message, identityProviderOid, userName, userId);
    }

    public String getAction() {
        return NAME;
    }

    //- PRIVATE
    
    private static final String NAME = "Token Request";
}
