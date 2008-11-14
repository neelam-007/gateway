package com.l7tech.server.ems.gateway;

import com.l7tech.identity.User;
import org.springframework.transaction.annotation.Transactional;

/**
 * Factory for generation of trust tokens. 
 */
@Transactional
public interface GatewayTrustTokenFactory {

    /**
     * Get a trust token for use by the given user.
     *
     * @param user The user establishing trust.
     * @return The trust token
     */
    String getTrustToken( User user ) throws GatewayException;

    /**
     * Get a trust token for use by the authenticated user.
     *
     * @return The trust token
     */
    String getTrustToken() throws GatewayException;
}
