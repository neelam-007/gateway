package com.l7tech.policy.validator;

import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.policy.assertion.SetVariableAssertion;
import com.l7tech.policy.variable.DataType;
import com.l7tech.util.Charsets;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.ResourceUtils;

import javax.xml.stream.Location;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLReporter;
import javax.xml.stream.XMLResolver;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.Charset;

/**
 * Policy validator for the set context variable assertion.
 */
public class SetVariableAssertionValidator  extends AssertionValidatorSupport<SetVariableAssertion> {

    //- PUBLIC

    public SetVariableAssertionValidator( final SetVariableAssertion assertion ) {
        super(assertion);

        try {
            if ( assertion.isEnabled() && assertion.getDataType()==DataType.MESSAGE && contentTypeConflicts( assertion ) ) {
                this.addWarningMessage( "The character encoding in the Content-Type header does not match the encoding in the XML declaration." );   
            }
        } catch ( IOException e ) {
            this.addWarningMessage( "Invalid Content-Type: " + ExceptionUtils.getMessage(e) );
        }
    }

    //- PRIVATE

    private static final XMLReporter SILENT_REPORTER = new XMLReporter() {
        @Override
        public void report( final String message, final String errorType, final Object relatedInformation, final Location location ) throws XMLStreamException {
            throw new XMLStreamException(message, location);
        }
    };

    private static final XMLResolver FAILING_RESOLVER = new XMLResolver() {
        @Override
        public Object resolveEntity( final String publicID, final String systemID, final String baseURI, final String namespace ) throws XMLStreamException {
            throw new XMLStreamException("External entity access forbidden '"+systemID+"' relative to '"+baseURI+"'.");
        }
    };

    private static boolean contentTypeConflicts( final SetVariableAssertion assertion ) throws IOException {
        boolean conflict = false;

        final ContentTypeHeader contentType = ContentTypeHeader.parseValue( assertion.getContentType() );
        if ( assertion.getDataType() == DataType.MESSAGE && contentType.isXml() ) {
            final String headerEncoding = contentType.getEncoding().name();
            final String messageEncoding = getEncoding( new StreamSource(new StringReader(assertion.expression())) );

            conflict = !headerEncoding.equals( messageEncoding );
        }

        return conflict;
    }

    private static String getEncoding( final Source source ) {
        String encoding = null;

        final XMLInputFactory xif = XMLInputFactory.newInstance();
        xif.setXMLReporter( SILENT_REPORTER );
        xif.setXMLResolver( FAILING_RESOLVER );
        XMLStreamReader reader = null;
        try {
            reader = xif.createXMLStreamReader( source );
            while( reader.hasNext() ) {
                final int eventType = reader.next();
                if ( eventType == XMLStreamReader.START_DOCUMENT ||
                     eventType == XMLStreamReader.START_ELEMENT ) {
                    encoding = reader.getCharacterEncodingScheme();
                    break;
                }
            }
        } catch ( XMLStreamException e ) {
            // use default encoding.
        } finally {
            ResourceUtils.closeQuietly( reader );
        }

        if ( encoding == null ) {
            encoding = Charsets.UTF8.name();
        }

        // normalize the name
        try {
            encoding = Charset.forName( encoding ).name();
        } catch ( IllegalArgumentException e ) {
            // use default encoding
        }

        return encoding;
    }
}
