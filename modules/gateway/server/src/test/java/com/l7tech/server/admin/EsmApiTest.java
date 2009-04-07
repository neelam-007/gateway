package com.l7tech.server.admin;

import org.junit.Test;
import org.junit.BeforeClass;
import org.junit.Assert;
import org.junit.Ignore;
import org.apache.cxf.transport.ConduitInitiatorManager;
import org.apache.cxf.transport.DestinationFactoryManager;
import org.apache.cxf.transport.local.LocalTransportFactory;
import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.binding.soap.interceptor.AbstractSoapInterceptor;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.service.model.BindingOperationInfo;
import org.apache.cxf.service.model.BindingInfo;
import org.apache.cxf.jaxws.JaxWsServerFactoryBean;
import org.apache.cxf.jaxws.JaxWsClientFactoryBean;
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.apache.cxf.interceptor.LoggingOutInterceptor;
import org.apache.cxf.interceptor.Interceptor;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.interceptor.LoggingInInterceptor;
import org.w3c.dom.Node;
import com.l7tech.server.management.api.node.GatewayApi;
import com.l7tech.server.management.api.node.MigrationApi;
import com.l7tech.server.management.api.node.ReportApi;
import com.l7tech.server.management.migration.bundle.MigrationMetadata;
import com.l7tech.server.management.migration.bundle.MigrationBundle;
import com.l7tech.server.management.migration.bundle.MigratedItem;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.ExternalEntityHeader;
import com.l7tech.objectmodel.Entity;
import com.l7tech.util.ResourceUtils;
import com.l7tech.gateway.common.service.PublishedService;

import javax.xml.soap.SOAPMessage;
import javax.xml.soap.MessageFactory;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.stream.XMLStreamException;
import java.util.*;
import java.io.*;

/**
 * Tests for ESM APIs with 1.0 format messages.
 *
 * <p>This includes test for the following Gateway APIs:</p>
 *
 * <ul>
 *   <li>GatewayApi - General Cluster / Node / Entity information</li>
 *   <li>MigrationApi - Policy Migration and dependency discovery</li>
 *   <li>ReportApi - Report Submission and Download</li>
 * </ul>
 *
 * TODO validation of response messages
 */
public class EsmApiTest {

    private ThreadLocal<SOAPMessage> requestMessage = new ThreadLocal<SOAPMessage>();

    @BeforeClass
    public static void setup() {
        System.setProperty("org.apache.cxf.nofastinfoset", "true");

        Bus bus = BusFactory.getDefaultBus();

        LocalTransportFactory localTransport = new LocalTransportFactory();
        DestinationFactoryManager dfm = bus.getExtension(DestinationFactoryManager.class);
        dfm.registerDestinationFactory("http://schemas.xmlsoap.org/soap/http", localTransport);
        dfm.registerDestinationFactory("http://schemas.xmlsoap.org/wsdl/soap/http", localTransport);
        dfm.registerDestinationFactory("http://cxf.apache.org/bindings/xformat", localTransport);
        dfm.registerDestinationFactory("http://cxf.apache.org/transports/local", localTransport);

        ConduitInitiatorManager extension = bus.getExtension(ConduitInitiatorManager.class);
        extension.registerConduitInitiator("http://cxf.apache.org/transports/local", localTransport);
        extension.registerConduitInitiator("http://schemas.xmlsoap.org/wsdl/soap/http", localTransport);
        extension.registerConduitInitiator("http://schemas.xmlsoap.org/soap/http", localTransport);
        extension.registerConduitInitiator("http://cxf.apache.org/bindings/xformat", localTransport);        
    }

    @Ignore("Test for developer use to capture request messages.")
    @Test
    public void invokeApi() throws Exception {
        JaxWsServerFactoryBean sf = new JaxWsServerFactoryBean();
        sf.setAddress("local://TestApi");
        sf.setServiceBean( getMigrationApi() );
        sf.setServiceClass(MigrationApi.class);
        sf.create();

        JaxWsProxyFactoryBean cf = new JaxWsProxyFactoryBean();
        cf.setAddress("local://TestApi");
        cf.setServiceClass(MigrationApi.class);
        cf.getOutInterceptors().add( new LoggingOutInterceptor() );
        MigrationApi api = (MigrationApi) cf.create();

        // invocations for migration api
        api.findDependencies( Arrays.asList( new ExternalEntityHeader(), new ExternalEntityHeader(), new ExternalEntityHeader() ) );
        api.retrieveMappingCandidates( Arrays.asList( new ExternalEntityHeader(), new ExternalEntityHeader()), new ExternalEntityHeader(), Collections.singletonMap("a","b") );
        api.checkHeaders( Arrays.asList( new ExternalEntityHeader() ) );
        api.exportBundle( Arrays.asList( new ExternalEntityHeader(), new ExternalEntityHeader() ) );
        api.listEntities( PublishedService.class );
        api.importBundle( new MigrationBundle( new MigrationMetadata() ), true );

        // invocations for report api
        //api.getGroupingKeys();
        //api.getReportStatus( Arrays.asList("a","b") );
        //api.getReportResult( "a", ReportApi.ReportOutputType.PDF );
        //api.getReportResult( "a", ReportApi.ReportOutputType.HTML );
        //api.submitReport( new ReportApi.ReportSubmission(), Arrays.asList( ReportApi.ReportOutputType.PDF, ReportApi.ReportOutputType.HTML ) );
    }

    @Test
    public void testGatewayApi() throws Exception {
        Server serverEndpoint = buildServer( "local://GatewayApi", getGatewayApi(), GatewayApi.class );
        Client client = buildClient( "local://GatewayApi", GatewayApi.class );
        
        testApiCall( client, new QName("http://www.layer7tech.com/management/gateway", "GetClusterInfo"), "GatewayApi_ClusterInfoRequest.xml" );
        testApiCall( client, new QName("http://www.layer7tech.com/management/gateway", "GetGatewayInfo"), "GatewayApi_GatewayInfoRequest.xml" );
        testApiCall( client, new QName("http://www.layer7tech.com/management/gateway", "GetEntityInfo"), "GatewayApi_EntityInfoRequest.xml" );

        serverEndpoint.stop();
    }

    @Test
    public void testMigrationApi() throws Exception {
        Server serverEndpoint = buildServer( "local://MigrationApi", getMigrationApi(), MigrationApi.class );
        Client client = buildClient( "local://MigrationApi", MigrationApi.class );

        testApiCall( client, new QName("http://www.layer7tech.com/management/migration", "FindDependencies"), "MigrationApi_FindDependenciesRequest.xml" );
        testApiCall( client, new QName("http://www.layer7tech.com/management/migration", "RetrieveMappingCandidates"), "MigrationApi_RetrieveMappingCandidatesRequest.xml" );
        testApiCall( client, new QName("http://www.layer7tech.com/management/migration", "CheckHeaders"), "MigrationApi_CheckHeadersRequest.xml" );
        testApiCall( client, new QName("http://www.layer7tech.com/management/migration", "ExportBundle"), "MigrationApi_ExportBundleRequest.xml" );
        testApiCall( client, new QName("http://www.layer7tech.com/management/migration", "ListEntities"), "MigrationApi_ListEntitiesRequest.xml" );
        testApiCall( client, new QName("http://www.layer7tech.com/management/migration", "ImportBundle"), "MigrationApi_ImportBundleRequest.xml" );

        serverEndpoint.stop();
    }

    @Test
    public void testReportApi() throws Exception {
        Server serverEndpoint = buildServer( "local://ReportApi", getReportApi(), ReportApi.class );
        Client client = buildClient( "local://ReportApi", ReportApi.class );

        testApiCall( client, new QName("http://www.layer7tech.com/management/report", "GetGroupingKeys"), "ReportApi_GroupingKeyRequest.xml" );
        testApiCall( client, new QName("http://www.layer7tech.com/management/report", "GetReportStatus"), "ReportApi_ReportStatusRequest.xml" );
        testApiCall( client, new QName("http://www.layer7tech.com/management/report", "GetReportResult"), "ReportApi_ReportReportRequest_PDF.xml" );
        testApiCall( client, new QName("http://www.layer7tech.com/management/report", "GetReportResult"), "ReportApi_ReportReportRequest_HTML.xml" );
        testApiCall( client, new QName("http://www.layer7tech.com/management/report", "SubmitReport"), "ReportApi_SubmitReportRequest.xml" );

        serverEndpoint.stop();
    }

    private void testApiCall( final Client client, final QName operation, final String messageResource ) throws Exception {
        InputStream messageIn = null;
        try {
            messageIn = EsmApiTest.class.getResourceAsStream(messageResource);
            Assert.assertNotNull( "Message resource is null '"+messageResource+"'.", messageIn );
            requestMessage.set( MessageFactory.newInstance().createMessage( null, messageIn ) );
            BindingInfo bindingInfo = client.getEndpoint().getBinding().getBindingInfo();
            BindingOperationInfo boi = bindingInfo.getOperation( operation );
            Assert.assertNotNull( "Operation not found '"+operation+"'.", boi );
            client.invoke( boi );
        } finally {
            ResourceUtils.closeQuietly( messageIn );
        }
    }

    private <T> Server buildServer( final String address, final T serviceImplementation, final Class<T> serviceClass  ) {
        JaxWsServerFactoryBean sf = new JaxWsServerFactoryBean();
        sf.setAddress( address );
        sf.setServiceBean( serviceImplementation );
        sf.setServiceClass( serviceClass );
        return sf.create();
    }

    private Client buildClient( final String address, final Class serviceClass ) {
        JaxWsClientFactoryBean cfb = new JaxWsClientFactoryBean();
        cfb.setAddress( address );
        cfb.setServiceClass( serviceClass );
        Client client = cfb.create();

        client.getOutInterceptors().add( getClientMessageInsertionInterceptor() );
        client.getOutInterceptors().add( new LoggingOutInterceptor() );
        client.getInInterceptors().add( new LoggingInInterceptor() );

        return client;
    }

    private GatewayApi getGatewayApi() {
        return new GatewayApi(){
            @Override
            public Collection<EntityInfo> getEntityInfo( final Collection<EntityType> entityTypes ) throws GatewayException {
                Assert.assertNotNull( "entityTypes not null", entityTypes );
                Assert.assertTrue( "2 entity types", entityTypes.size()==2 );
                return Collections.emptyList();
            }

            @Override
            public ClusterInfo getClusterInfo() {
                ClusterInfo info = new ClusterInfo();
                info.setAdminAppletPort(9443);
                info.setClusterHostname("ssg.l7tech.com");
                info.setClusterHttpPort(8080);
                info.setClusterHttpsPort(8443);
                return info;
            }

            @Override
            public Collection<GatewayInfo> getGatewayInfo() {
                GatewayInfo info = new GatewayInfo();
                info.setId( "1" );
                info.setName( "SSG1" );
                info.setSoftwareVersion( "5.0" );
                info.setGatewayPort( 8443 );
                info.setProcessControllerPort( 8765 );
                info.setStatusTimestamp( 1 );
                info.setIpAddress( "127.0.0.1" );
                return Collections.singletonList( info );
            }
        };
    }

    private MigrationApi getMigrationApi() {
        return new MigrationApi() {
            @Override
            public Collection<ExternalEntityHeader> listEntities( Class<? extends Entity> clazz ) throws MigrationException {
                Assert.assertNotNull( "clazz not null", clazz );
                return null;
            }

            @Override
            public Collection<ExternalEntityHeader> checkHeaders(Collection<ExternalEntityHeader> headers) {
                Assert.assertNotNull( "headers not null", headers );
                Assert.assertTrue( "1 headers", headers.size()==1 );
                return null;                
            }

            @Override
            public MigrationMetadata findDependencies(Collection<ExternalEntityHeader> headers) throws MigrationException {
                Assert.assertNotNull( "headers not null", headers );
                Assert.assertTrue( "3 headers", headers.size()==3 );
                return null;
            }

            @Override
            public MigrationBundle exportBundle(Collection<ExternalEntityHeader> headers) throws MigrationException {
                Assert.assertNotNull( "headers not null", headers );
                Assert.assertTrue( "2 headers", headers.size()==2 );
                return null;
            }

            @Override
            public Collection<MappingCandidate> retrieveMappingCandidates(Collection<ExternalEntityHeader> mappables, ExternalEntityHeader scope, Map<String, String> filters) throws MigrationException {
                Assert.assertNotNull( "mappables not null", mappables );
                Assert.assertTrue( "2 mappables", mappables.size()==2 );
                Assert.assertNotNull( "scope not null", scope );
                Assert.assertNotNull( "filters not null", filters );
                Assert.assertTrue( "1 filters", filters.size()==1 );
                return null;
            }

            @Override
            public Collection<MigratedItem> importBundle(MigrationBundle bundle, boolean dryRun) throws MigrationException {
                Assert.assertNotNull( "bundle not null", bundle );
                Assert.assertNotNull( "dryRun not null", dryRun );
                return null;
            }
        };
    }

    private ReportApi getReportApi() {
        return new ReportApi() {
            @Override
            public String submitReport(ReportSubmission submission, Collection<ReportOutputType> types) throws ReportException {
                Assert.assertNotNull( "submission not null", submission );
                Assert.assertNotNull( "types not null", types );
                Assert.assertTrue( "2 types", types.size()==2 );
                return null;
            }

            @Override
            public Collection<ReportStatus> getReportStatus(Collection<String> ids) throws ReportException {
                Assert.assertNotNull( "ids not null", ids );
                Assert.assertTrue( "2 ids", ids.size()==2 );
                return null;
            }

            @Override
            public ReportResult getReportResult(String id, ReportOutputType type) throws ReportException {
                Assert.assertNotNull( "id not null", id );
                Assert.assertNotNull( "type not null", type );
                return null;
            }

            @Override
            public Collection<GroupingKey> getGroupingKeys() throws ReportException {
                return null;
            }
        };
    }

    private Interceptor getClientMessageInsertionInterceptor() {
        return new AbstractSoapInterceptor(Phase.MARSHAL_ENDING) {
            @Override
            public void handleMessage( final SoapMessage soapMessage ) throws Fault {
                try {
                    final XMLStreamWriter xsw = soapMessage.getContent( javax.xml.stream.XMLStreamWriter.class );
                    Node child = requestMessage.get().getSOAPBody().getFirstChild();
                    while ( child != null ) {
                        writeNode( child, xsw );
                        child = child.getNextSibling();
                    }
                } catch ( Exception e ) {
                    e.printStackTrace();
                }
            }
        };
    }

    private void writeNode( final Node node, final XMLStreamWriter xsw ) throws XMLStreamException {
        switch( node.getNodeType() ) {
            case Node.ELEMENT_NODE:
                boolean writeEnd = false;
                if ( node.getFirstChild()==null ) {
                    if ( node.getNamespaceURI()==null ) {
                        xsw.writeEmptyElement( node.getLocalName() );
                    } else {
                        xsw.writeEmptyElement( node.getPrefix(), node.getLocalName(), node.getNamespaceURI() );
                    }
                } else {
                    writeEnd = true;
                    if ( node.getNamespaceURI()==null ) {
                        xsw.writeStartElement( node.getLocalName() );
                    } else {
                        xsw.writeStartElement( node.getPrefix(), node.getLocalName(), node.getNamespaceURI() );
                    }
                }
                for ( int i=0; i<node.getAttributes().getLength(); i++ ) {
                    writeNode( node.getAttributes().item(i), xsw );
                }
                Node child = node.getFirstChild();
                while ( child != null ) {
                    writeNode( child, xsw );
                    child = child.getNextSibling();
                }
                if ( writeEnd ) {
                    xsw.writeEndElement();                    
                }
                break;
            case Node.ATTRIBUTE_NODE:
                if ( node.getNamespaceURI()==null ) {
                    xsw.writeAttribute( node.getLocalName(), node.getNodeValue() );
                } else {
                    xsw.writeAttribute( node.getPrefix(),  node.getNamespaceURI(), node.getLocalName(), node.getNodeValue() );
                }
                break;
            case Node.TEXT_NODE:
                xsw.writeCharacters( node.getNodeValue() );
                break;
        }
    }
}
