package com.l7tech.server.service.resolution;

import com.l7tech.common.http.GenericHttpHeader;
import com.l7tech.common.http.HttpHeader;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.gateway.common.audit.Audit;
import com.l7tech.gateway.common.audit.AuditFactory;
import com.l7tech.gateway.common.audit.LoggingAudit;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.gateway.common.transport.ResolutionConfiguration;
import com.l7tech.message.HttpRequestKnobStub;
import com.l7tech.message.JmsKnobStub;
import com.l7tech.message.Message;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.GoidEntity;
import com.l7tech.server.service.PersistentServiceDocumentWsdlStrategy;
import com.l7tech.server.service.ServiceDocumentManagerStub;
import com.l7tech.server.transport.ResolutionConfigurationManagerStub;
import com.l7tech.server.util.GoidUpgradeMapperTestUtil;
import com.l7tech.test.BugNumber;
import com.l7tech.xml.soap.SoapVersion;
import org.junit.BeforeClass;
import org.junit.Test;
import org.xml.sax.SAXException;

import javax.xml.namespace.QName;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.junit.Assert.*;

/**
 * Tests for service resolution.
 */
public class ServiceResolutionManagerTest {

    @Test
    public void testResolutionEmptyBody() throws Exception {
        configure( getDefaultResolutionConfiguration(), resolutionManager.getResolvers() );
        Message message = new Message( XmlUtil.parse( "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\"><soapenv:Body/></soapenv:Envelope>" ));
        PublishedService service = resolutionManager.resolve( auditor, message, srl(), services );
        assertNotNull( "Service null (not resolved)", service );
        assertEquals( "Service id", new Goid(serviceUpgradePrefix,1L), service.getGoid() );
    }

    @Test
    public void testResolutionUri() throws Exception {
        configure( getDefaultResolutionConfiguration(), resolutionManager.getResolvers() );
        Message message = new Message( XmlUtil.parse( "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\"><soapenv:Body><listProducts xmlns=\"http://warehouse.acme.com/ws\"/></soapenv:Body></soapenv:Envelope>" ));
        message.attachHttpRequestKnob( new HttpRequestKnobStub(null, "/warehouse") );
        PublishedService service = resolutionManager.resolve( auditor, message, srl(), services );
        assertNotNull( "Service null (not resolved)", service );
        assertEquals( "Service id", new Goid(serviceUpgradePrefix,2L), service.getGoid() );

        // test with body content resolver disabled
        final ResolutionConfiguration config = getDefaultResolutionConfiguration();
        config.setUseSoapBodyChildNamespace( false );
        configure( config, resolutionManager.getResolvers() );
        PublishedService service2 = resolutionManager.resolve( auditor, message, srl(), services );
        assertNotNull( "Service null (not resolved 2)", service2 );
        assertEquals( "Service id", new Goid(serviceUpgradePrefix,2L), service2.getGoid() );
    }

    @Test
    public void testResolutionUriWild() throws Exception {
        configure( getDefaultResolutionConfiguration(), resolutionManager.getResolvers() );
        Message message = new Message( XmlUtil.parse( "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\"><soapenv:Body><listProducts xmlns=\"http://warehouse.acme.com/ws\"/></soapenv:Body></soapenv:Envelope>" ));
        message.attachHttpRequestKnob( new HttpRequestKnobStub(null, "/wild/some/path/here") );
        PublishedService service = resolutionManager.resolve( auditor, message, srl(), services );
        assertNotNull( "Service null (not resolved)", service );
        assertEquals( "Service id", new Goid(serviceUpgradePrefix,4L), service.getGoid() );
    }

    @Test
    public void testResolutionUriCaseInsensitive() throws Exception {
        configure( getDefaultResolutionConfiguration(), resolutionManager.getResolvers() );
        Message message = new Message( XmlUtil.parse( "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\"><soapenv:Body><listProducts xmlns=\"http://warehouse.acme.com/ws\"/></soapenv:Body></soapenv:Envelope>" ));
        message.attachHttpRequestKnob( new HttpRequestKnobStub(null, "/WaReHoUsE") );
        PublishedService service = resolutionManager.resolve( auditor, message, srl(), services );
        assertNull( "Service not null (resolved)", service );

        // test with case insensitive resolution enabled
        final ResolutionConfiguration config = getDefaultResolutionConfiguration();
        config.setPathCaseSensitive( false );
        configure( config, resolutionManager.getResolvers() );
        PublishedService service2 = resolutionManager.resolve( auditor, message, srl(), services );
        assertNotNull( "Service null (not resolved 2)", service2 );
        assertEquals( "Service id", new Goid(serviceUpgradePrefix,2L), service2.getGoid() );
    }

    @Test
    public void testResolutionUriPathRequired() throws Exception {
        // test services when path is not required
        configure( getDefaultResolutionConfiguration(), resolutionManager.getResolvers() );
        Message message = new Message( XmlUtil.parse( "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\"><soapenv:Body><listProducts xmlns=\"http://warehouse.acme.com/ws\"/></soapenv:Body></soapenv:Envelope>" ));
        message.attachHttpRequestKnob( new HttpRequestKnobStub(null, "/warehouse") );
        PublishedService service = resolutionManager.resolve( auditor, message, srl(), services );
        assertNotNull( "Service null (not resolved)", service );
        assertEquals( "Service id", new Goid(serviceUpgradePrefix,2L), service.getGoid() );

        Message message2 = new Message( XmlUtil.parse( "<xml-message/>" ));
        message2.attachHttpRequestKnob( new HttpRequestKnobStub(null, "/service/3") );
        PublishedService service2 = resolutionManager.resolve( auditor, message2, srl(), services );
        assertNotNull( "Service null (not resolved)", service2 );
        assertEquals( "Service id", new Goid(serviceUpgradePrefix,3L), service2.getGoid() );

        // test with uri path required
        final ResolutionConfiguration config = getDefaultResolutionConfiguration();
        config.setPathRequired( true );
        configure( config, resolutionManager.getResolvers() );
        PublishedService service3 = resolutionManager.resolve( auditor, message, srl(), services );
        assertNotNull( "Service null (not resolved 2)", service3 );
        assertEquals( "Service id", new Goid(serviceUpgradePrefix,2L), service3.getGoid() );

        PublishedService service4 = resolutionManager.resolve( auditor, message2, srl(), services );
        assertNull( "Service not null (resolved)", service4 );
    }

    @Test
    public void testResolutionSoapBody() throws Exception {
        configure( getDefaultResolutionConfiguration(), resolutionManager.getResolvers() );
        Message message = new Message( XmlUtil.parse( "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\"><soapenv:Body><listProducts xmlns=\"http://warehouse.acme.com/ws\"/></soapenv:Body></soapenv:Envelope>" ));
        PublishedService service = resolutionManager.resolve( auditor, message, srl(), services );
        assertNotNull( "Service null (not resolved)", service );
        assertEquals( "Service id", new Goid(serviceUpgradePrefix,2L), service.getGoid() );

        // test with SOAP action resolver disabled
        final ResolutionConfiguration config = getDefaultResolutionConfiguration();
        config.setUseSoapAction( false );
        configure( config, resolutionManager.getResolvers() );
        PublishedService service2 = resolutionManager.resolve( auditor, message, srl(), services );
        assertNotNull( "Service null (not resolved 2)", service2 );
        assertEquals( "Service id", new Goid(serviceUpgradePrefix,2L), service2.getGoid() );

        // test failure when SOAP Body child resolution is not in use
        config.setUseSoapBodyChildNamespace( false );
        configure( config, resolutionManager.getResolvers() );
        PublishedService service3 = resolutionManager.resolve( auditor, message, srl(), services );
        assertNull( "Service not null (not resolved 3)", service3 );
    }

    @Test
    public void testResolutionSoapAction() throws Exception {
        configure( getDefaultResolutionConfiguration(), resolutionManager.getResolvers() );
        Message message = new Message( XmlUtil.parse( "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\"><soapenv:Body><listProducts xmlns=\"http://warehouse.acme.com/ws\"/></soapenv:Body></soapenv:Envelope>" ));
        message.attachJmsKnob( new JmsKnobStub(GoidEntity.DEFAULT_GOID, true, "http://warehouse.acme.com/ws/listProducts" ) );
        PublishedService service = resolutionManager.resolve( auditor, message, srl(), services );
        assertNotNull( "Service null (not resolved 1)", service );
        assertEquals( "Service id", new Goid(serviceUpgradePrefix,2L), service.getGoid() );

        // test with body content resolver disabled
        final ResolutionConfiguration config = getDefaultResolutionConfiguration();
        config.setUseSoapBodyChildNamespace( false );
        configure( config, resolutionManager.getResolvers() );
        PublishedService service2 = resolutionManager.resolve( auditor, message, srl(), services );
        assertNotNull( "Service null (not resolved 2)", service2 );
        assertEquals( "Service id", new Goid(serviceUpgradePrefix,2L), service2.getGoid() );

        // test failure when soap action is not in use
        config.setUseSoapAction( false );
        configure( config, resolutionManager.getResolvers() );
        PublishedService service3 = resolutionManager.resolve( auditor, message, srl(), services );
        assertNull( "Service not null (not resolved 3)", service3 );
    }

    @Test
    public void testResolutionOriginalUrl() throws Exception {
        configure( getDefaultResolutionConfiguration(), resolutionManager.getResolvers() );
        Message message = new Message( XmlUtil.parse( "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\"><soapenv:Body><listProducts xmlns=\"http://warehouse.acme.com/ws\"/></soapenv:Body></soapenv:Envelope>" ));
        message.attachHttpRequestKnob( new HttpRequestKnobStub(l(header("L7-Original-URL","http://blah/warehouse")),"/some/other/uri") );
        PublishedService service = resolutionManager.resolve( auditor, message, srl(), services );
        assertNotNull( "Service null (not resolved 1)", service );
        assertEquals( "Service id", new Goid(serviceUpgradePrefix,2L), service.getGoid() );

        // test with original url disabled
        final ResolutionConfiguration config = getDefaultResolutionConfiguration();
        config.setUseL7OriginalUrl( false );
        configure( config, resolutionManager.getResolvers() );
        PublishedService service2 = resolutionManager.resolve( auditor, message, srl(), services );
        assertNull( "Service not null (resolved 2)", service2 );
    }

    @Test
    public void testResolutionServiceId() throws Exception {
        configure( getDefaultResolutionConfiguration(), resolutionManager.getResolvers() );
        Message message = new Message( XmlUtil.parse( "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\"><soapenv:Body><listProducts xmlns=\"http://warehouse.acme.com/ws\"/></soapenv:Body></soapenv:Envelope>" ));
        message.attachHttpRequestKnob( new HttpRequestKnobStub(null,"/service/2") );
        PublishedService service = resolutionManager.resolve( auditor, message, srl(), services );
        assertNotNull( "Service null (not resolved 1)", service );
        assertEquals( "Service id", new Goid(serviceUpgradePrefix,2L), service.getGoid() );

        // test with service id disabled
        final ResolutionConfiguration config = getDefaultResolutionConfiguration();
        config.setUseServiceOid( false );
        configure( config, resolutionManager.getResolvers() );
        PublishedService service2 = resolutionManager.resolve( auditor, message, srl(), services );
        assertNull( "Service not null (resolved 2)", service2 );
    }

    @Test
    public void testResolutionServiceGoid() throws Exception {
        configure( getDefaultResolutionConfiguration(), resolutionManager.getResolvers() );
        Message message = new Message( XmlUtil.parse( "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\"><soapenv:Body><listProducts xmlns=\"http://warehouse.acme.com/ws\"/></soapenv:Body></soapenv:Envelope>" ));
        message.attachHttpRequestKnob( new HttpRequestKnobStub(null,"/service/"+new Goid(serviceUpgradePrefix,2L).toHexString()) );
        PublishedService service = resolutionManager.resolve( auditor, message, srl(), services );
        assertNotNull( "Service null (not resolved 1)", service );
        assertEquals( "Service id", new Goid(serviceUpgradePrefix,2L), service.getGoid() );

        // test with service id disabled
        final ResolutionConfiguration config = getDefaultResolutionConfiguration();
        config.setUseServiceOid( false );
        configure( config, resolutionManager.getResolvers() );
        PublishedService service2 = resolutionManager.resolve( auditor, message, srl(), services );
        assertNull( "Service not null (resolved 2)", service2 );
    }

    @Test
    public void testResolutionServiceComplexHexGoid() throws Exception {
        configure( getDefaultResolutionConfiguration(), resolutionManager.getResolvers() );
        Message message = new Message( XmlUtil.parse( "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\"><soapenv:Body><listProducts xmlns=\"http://warehouse.acme.com/ws\"/></soapenv:Body></soapenv:Envelope>" ));
        message.attachHttpRequestKnob( new HttpRequestKnobStub(null,"/service/"+new Goid(-93463452159384L,6294682937658L).toHexString()) );
        PublishedService service = resolutionManager.resolve( auditor, message, srl(), services );
        assertNotNull( "Service null (not resolved 1)", service );
        assertEquals( "Service id", new Goid(-93463452159384L,6294682937658L), service.getGoid() );

        // test with service id disabled
        final ResolutionConfiguration config = getDefaultResolutionConfiguration();
        config.setUseServiceOid( false );
        configure( config, resolutionManager.getResolvers() );
        PublishedService service2 = resolutionManager.resolve( auditor, message, srl(), services );
        assertNull( "Service not null (resolved 2)", service2 );
    }

    @Test
    public void testResolutionServiceIdInOriginalUrl() throws Exception {
        configure( getDefaultResolutionConfiguration(), resolutionManager.getResolvers() );
        Message message = new Message( XmlUtil.parse( "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\"><soapenv:Body><listProducts xmlns=\"http://warehouse.acme.com/ws\"/></soapenv:Body></soapenv:Envelope>" ));
        message.attachHttpRequestKnob( new HttpRequestKnobStub(l(header("L7-Original-URL","http://blah/service/2")),"/some/other/uri") );
        PublishedService service = resolutionManager.resolve( auditor, message, srl(), services );
        assertNotNull( "Service null (not resolved 1)", service );
        assertEquals( "Service id", new Goid(serviceUpgradePrefix,2L), service.getGoid() );

        // test with original url disabled
        final ResolutionConfiguration config = getDefaultResolutionConfiguration();
        config.setUseL7OriginalUrl( false );
        configure( config, resolutionManager.getResolvers() );
        PublishedService service2 = resolutionManager.resolve( auditor, message, srl(), services );
        assertNull( "Service not null (resolved 2)", service2 );

        // test with service id disabled
        config.setUseL7OriginalUrl( true );
        config.setUseServiceOid( false );
        configure( config, resolutionManager.getResolvers() );
        PublishedService service3 = resolutionManager.resolve( auditor, message, srl(), services );
        assertNull( "Service not null (resolved 3)", service3 );

        // test with both disabled
        config.setUseL7OriginalUrl( false );
        config.setUseServiceOid( false );
        configure( config, resolutionManager.getResolvers() );
        PublishedService service4 = resolutionManager.resolve( auditor, message, srl(), services );
        assertNull( "Service not null (resolved 4)", service4 );
    }

    @Test
    public void testStrictResolutionOperation() throws Exception {
        configure( getDefaultResolutionConfiguration(), resolutionManager.getResolvers() );

        final String[] extraElementValues = new String[]{ "2", "" };
        for ( final String extraElementText : extraElementValues ) {
            final Message message = new Message( XmlUtil.parse( "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\"><soapenv:Body><listProducts"+extraElementText+" xmlns=\"http://warehouse.acme.com/ws\"/></soapenv:Body></soapenv:Envelope>" ));
            message.attachKnob( HttpRequestKnobStub.class, new HttpRequestKnobStub(null, "/warehouse") );
            final PublishedService service = resolutionManager.resolve( auditor, message, srl(), services );

            if ( "".equals(extraElementText) ) {
                assertNotNull( "Service not null (resolved)", service );
            } else {
                assertNull( "Service null (not resolved)", service );
            }
        }
    }

    @Test
    public void testStrictResolutionSoapVersion() throws Exception {
        configure( getDefaultResolutionConfiguration(), resolutionManager.getResolvers() );

        final SoapVersion[] soapVersions = new SoapVersion[]{ SoapVersion.SOAP_1_2, SoapVersion.SOAP_1_1 };
        for ( final SoapVersion soapVersion : soapVersions ) {
            final Message message = new Message();
            message.initialize( XmlUtil.parse( "<soapenv:Envelope xmlns:soapenv=\""+soapVersion.getNamespaceUri()+"\"><soapenv:Body><listProducts xmlns=\"http://warehouse.acme.com/ws\"/></soapenv:Body></soapenv:Envelope>" ), ContentTypeHeader.create( soapVersion.getContentType() ));
            final PublishedService service = resolutionManager.resolve( auditor, message, srl(), services );
            if ( soapVersion == SoapVersion.SOAP_1_1 ) {
                assertNotNull( "Service not null (resolved)", service );
            } else {
                assertNull( "Service null (not resolved)", service );
            }
        }
    }

    @Test
    public void testBuildTargetValuesNoOperations() throws Exception {
        AuditFactory auditFactory = LoggingAudit.factory();

        final ServiceResolver r1 = new SoapActionResolver(auditFactory);
        final ServiceResolver r2 = new UrnResolver(auditFactory);
        final ServiceResolver r3 = new SoapOperationResolver(auditFactory, new ServiceDocumentManagerStub());

        final PublishedService service = new PublishedService();
        service.setGoid(new Goid(0,1L));
        service.setName("EmptyOpService");
        service.setRoutingUri( "/empty" );
        service.setSoap(true);
        service.setLaxResolution(false);
        service.setWsdlXml( EMPTY_OPERATION_REQUEST_WSDL );

        assertEquals( "SOAP action resolver", Collections.singletonList( "urn:empty:empty" ), r1.buildTargetValues( service ) );
        assertEquals( "SOAP namespace resolver", Collections.singletonList( null ), r2.buildTargetValues( service ) );
        assertEquals( "SOAP operation resolver", Collections.singletonList( Collections.emptyList() ), r3.buildTargetValues( service ) );
    }

    @BugNumber(9776)
    @Test
    public void testServiceConflictWild() throws Exception {
        resolutionManager.checkResolution( service(new Goid(0,-1L), "Test1", "/wild/path", null, false ), services );

        try {
            resolutionManager.checkResolution( service(new Goid(0,-1L), "Test2", "/wild/*", null, false ), services );
            fail( "Service conflict expected for /wild/*" );
        } catch ( NonUniqueServiceResolutionException e ) {
            // expected
        }
    }

    private static long serviceUpgradePrefix = 36458376452L;

    @SuppressWarnings({ "unchecked" })
    @BeforeClass
    public static void init() {
        PersistentServiceDocumentWsdlStrategy.setServiceDocumentManager( new ServiceDocumentManagerStub() );

        AuditFactory auditFactory = LoggingAudit.factory();

        GoidUpgradeMapperTestUtil.addPrefix("published_service", serviceUpgradePrefix);

        resolutionManager = new ServiceResolutionManager(
            new ResolutionConfigurationManagerStub(),
            "Default",
            Arrays.<ServiceResolver>asList(
                new RequirePathResolver(auditFactory),
                new ServiceIdResolver(auditFactory),
                new UriResolver(auditFactory),
                new CaseInsensitiveUriResolver(auditFactory),
                new SoapActionResolver(auditFactory),
                new UrnResolver(auditFactory)
            ), Arrays.<ServiceResolver>asList(
                new SoapVersionResolver(auditFactory),
                new SoapOperationResolver(auditFactory, new ServiceDocumentManagerStub())
        ));


        final ServiceInfo[] serviceInfos = new ServiceInfo[]{
            si( service( new Goid(serviceUpgradePrefix,1L), "EmptyOpService", "/empty", SoapVersion.UNKNOWN, false), l("urn:empty:empty"), l((String)null), l(Collections.<QName>emptyList()) ),
            si( service( new Goid(serviceUpgradePrefix,2L), "SoapService", "/warehouse", SoapVersion.SOAP_1_1, false), l("http://warehouse.acme.com/ws/listProducts","http://warehouse.acme.com/ws/getProductDetails"), l("http://warehouse.acme.com/ws"), l(qns("http://warehouse.acme.com/ws","listProducts"),qns("http://warehouse.acme.com/ws","getProductDetails")) ),
            si( service( new Goid(serviceUpgradePrefix,3L), "XML", null, null, false), null, null, null ), // unresolvable other than by id
            si( service( new Goid(serviceUpgradePrefix,4L), "Wild", "/wild/*", null, false), null, null, null ),
            si( service( new Goid(-93463452159384L,6294682937658L), "ComplexHex", "/complex_hex/*", null, false), null, null, null ),
        };

        Collection<PublishedService> services = new ArrayList<PublishedService>();
        for ( final ServiceInfo info : serviceInfos ) {
            services.add( info.service );
            for ( final ServiceResolver resolver : resolutionManager.getResolvers() ) {
                resolver.updateServiceValues( info.service, info.getTargetValues( resolver.getClass() ) );
            }
        }
        ServiceResolutionManagerTest.services = Collections.unmodifiableCollection( services );
    }

    //- PRIVATE

    private static final Logger logger = Logger.getLogger( ServiceResolutionManagerTest.class.getName() );
    private static final Audit auditor = new LoggingAudit(logger);

    private static Collection<PublishedService> services;
    private static ServiceResolutionManager resolutionManager;

    private static ResolutionConfiguration getDefaultResolutionConfiguration() {
        final ResolutionConfiguration configuration = new ResolutionConfiguration();

        configuration.setPathRequired( false );
        configuration.setPathCaseSensitive( true );
        configuration.setUseL7OriginalUrl( true );
        configuration.setUseServiceOid( true );
        configuration.setUseSoapAction( true );
        configuration.setUseSoapBodyChildNamespace( true );

        return configuration;
    }

    private static void configure( final ResolutionConfiguration configuration,
                                   final Collection<ServiceResolver> resolvers ) {
        for ( final ServiceResolver resolver : resolvers ) {
            resolver.configure( configuration );
        }
    }

    private static PublishedService service( final Goid goid, final String name, final String uri, final SoapVersion soapVersion, final boolean lax ) {
        final PublishedService service = new PublishedService();
        service.setGoid(goid);
        service.setName( name );
        service.setRoutingUri( uri );
        if ( soapVersion == null ) {
            service.setSoap( false );
        } else {
            service.setSoap( true );
            service.setSoapVersion( soapVersion );
        }
        service.setLaxResolution( lax );
        return service;
    }

    private static ServiceInfo si( final PublishedService service,
                                   final List<String> soapActions,
                                   final List<String> soapNamespaces,
                                   final List<List<QName>> soapOperations  ) {
        return new ServiceInfo(
                service,
                soapActions == null ? Collections.<String>emptyList() : soapActions,
                soapNamespaces == null ? Collections.<String>emptyList() : soapNamespaces,
                soapOperations == null ? Collections.<List<QName>>emptyList() : soapOperations );
    }

    private static <V> List<V> l( V... values ) {
        return Collections.unmodifiableList( Arrays.asList( values ) );
    }

    private static List<QName> qns( final String namespace, final String localName ) {
        return Collections.singletonList( new QName( namespace, localName ) );
    }

    private static HttpHeader header( final String name, final String value ) {
        return new GenericHttpHeader( name, value );
    }

    /**
     * Strict service resolution currently fails if we don't call isSoap to create a SoapKnob ... 
     */
    private static ServiceResolutionManager.ServiceResolutionListener srl() {
        return new ServiceResolutionManager.ServiceResolutionListener() {
            @Override
            public boolean notifyMessageBodyAccess( final Message message, final Collection<PublishedService> serviceSet ) {
                try {
                    message.isSoap();
                } catch ( SAXException e ) {
                    logger.log( Level.WARNING, "Error checking if soap", e );
                } catch ( IOException e ) {
                    logger.log( Level.WARNING, "Error checking if soap", e );
                }
                return true;
            }

            @Override
            public void notifyMessageValidation( final Message message, final PublishedService service ) {
            }
        };
    }

    private static final class ServiceInfo {
        private final PublishedService service;
        private final List<String> soapActions;
        private final List<String> soapNamespaces;
        private final List<List<QName>> soapOperations;

        private ServiceInfo( final PublishedService service,
                             final List<String> soapActions,
                             final List<String> soapNamespaces,
                             final List<List<QName>> soapOperations ) {
            this.service = service;
            this.soapActions = Collections.unmodifiableList( soapActions );
            this.soapNamespaces = Collections.unmodifiableList( soapNamespaces );
            this.soapOperations = Collections.unmodifiableList( soapOperations );
        }

        private List<?> getTargetValues( final Class<? extends ServiceResolver> resolverClass ) {
            final List<Object> targetValues = new ArrayList<Object>();

            if ( ServiceIdResolver.class.equals( resolverClass ) ) {
                targetValues.add( service.getGoid().toString() );
                if(service.getGoid().getHi()==serviceUpgradePrefix)
                    targetValues.add( Long.toString(service.getGoid().getLow()) );
            } else if ( UriResolver.class.equals( resolverClass ) ) {
                targetValues.add( service.getRoutingUri()==null ? "" : service.getRoutingUri() );
            } else if ( CaseInsensitiveUriResolver.class.equals( resolverClass ) ) {
                targetValues.add( service.getRoutingUri()==null ? "" : service.getRoutingUri() );
            } else if ( SoapActionResolver.class.equals( resolverClass ) ) {
                targetValues.addAll( soapActions );
            } else if ( UrnResolver.class.equals( resolverClass ) ) {
                targetValues.addAll( soapNamespaces );
            } else if ( SoapOperationResolver.class.equals( resolverClass ) ) {
                targetValues.addAll( soapOperations );
            }  else if ( SoapVersionResolver.class.equals( resolverClass ) ) {
                targetValues.addAll( service.getSoapVersion().getContentType()==null ?
                        Arrays.asList(SoapVersion.SOAP_1_1.getContentType(), SoapVersion.SOAP_1_2.getContentType()) : 
                        l(service.getSoapVersion().getContentType()) );
            }
            
            return targetValues;
        }
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
