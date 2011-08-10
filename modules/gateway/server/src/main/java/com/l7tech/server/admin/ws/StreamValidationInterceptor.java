package com.l7tech.server.admin.ws;

import com.l7tech.common.io.ByteLimitInputStream;
import com.l7tech.util.Config;
import com.l7tech.util.ConfigFactory;
import com.l7tech.util.ValidatedConfig;
import org.apache.cxf.interceptor.AttachmentInInterceptor;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;

import javax.inject.Inject;
import java.io.BufferedInputStream;
import java.io.InputStream;
import java.util.logging.Logger;

/**
 * 
 */
public class StreamValidationInterceptor extends AbstractPhaseInterceptor<Message> {

    //- PUBLIC

    @Inject
    public StreamValidationInterceptor( final Config config ) {
        super( Phase.RECEIVE );
        addBefore( AttachmentInInterceptor.class.getName() );
        this.config = validated(config);
    }

    @Override
    public void handleMessage( final Message message ) throws Fault {
        if( isGET(message) ) {
            return;
        }

        // Limit content size
        final InputStream in = message.getContent( InputStream.class );
        final BufferedInputStream limited;
        if ( in != null && getRequestSizeLimit() > 0 ) {
            limited = new BufferedInputStream( new ByteLimitInputStream( in, 32, getRequestSizeLimit() ), dtdLimit );
        } else if ( in != null ) {
            limited = new BufferedInputStream( in, dtdLimit );
        } else {
            limited = null;
        }

        if ( limited != null ) {
            message.setContent( InputStream.class, limited );
        }
    }

    //- PRIVATE

    private static final Logger logger = Logger.getLogger( StreamValidationInterceptor.class.getName() );

    private static final int DEFAULT_REQUEST_SIZE_LIMIT = 10 * 1024 * 1024;
    private static final String PROP_ESM_REQUEST_SIZE_LIMIT = "admin.esmRequestSizeLimit";
    private static final int dtdLimit = ConfigFactory.getIntProperty( "com.l7tech.server.admin.ws.dtdLimit", 4096 );

    private final Config config;

    private int getRequestSizeLimit() {
        return config.getIntProperty( PROP_ESM_REQUEST_SIZE_LIMIT, DEFAULT_REQUEST_SIZE_LIMIT );
    }

    private Config validated( final Config config ) {
        final ValidatedConfig validatedConfig = new ValidatedConfig( config, logger );

        validatedConfig.setMinimumValue( PROP_ESM_REQUEST_SIZE_LIMIT, 0 );

        return validatedConfig;
    }
}
