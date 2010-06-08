package com.l7tech.server.admin.ws;

import com.l7tech.common.io.ByteLimitInputStream;
import com.l7tech.util.SyspropUtil;
import org.apache.cxf.interceptor.AttachmentInInterceptor;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;

import java.io.BufferedInputStream;
import java.io.InputStream;

/**
 * 
 */
public class StreamValidationInterceptor extends AbstractPhaseInterceptor<Message> {

    //- PUBLIC

    public StreamValidationInterceptor() {
        super( Phase.RECEIVE );
        addBefore( AttachmentInInterceptor.class.getName() );
    }

    @SuppressWarnings({ "ThrowableInstanceNeverThrown" })
    @Override
    public void handleMessage( final Message message ) throws Fault {
        if( isGET(message) ) {
            return;
        }

        // Limit content size
        final InputStream in = message.getContent( InputStream.class );
        final BufferedInputStream limited;
        if ( in != null && limit > 0 ) {
            limited = new BufferedInputStream( new ByteLimitInputStream( in, 32, limit ), dtdLimit );
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

    private static final int limit = SyspropUtil.getInteger( "com.l7tech.server.admin.ws.requestSizeLimit", 4*1024*1024 );
    private static final int dtdLimit = SyspropUtil.getInteger( "com.l7tech.server.admin.ws.dtdLimit", 4096 );

}