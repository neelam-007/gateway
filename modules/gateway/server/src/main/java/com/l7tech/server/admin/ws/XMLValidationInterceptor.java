package com.l7tech.server.admin.ws;

import com.l7tech.common.io.ByteLimitInputStream;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.SAXParsingCompleteException;
import com.l7tech.util.SyspropUtil;
import org.apache.cxf.interceptor.AttachmentInInterceptor;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.xml.sax.Attributes;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;
import org.xml.sax.ext.DefaultHandler2;
import org.xml.sax.helpers.XMLReaderFactory;

import java.io.IOException;
import java.io.InputStream;

/**
 *
 */
public class XMLValidationInterceptor extends AbstractPhaseInterceptor<Message> {

    //- PUBLIC

    public XMLValidationInterceptor() {
        super( Phase.RECEIVE );
        addAfter( AttachmentInInterceptor.class.getName() );
    }

    @SuppressWarnings({ "ThrowableInstanceNeverThrown" })
    @Override
    public void handleMessage( final Message message ) throws Fault {
        if( isGET(message) ) {
            return;
        }

        // Check for DOCTYPE
        final InputStream in = message.getContent( InputStream.class );
        if ( in != null ) {
            message.setContent( InputStream.class, in );

            try {
                ensureNoDoctype( in );
            } catch ( SAXException se ) {
                if ( ExceptionUtils.getMessage(se).toLowerCase().contains( "doctype" )) {
                    throw new Fault( new SAXException("Invalid request, DOCTYPE not permitted", se) );
                } else {
                    throw new Fault( new SAXException("Invalid request, " + ExceptionUtils.getMessage(se), se) );
                }
            } catch ( IOException ioe ) {
                throw new Fault( ioe );
            }
        }
    }

    //- PRIVATE

    private static final int dtdLimit = SyspropUtil.getInteger( "com.l7tech.server.admin.ws.dtdLimit", 4096 );

    private void ensureNoDoctype( final InputStream in ) throws SAXException, IOException {
        in.mark( dtdLimit );

        final InputStream pin = new ByteLimitInputStream( in, 32, dtdLimit ) { // limit to the dtdLimit to ensure we can reset
            @Override
            public void close() throws IOException {
                // suppress close since we want to use the stream later
            }
        };

        try {
            final XMLReader reader = XMLReaderFactory.createXMLReader();
            reader.setFeature( "http://xml.org/sax/features/namespaces", true );
            reader.setFeature( XmlUtil.XERCES_DISALLOW_DOCTYPE, true );
            reader.setEntityResolver( XmlUtil.getSafeEntityResolver() );
            reader.setErrorHandler( new SaxErrorHandler() );
            reader.setContentHandler( new DefaultHandler2(){
                @Override
                public void startElement( final String uri, final String localName, final String qName, final Attributes attributes ) throws SAXException {
                    throw new SAXParsingCompleteException();
                }
            } );
            reader.parse( new InputSource( pin ) );
        } catch ( SAXParsingCompleteException e ) {
            // Success, reset stream
            in.reset();
        }
    }

    private static final class SaxErrorHandler implements ErrorHandler {
        @Override
        public void warning( final SAXParseException exception ) throws SAXException {
        }

        @Override
        public void error( final SAXParseException exception ) throws SAXException {
            throw exception;
        }

        @Override
        public void fatalError( final SAXParseException exception ) throws SAXException {
            throw exception;
        }
    }}
