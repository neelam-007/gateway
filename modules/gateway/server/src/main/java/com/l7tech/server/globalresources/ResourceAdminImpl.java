package com.l7tech.server.globalresources;

import com.l7tech.gateway.common.resources.HttpConfiguration;
import com.l7tech.gateway.common.resources.HttpProxyConfiguration;
import com.l7tech.gateway.common.resources.ResourceAdmin;
import com.l7tech.objectmodel.DeleteException;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.SaveException;
import com.l7tech.objectmodel.UpdateException;

import java.util.Collection;

/**
 * Resource administration implementation
 */
public class ResourceAdminImpl implements ResourceAdmin {

    //- PUBLIC

    public ResourceAdminImpl( final DefaultHttpProxyManager defaultHttpProxyManager,
                              final HttpConfigurationManager httpConfigurationManager ) {
        this.defaultHttpProxyManager = defaultHttpProxyManager;
        this.httpConfigurationManager = httpConfigurationManager;    
    }

    @SuppressWarnings({ "unchecked" })
    @Override
    public HttpProxyConfiguration getDefaultHttpProxyConfiguration() throws FindException {
        return defaultHttpProxyManager.getDefaultHttpProxyConfiguration();
    }

    @Override
    public void setDefaultHttpProxyConfiguration( final HttpProxyConfiguration httpProxyConfiguration ) throws SaveException, UpdateException {
        defaultHttpProxyManager.setDefaultHttpProxyConfiguration( httpProxyConfiguration );
    }

    @Override
    public Collection<HttpConfiguration> findAllHttpConfigurations() throws FindException {
        return httpConfigurationManager.findAll();
    }

    @Override
    public HttpConfiguration findHttpConfigurationByPrimaryKey( final long oid ) throws FindException {
        return httpConfigurationManager.findByPrimaryKey( oid );
    }

    @Override
    public void deleteHttpConfiguration( final HttpConfiguration httpConfiguration ) throws DeleteException {
        httpConfigurationManager.delete( httpConfiguration );
    }

    @Override
    public long saveHttpConfiguration( final HttpConfiguration httpConfiguration ) throws SaveException, UpdateException {
        final long oid;

        if ( httpConfiguration.getOid() == HttpConfiguration.DEFAULT_OID ) {
            oid = httpConfigurationManager.save( httpConfiguration );
        } else {
            oid = httpConfiguration.getOid();
            httpConfigurationManager.update( httpConfiguration );
        }

        return oid;
    }

    //- PRIVATE

    private final DefaultHttpProxyManager defaultHttpProxyManager;
    private final HttpConfigurationManager httpConfigurationManager;
}
