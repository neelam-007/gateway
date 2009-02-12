package com.l7tech.server.ems.gateway;

import com.l7tech.identity.User;
import com.l7tech.objectmodel.FindException;
import com.l7tech.server.DefaultKey;
import com.l7tech.server.ems.user.UserPropertyManager;
import com.l7tech.server.ems.enterprise.SsgNode;
import com.l7tech.util.Config;

import java.util.Map;

/**
 * 
 */
public class GatewayContextFactoryImpl implements GatewayContextFactory {

    //- PUBLIC

    public GatewayContextFactoryImpl( final Config config,
                                      final DefaultKey defaultKey,
                                      final UserPropertyManager propertyManager ) {
        this.config = config;
        this.defaultKey = defaultKey;
        this.propertyManager = propertyManager;
    }

    @Override
    public GatewayContext createGatewayContext( final User user, final String clusterId, final String clusterSslHostname, final int gatewayPort ) throws GatewayException {
        return new GatewayContext( defaultKey, clusterSslHostname, gatewayPort, config.getProperty("em.server.id", ""), user==null ? null : getUserUuid(user, clusterId) );
    }

    @Override
    public ProcessControllerContext createProcessControllerContext(SsgNode node) throws GatewayException {
        return new ProcessControllerContext( defaultKey, node.getIpAddress(), node.getProcessControllerPort(), config.getProperty("em.server.id", ""));
    }

    //- PRIVATE

    private final Config config;
    private final DefaultKey defaultKey;
    private final UserPropertyManager propertyManager;

    private String getUserUuid( final User user, final String clusterId ) throws GatewayException {
        try {
            Map<String,String> properties =  propertyManager.getUserProperties(user);
            if ( !properties.containsKey( GatewayConsts.PROP_USER_UUID ) ||
                 (clusterId != null && !properties.containsKey("cluster." +  clusterId + ".trusteduser")) ) {
                throw new GatewayNotMappedException( "Missing UUID for user '"+user.getLogin()+"'.");
            }

            return properties.get( GatewayConsts.PROP_USER_UUID );
        } catch ( FindException fe ) {
            throw new GatewayException( "Error getting UUID for user '"+user.getLogin()+"'.", fe );
        }
    }
}
