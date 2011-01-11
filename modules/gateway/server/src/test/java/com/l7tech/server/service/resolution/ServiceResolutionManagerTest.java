package com.l7tech.server.service.resolution;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.message.Message;
import com.l7tech.server.audit.Auditor;
import com.l7tech.server.audit.LogOnlyAuditor;
import com.l7tech.server.service.PersistentServiceDocumentWsdlStrategy;
import com.l7tech.server.service.ServiceDocumentManagerStub;
import static org.junit.Assert.*;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.logging.Logger;

/**
 * Tests for service resolution.
 */
public class ServiceResolutionManagerTest {

    @Test
    public void testResolutionEmptyBody() throws Exception {
        Message message = new Message( XmlUtil.parse( "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\"><soapenv:Body/></soapenv:Envelope>" ),0);
        PublishedService service = resolutionManager.resolve( auditor, message, null, services );
        assertNotNull( "Service null (not resolved)", service );
        assertEquals( "Service id", 1, service.getOid() );
    }

    @BeforeClass
    public static void init() {
        Auditor.AuditorFactory auditFactory = new Auditor.AuditorFactory(){
            @Override
            public Auditor newInstance( final Object source, final Logger logger ) {
                return new LogOnlyAuditor( logger );
            }
        };

        resolutionManager = new ServiceResolutionManager(
            Arrays.<ServiceResolver>asList(
                new ServiceOidResolver(auditFactory),
                new UriResolver(auditFactory),
                new SoapActionResolver(auditFactory),
                new UrnResolver(auditFactory)
            ), Arrays.<ServiceResolver>asList(
                new SoapOperationResolver(auditFactory, new ServiceDocumentManagerStub())
        ));

        Collection<PublishedService> services = new ArrayList<PublishedService>();
        services.add( service(1, "EmptyOpService", "/empty", true, false, EMPTY_OPERATION_REQUEST_WSDL) );
        ServiceResolutionManagerTest.services = Collections.unmodifiableCollection( services );

        PersistentServiceDocumentWsdlStrategy.setServiceDocumentManager( new ServiceDocumentManagerStub() );
        for ( final PublishedService service : services ) {
            resolutionManager.notifyServiceCreated( auditor, service );            
        }
    }

    //- PRIVATE

    private static final Logger logger = Logger.getLogger( ServiceResolutionManagerTest.class.getName() );
    private static final Auditor auditor = new LogOnlyAuditor(logger);

    private static Collection<PublishedService> services;
    private static ServiceResolutionManager resolutionManager;

    private static PublishedService service( final long oid, final String name, final String uri, final boolean soap, final boolean lax, final String wsdl ) {
        final PublishedService service = new PublishedService();
        service.setOid( oid );
        service.setName( name );
        service.setRoutingUri( uri );
        service.setSoap( soap );
        service.setLaxResolution( lax );
        service.setWsdlXml( wsdl );
        return service;
    }

    private static final String EMPTY_OPERATION_REQUEST_WSDL =
        "<wsdl:definitions \n" +
        "    targetNamespace=\"urn:empty\" \n" +
        "    xmlns:tns=\"urn:empty\" \n" +
        "    xmlns:wsa=\"http://schemas.xmlsoap.org/ws/2004/08/addressing\" \n" +
        "    xmlns:wsdl=\"http://schemas.xmlsoap.org/wsdl/\" \n" +
        "    xmlns:wsoap12=\"http://schemas.xmlsoap.org/wsdl/soap12/\"\n" +
        "    xmlns:xs=\"http://www.w3.org/2001/XMLSchema\">\n" +
        " \n" +
        "  <wsdl:message name=\"EmptyMessage\"/>\n" +
        " \n" +
        "  <wsdl:portType name=\"EmptyPortType\">\n" +
        "    <wsdl:operation name=\"Empty\">\n" +
        "      <wsdl:input \n" +
        "        message=\"tns:EmptyMessage\"\n" +
        "        wsa:Action=\"urn:empty:empty\" />\n" +
        "      <wsdl:output \n" +
        "        message=\"tns:EmptyMessage\"\n" +
        "        wsa:Action=\"urn:empty:emptyResponse\" />\n" +
        "    </wsdl:operation>\n" +
        "  </wsdl:portType>\n" +
        " \n" +
        "  <wsdl:binding name=\"EmptyBinding\" type=\"tns:EmptyPortType\">\n" +
        "    <wsoap12:binding transport=\"http://schemas.xmlsoap.org/soap/http\" style=\"document\"/>\n" +
        "    <wsdl:operation name=\"Empty\">\n" +
        "      <wsoap12:operation soapAction=\"urn:empty:empty\"/>\n" +
        "      <wsdl:input>\n" +
        "        <wsoap12:body use=\"literal\"/>\n" +
        "      </wsdl:input>\n" +
        "      <wsdl:output>\n" +
        "        <wsoap12:body use=\"literal\"/>\n" +
        "      </wsdl:output>\n" +
        "    </wsdl:operation>\n" +
        "  </wsdl:binding>\n" +
        "</wsdl:definitions>";
}
