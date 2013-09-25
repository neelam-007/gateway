package com.l7tech.server.globalresources;

import com.l7tech.common.io.NonCloseableOutputStream;
import com.l7tech.gateway.common.resources.HttpProxyConfiguration;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.SaveException;
import com.l7tech.objectmodel.UpdateException;
import com.l7tech.server.cluster.ClusterPropertyManager;
import com.l7tech.util.*;

import java.beans.ExceptionListener;
import java.beans.XMLEncoder;
import java.io.ByteArrayInputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Manager for default HTTP proxy configuration.
 */
public class DefaultHttpProxyManager {

    //- PUBLIC

    public DefaultHttpProxyManager( final ClusterPropertyManager clusterPropertyManager ) {
        this.clusterPropertyManager = clusterPropertyManager;
    }

    public HttpProxyConfiguration getDefaultHttpProxyConfiguration() throws FindException {
        final String httpProxyXml = clusterPropertyManager.getProperty( PROP_IO_HTTP_PROXY );

        HttpProxyConfiguration httpProxyConfiguration = new HttpProxyConfiguration();
        if ( httpProxyXml != null && !httpProxyXml.isEmpty() ) {
            final ByteArrayInputStream in = new ByteArrayInputStream( httpProxyXml.getBytes( Charsets.UTF8));
            final SafeXMLDecoder decoder = new SafeXMLDecoderBuilder(in)
                    .setExceptionListener(new ExceptionListener() {
                        @Override
                        public void exceptionThrown( final Exception e ) {
                            logger.log( Level.WARNING, "Error loading configuration '"+ ExceptionUtils.getMessage(e)+"'.", ExceptionUtils.getDebugException(e));
                        }
                    })
                    .build();
            try {
                final Object httpProxyObject = decoder.readObject();
                if ( httpProxyObject instanceof HttpProxyConfiguration ) {
                    httpProxyConfiguration = (HttpProxyConfiguration) httpProxyObject;
                } else {
                    logger.warning( "Error loading configuration, unexpected data : " + (httpProxyObject==null?"<NULL>":httpProxyObject.getClass().getName()) );
                }
            } catch ( ArrayIndexOutOfBoundsException e ) {
                logger.warning( "Error loading configuration, no data." );
            }
        }

        return httpProxyConfiguration;
    }

    public void setDefaultHttpProxyConfiguration( final HttpProxyConfiguration httpProxyConfiguration ) throws SaveException, UpdateException {
        String value;

        if ( httpProxyConfiguration != null ) {
            PoolByteArrayOutputStream output = null;
            java.beans.XMLEncoder encoder = null;
            try {
                output = new PoolByteArrayOutputStream();
                encoder = new XMLEncoder(new NonCloseableOutputStream(output));
                encoder.setPersistenceDelegate( Goid.class, Goid.getPersistenceDelegate() );
                encoder.setExceptionListener( new ExceptionListener() {
                    @Override
                    public void exceptionThrown( final Exception e ) {
                        logger.log( Level.WARNING, "Error storing configuration '"+ ExceptionUtils.getMessage(e)+"'.", ExceptionUtils.getDebugException(e));
                    }
                });
                encoder.writeObject(httpProxyConfiguration);
                encoder.close(); // writes closing XML tag
                encoder = null;
                value = output.toString(Charsets.UTF8);
            }
            finally {
                if(encoder!=null) encoder.close();
                ResourceUtils.closeQuietly(output);
            }
        } else {
            value = "";
        }

        try {
            clusterPropertyManager.putProperty( PROP_IO_HTTP_PROXY, value );
        } catch ( FindException e ) {
            throw new UpdateException( "Unable to save HTTP proxy configuration.", e );
        }
    }

    //- PRIVATE

    private static final Logger logger = Logger.getLogger(DefaultHttpProxyManager.class.getName());

    private static final String PROP_IO_HTTP_PROXY = "ioHttpProxy";

    private final ClusterPropertyManager clusterPropertyManager;
}
