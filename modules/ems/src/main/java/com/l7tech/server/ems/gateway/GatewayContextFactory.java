package com.l7tech.server.ems.gateway;

import com.l7tech.server.DefaultKey;
import com.l7tech.server.ems.user.UserPropertyManager;
import com.l7tech.util.Config;
import com.l7tech.identity.User;
import com.l7tech.objectmodel.FindException;

import java.util.Map;

/**
 * 
 */
public class GatewayContextFactory {

    //- PUBLIC

    public GatewayContextFactory( final Config config,
                                  final DefaultKey defaultKey,
                                  final UserPropertyManager propertyManager ) {
        this.config = config;
        this.defaultKey = defaultKey;
        this.propertyManager = propertyManager;
    }

    public GatewayContext getGatewayContext( final User user, final String host, final int port ) throws GatewayException {
        return new GatewayContext( defaultKey, host, port, config.getProperty("em.server.id", ""), getUserUuid(user) );
    }

    //- PRIVATE

    private final Config config;
    private final DefaultKey defaultKey;
    private final UserPropertyManager propertyManager;

    private String getUserUuid( final User user ) throws GatewayException {
        try {
            Map<String,String> properties =  propertyManager.getUserProperties(user);
            if ( !properties.containsKey( GatewayConsts.PROP_USER_UUID ) ) {
                throw new GatewayException( "Missing UUID for user '"+user.getLogin()+"'.");
            }

            return properties.get( GatewayConsts.PROP_USER_UUID );
        } catch ( FindException fe ) {
            throw new GatewayException( "Error getting UUID for user '"+user.getLogin()+"'.", fe );
        }
    }
}
