package com.l7tech.server.management.api.node;

import org.junit.Test;
import org.junit.Assert;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.bind.ValidationEventHandler;
import javax.xml.bind.ValidationEvent;
import javax.xml.bind.Unmarshaller;
import javax.activation.DataHandler;
import javax.mail.util.ByteArrayDataSource;
import java.util.Collection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.UUID;
import java.io.StringWriter;
import java.io.StringReader;

import com.l7tech.server.management.api.TypedValue;

/**
 * Tests for Management Report API
 */
public class ReportApiTest {

    @Test
    public void testReportSubmissionSerialization() throws Exception {
        ReportApi.ReportSubmission submission = new ReportApi.ReportSubmission();
        submission.setName("Test name");
        submission.setType(ReportApi.ReportType.PERFORMANCE_INTERVAL);

        Collection<ReportApi.ReportSubmission.ReportParam> params = new ArrayList<ReportApi.ReportSubmission.ReportParam>();
        params.add( new ReportApi.ReportSubmission.ReportParam( "a", "string" ) );
        params.add( new ReportApi.ReportSubmission.ReportParam( "b", Boolean.TRUE ) );
        params.add( new ReportApi.ReportSubmission.ReportParam( "c", 1) );
        params.add( new ReportApi.ReportSubmission.ReportParam( "d", Collections.singleton("stringset") ) );
        params.add( new ReportApi.ReportSubmission.ReportParam( "e", Collections.singletonList("stringlist") ) );
        params.add( new ReportApi.ReportSubmission.ReportParam( "f", Arrays.asList("stringlist1", "stringlist2", "stringlist3") ) );
        params.add( new ReportApi.ReportSubmission.ReportParam( "g", new LinkedHashSet<String>() ) );
        params.add( new ReportApi.ReportSubmission.ReportParam( "h", new ArrayList<String>() ) );
        params.add( new ReportApi.ReportSubmission.ReportParam( "i", Arrays.asList( (String) null, (String) null ) ) );
        params.add( new ReportApi.ReportSubmission.ReportParam( "j", Arrays.asList( "", "" ) ) );
        params.add( new ReportApi.ReportSubmission.ReportParam( "k", Arrays.asList( " ", "  ", "   " ) ) );
        submission.setParameters(params);

        roundTrip( submission );
    }

    @Test
    public void testReportStatusSerialization() throws Exception {
        ReportApi.ReportStatus status = new ReportApi.ReportStatus();
        status.setId("asdf");
        status.setStatus( ReportApi.ReportStatus.Status.COMPLETED );
        status.setTime( System.currentTimeMillis() );

        roundTrip( status );
    }

    @Test
    public void testReportResultSerialization() throws Exception {
        ReportApi.ReportResult result = new ReportApi.ReportResult();
        result.setId( UUID.randomUUID().toString() );
        result.setType( ReportApi.ReportOutputType.PDF );
        result.setData( new DataHandler( new ByteArrayDataSource(new byte[32], "application/octet-stream") ) );

        roundTrip( result );
    }

    private Object roundTrip( Object object ) throws Exception {
        StringWriter out = new StringWriter();
        JAXBContext context = JAXBContext.newInstance( ReportApi.ReportSubmission.class, ReportApi.ReportType.class, ReportApi.ReportStatus.class, ReportApi.ReportResult.class, ReportApi.ReportOutputType.class );
        Marshaller marshaller = context.createMarshaller();
        marshaller.setEventHandler( new ValidationEventHandler(){
            @Override
            public boolean handleEvent( final ValidationEvent event ) {
                System.out.println( event );
                return false;
            }
        } );
        marshaller.setListener( new Marshaller.Listener(){
            @Override
            public void beforeMarshal(Object source) {
                System.out.println( "Marshalling: " + source );
            }
        } );
        marshaller.setProperty( Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE );
        marshaller.marshal( object, out );
        System.out.println( out );

        Unmarshaller unmarshaller = context.createUnmarshaller();
        Object objectRedux = unmarshaller.unmarshal( new StringReader( out.toString() ) );

        Assert.assertTrue( "Roundtrip JAXB equality failed.", object.equals(objectRedux) );

        return objectRedux;
    }

}
