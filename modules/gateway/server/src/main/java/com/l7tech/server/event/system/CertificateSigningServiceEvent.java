package com.l7tech.server.event.system;

import java.util.logging.Level;

import com.l7tech.gateway.common.Component;

/**
 * Event class for use by the CSR Handler.
 */
public class CertificateSigningServiceEvent extends SystemEvent {

    //- PUBLIC

    public CertificateSigningServiceEvent(
            Object source,
            Level level,
            String ip,
            String message,
            long identityProviderOid,
            String userName,
            String userId) {
        super(source, Component.GW_CSR_SERVLET, ip, level, message, identityProviderOid, userName, userId);
    }

    public String getAction() {
        return NAME;
    }

    //- PRIVATE

    private static final String NAME = "Signed Client Certificate";
}