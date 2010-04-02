package com.l7tech.gateway.api;

import com.l7tech.gateway.api.impl.TransportFactory;
import com.l7tech.util.DomUtils;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.Functions;
import com.l7tech.util.ResourceUtils;
import com.l7tech.util.SyspropUtil;
import com.sun.ws.management.Management;
import com.sun.ws.management.addressing.Addressing;
import com.sun.ws.management.client.exceptions.FaultException;
import com.sun.ws.management.client.impl.TransportClient;
import org.junit.BeforeClass;
import org.junit.Test;
import org.w3c.dom.Element;

import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSocketFactory;
import javax.xml.bind.JAXBException;
import javax.xml.soap.MessageFactory;
import javax.xml.soap.SOAPBody;
import javax.xml.soap.SOAPConstants;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPFault;
import javax.xml.soap.SOAPMessage;
import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.PasswordAuthentication;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 */
public class AccessorTest {

    //- PUBLIC

    @Test
    public void testCreate() throws Exception {
        setResponse( "ClusterProperty_Create_Response.xml" );
        Client client = null;
        try {
            client = getClient();
            final Accessor<ClusterPropertyMO> clusterPropertyAccessor = client.getAccessor( ClusterPropertyMO.class );
            final ClusterPropertyMO clusterProperty = ManagedObjectFactory.createClusterProperty();
            clusterProperty.setName( "a" );
            clusterProperty.setValue( "b" );
            final String identifier = clusterPropertyAccessor.create( clusterProperty );
            assertEquals( "identifier", "264372224", identifier );
        } finally {
            ResourceUtils.closeQuietly( client );
        }
    }

    @Test
    public void testGetById() throws Exception {
        setResponse( "ClusterProperty_Get_Response.xml" );
        Client client = null;
        try {
            client = getClient();
            final Accessor<ClusterPropertyMO> clusterPropertyAccessor = client.getAccessor( ClusterPropertyMO.class );
            final ClusterPropertyMO clusterProperty = clusterPropertyAccessor.get( "20774913" );
            assertEquals( "id", "20774913", clusterProperty.getId() );
            assertEquals( "version", (Integer)4, clusterProperty.getVersion() );
            assertEquals( "name", "soap.roles", clusterProperty.getName() );
            assertEquals( "value", "secure_span\nhttp://www.layer7tech.com/ws/policy\nhttp://tempuri.org/myactor4", clusterProperty.getValue() );
        } finally {
            ResourceUtils.closeQuietly( client );
        }
    }

    @Test
    public void testGetByName() throws Exception {
        setResponse( "ClusterProperty_Get_Response.xml" );
        Client client = null;
        try {
            client = getClient();
            final Accessor<ClusterPropertyMO> clusterPropertyAccessor = client.getAccessor( ClusterPropertyMO.class );
            final ClusterPropertyMO clusterProperty = clusterPropertyAccessor.get( "name", "soap.roles" );
            assertEquals( "id", "20774913", clusterProperty.getId() );
            assertEquals( "version", (Integer)4, clusterProperty.getVersion() );
            assertEquals( "name", "soap.roles", clusterProperty.getName() );
            assertEquals( "value", "secure_span\nhttp://www.layer7tech.com/ws/policy\nhttp://tempuri.org/myactor4", clusterProperty.getValue() );
        } finally {
            ResourceUtils.closeQuietly( client );
        }
    }

    @Test
    public void testPut() throws Exception {
        setResponse( "ClusterProperty_Put_Response.xml" );
        Client client = null;
        try {
            client = getClient();
            final Accessor<ClusterPropertyMO> clusterPropertyAccessor = client.getAccessor( ClusterPropertyMO.class );
            final ClusterPropertyMO clusterProperty = ManagedObjectFactory.createClusterProperty();
            clusterProperty.setId("264372224");
            clusterProperty.setVersion(0);
            clusterProperty.setName( "testproperty" );
            clusterProperty.setValue( "testvalue2" );
            final ClusterPropertyMO clusterPropertyUpdated = clusterPropertyAccessor.put( clusterProperty );
            assertEquals( "id", "264372224", clusterPropertyUpdated.getId() );
            assertEquals( "version", (Integer)1, clusterPropertyUpdated.getVersion() );
            assertEquals( "name", "testproperty", clusterPropertyUpdated.getName() );
            assertEquals( "value", "testvalue2", clusterPropertyUpdated.getValue() );
        } finally {
            ResourceUtils.closeQuietly( client );
        }
    }

    @Test
    public void testDelete() throws Exception {
        setResponse( "ClusterProperty_Delete_Response.xml" );
        Client client = null;
        try {
            client = getClient();
            final Accessor<ClusterPropertyMO> clusterPropertyAccessor = client.getAccessor( ClusterPropertyMO.class );
            clusterPropertyAccessor.delete( "1234" );
        } finally {
            ResourceUtils.closeQuietly( client );
        }
    }

    @Test
    public void testEnumerate() throws Exception {
        setResponse( "ClusterProperty_Enumerate_Response1.xml", "ClusterProperty_Enumerate_Response2.xml", "ClusterProperty_Enumerate_Response3.xml" );
        Client client = null;
        try {
            client = getClient();
            final Accessor<ClusterPropertyMO> clusterPropertyAccessor = client.getAccessor( ClusterPropertyMO.class );
            final Iterator<ClusterPropertyMO> clusterPropertyIterator = clusterPropertyAccessor.enumerate();
            int count = 0;
            while ( clusterPropertyIterator.hasNext() ) {
                final ClusterPropertyMO clusterProperty = clusterPropertyIterator.next();
                assertNotNull( clusterProperty );
                assertNotNull( clusterProperty.getId() );
                assertNotNull( clusterProperty.getName() );
                assertNotNull( clusterProperty.getValue() );
                count++;
            }
            assertEquals( "Cluster property count", 12, count );
        } finally {
            ResourceUtils.closeQuietly( client );
        }
    }

    @Test
    public void testGetPolicy() throws Exception {
        setResponse( "Policy_GetPolicy_Response.xml" );
        Client client = null;
        try {
            client = getClient();
            final PolicyAccessor<PolicyMO> policyAccessor = (PolicyAccessor<PolicyMO>) client.getAccessor( PolicyMO.class );
            final Resource policyResource = policyAccessor.getPolicy( "248872960" );
            assertEquals( "resource type", "policy", policyResource.getType() );
            assertEquals( "resource content", "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                    "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
                    "    <wsp:All wsp:Usage=\"Required\">\n" +
                    "        <L7p:AuditDetailAssertion>\n" +
                    "            <L7p:Detail stringValue=\"Policy Fragment: 555\"/>\n" +
                    "        </L7p:AuditDetailAssertion>\n" +
                    "    </wsp:All>\n" +
                    "</wsp:Policy>\n", policyResource.getContent() );
            assertNull( "resource id", policyResource.getId() );
            assertNull( "resource version", policyResource.getVersion() );
            assertNull( "resource source url", policyResource.getSourceUrl() );
        } finally {
            ResourceUtils.closeQuietly( client );
        }
    }

    @Test
    public void testPutPolicy() throws Exception {
        setResponse( "Policy_PutPolicy_Response.xml" );
        Client client = null;
        try {
            client = getClient();
            final PolicyAccessor<PolicyMO> policyAccessor = (PolicyAccessor<PolicyMO>) client.getAccessor( PolicyMO.class );
            final Resource policyResource = ManagedObjectFactory.createResource();
            policyResource.setType( "policy" );
            policyResource.setContent( "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                    "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
                    "    <wsp:All wsp:Usage=\"Required\">\n" +
                    "        <L7p:AuditDetailAssertion>\n" +
                    "            <L7p:Detail stringValue=\"Policy Fragment: 555\"/>\n" +
                    "        </L7p:AuditDetailAssertion>\n" +
                    "    </wsp:All>\n" +
                    "</wsp:Policy>\n" );
            policyAccessor.putPolicy( "248872960", policyResource );
        } finally {
            ResourceUtils.closeQuietly( client );
        }
    }

    @Test
    public void testValidatePolicy() throws Exception {
        setResponse( "Policy_Validate_Response.xml" );
        Client client = null;
        try {
            client = getClient();
            final PolicyAccessor<PolicyMO> policyAccessor = (PolicyAccessor<PolicyMO>) client.getAccessor( PolicyMO.class );
            final PolicyValidationResult result = policyAccessor.validatePolicy( "248872960" );
            assertEquals("validation status", PolicyValidationResult.ValidationStatus.OK, result.getStatus());
            assertNull("validation messages", result.getPolicyValidationMessages());
        } finally {
            ResourceUtils.closeQuietly( client );
        }
    }

    @Test
    public void testValidateSpecifiedPolicy() throws Exception {
        // Callback to validate that the request message contains the policy resource
        validationCallback = new Functions.UnaryVoid<Addressing>(){
            @Override
            public void call( final Addressing addressing ) {
                try {
                    final SOAPBody body = addressing.getBody();
                    assertNotNull( "request body", body );
                    Element context = DomUtils.findExactlyOneChildElementByName( body, NS_GATEWAY_MANAGEMENT, "PolicyValidationContext" );
                    DomUtils.findExactlyOneChildElementByName( context, NS_GATEWAY_MANAGEMENT, "Resources" );
                } catch ( Exception e ) {
                    throw ExceptionUtils.wrap( e );
                }
            }
        };
        setResponse( "Policy_Validate_Response.xml" );
        Client client = null;
        try {
            client = getClient();
            final PolicyAccessor<PolicyMO> policyAccessor = (PolicyAccessor<PolicyMO>) client.getAccessor( PolicyMO.class );

            final Map<String,Object> props = new HashMap<String,Object>();
            props.put( "revision", 2L );
            props.put( "soap", false );

            final PolicyDetail policyDetail = ManagedObjectFactory.createPolicyDetail();
            policyDetail.setId( "248872960" );
            policyDetail.setGuid( "447a0133-5e33-43eb-a197-8a70e6e3d2f1" );
            policyDetail.setVersion( 0 );
            policyDetail.setName( "555" );
            policyDetail.setPolicyType( PolicyDetail.PolicyType.INCLUDE );
            policyDetail.setProperties( props );

            final Resource policyResource = ManagedObjectFactory.createResource();
            policyResource.setContent( "&lt;wsp:Policy xmlns:L7p=&quot;http://www.layer7tech.com/ws/policy&quot; xmlns:wsp=&quot;http://schemas.xmlsoap.org/ws/2002/12/policy&quot;&gt;&lt;wsp:All wsp:Usage=&quot;Required&quot;&gt;&lt;L7p:EchoRoutingAssertion/&gt;&lt;/wsp:All&gt;&lt;/wsp:Policy&gt;" );
            policyResource.setType( "policy" );

            final ResourceSet policyResourceSet = ManagedObjectFactory.createResourceSet();
            policyResourceSet.setTag( "policy" );
            policyResourceSet.setResources( Arrays.asList(policyResource) );

            final PolicyMO policy = ManagedObjectFactory.createPolicy();
            policy.setId( "248872960" );
            policy.setGuid( "447a0133-5e33-43eb-a197-8a70e6e3d2f1" );
            policy.setPolicyDetail( policyDetail );
            policy.setResourceSets( Arrays.asList( policyResourceSet ) );

            final PolicyValidationResult result = policyAccessor.validatePolicy( policy, null );
            assertEquals("validation status", PolicyValidationResult.ValidationStatus.OK, result.getStatus());
            assertNull("validation messages", result.getPolicyValidationMessages());
        } finally {
            ResourceUtils.closeQuietly( client );
        }
    }

    @Test
    public void testValidateSpecifiedWithResourcePolicy() throws Exception {
        // Callback to validate that the request message contains the policy resources
        validationCallback = new Functions.UnaryVoid<Addressing>(){
            @Override
            public void call( final Addressing addressing ) {
                try {
                    final SOAPBody body = addressing.getBody();
                    assertNotNull( "request body", body );
                    Element context = DomUtils.findExactlyOneChildElementByName( body, NS_GATEWAY_MANAGEMENT, "PolicyValidationContext" );
                    Element resourceList = DomUtils.findExactlyOneChildElementByName( context, NS_GATEWAY_MANAGEMENT, "Resources" );
                    Element resourceSet = DomUtils.findExactlyOneChildElementByName( resourceList, NS_GATEWAY_MANAGEMENT, "ResourceSet" );
                    List<Element> resources = DomUtils.findChildElementsByName( resourceSet, NS_GATEWAY_MANAGEMENT, "Resource" );
                    assertEquals( "Resource count", 2, resources.size() );
                } catch ( Exception e ) {
                    throw ExceptionUtils.wrap( e );
                }
            }
        };
        setResponse( "Policy_Validate_Response.xml" );
        Client client = null;
        try {
            client = getClient();
            final PolicyAccessor<PolicyMO> policyAccessor = (PolicyAccessor<PolicyMO>) client.getAccessor( PolicyMO.class );

            final Map<String,Object> props = new HashMap<String,Object>();
            props.put( "revision", 2L );
            props.put( "soap", false );

            final PolicyDetail policyDetail = ManagedObjectFactory.createPolicyDetail();
            policyDetail.setId( "248872960" );
            policyDetail.setGuid( "447a0133-5e33-43eb-a197-8a70e6e3d2f1" );
            policyDetail.setVersion( 0 );
            policyDetail.setName( "555" );
            policyDetail.setPolicyType( PolicyDetail.PolicyType.INCLUDE );
            policyDetail.setProperties( props );

            final Resource policyResource = ManagedObjectFactory.createResource();
            policyResource.setContent( "&lt;wsp:Policy xmlns:L7p=&quot;http://www.layer7tech.com/ws/policy&quot; xmlns:wsp=&quot;http://schemas.xmlsoap.org/ws/2002/12/policy&quot;&gt;&lt;wsp:All wsp:Usage=&quot;Required&quot;&gt;&lt;L7p:EchoRoutingAssertion/&gt;&lt;/wsp:All&gt;&lt;/wsp:Policy&gt;" );
            policyResource.setType( "policy" );

            final ResourceSet policyResourceSet = ManagedObjectFactory.createResourceSet();
            policyResourceSet.setTag( "policy" );
            policyResourceSet.setResources( Arrays.asList(policyResource) );

            final PolicyMO policy = ManagedObjectFactory.createPolicy();
            policy.setId( "248872960" );
            policy.setGuid( "447a0133-5e33-43eb-a197-8a70e6e3d2f1" );
            policy.setPolicyDetail( policyDetail );
            policy.setResourceSets( Arrays.asList( policyResourceSet ) );

            final Resource policyIncludeResource = ManagedObjectFactory.createResource();
            policyIncludeResource.setContent( "&lt;wsp:Policy xmlns:L7p=&quot;http://www.layer7tech.com/ws/policy&quot; xmlns:wsp=&quot;http://schemas.xmlsoap.org/ws/2002/12/policy&quot;&gt;&lt;wsp:All wsp:Usage=&quot;Required&quot;&gt;&lt;L7p:EchoRoutingAssertion/&gt;&lt;/wsp:All&gt;&lt;/wsp:Policy&gt;" );
            policyIncludeResource.setType( "policy" );

            final ResourceSet policyIncludeResourceSet = ManagedObjectFactory.createResourceSet();
            policyIncludeResourceSet.setTag( "policy" );
            policyIncludeResourceSet.setResources( Arrays.asList(policyIncludeResource) );

            final PolicyValidationResult result = policyAccessor.validatePolicy( policy, Arrays.asList( policyIncludeResourceSet ) );
            assertEquals("validation status", PolicyValidationResult.ValidationStatus.OK, result.getStatus());
            assertNull("validation messages", result.getPolicyValidationMessages());
        } finally {
            ResourceUtils.closeQuietly( client );
        }
    }

    @Test
    public void testGetPolicyDetail() throws Exception {
        setResponse( "Policy_GetDetails_Response.xml" );
        Client client = null;
        try {
            client = getClient();
            final PolicyMOAccessor policyAccessor = (PolicyMOAccessor) client.getAccessor( PolicyMO.class );
            final PolicyDetail detail = policyAccessor.getPolicyDetail( "248872960" );
            assertEquals( "id", "248872960", detail.getId() );
            assertEquals( "guid", "447a0133-5e33-43eb-a197-8a70e6e3d2f1", detail.getGuid() );
            assertEquals( "version", (Integer)0, detail.getVersion() );
            assertNull( "folderId", detail.getFolderId() );
            assertEquals( "name", "555", detail.getName() );
            assertEquals( "policy type", PolicyDetail.PolicyType.INCLUDE, detail.getPolicyType() );
            final Map<String,Object> props = new HashMap<String,Object>();
            props.put( "revision", 2L );
            props.put( "soap", true );
            assertEquals( "properties", props, detail.getProperties() );
        } finally {
            ResourceUtils.closeQuietly( client );
        }
    }

    @Test
    public void testPutPolicyDetail() throws Exception {
        setResponse( "Policy_PutDetails_Response.xml" );
        Client client = null;
        try {
            client = getClient();
            final PolicyMOAccessor policyAccessor = (PolicyMOAccessor) client.getAccessor( PolicyMO.class );
            final PolicyDetail policyDetail = ManagedObjectFactory.createPolicyDetail();
            policyDetail.setId( "248872960" );
            policyDetail.setGuid( "447a0133-5e33-43eb-a197-8a70e6e3d2f1" );
            policyDetail.setVersion( 0 );
            policyDetail.setName( "555" );
            policyDetail.setPolicyType( PolicyDetail.PolicyType.INCLUDE );
            final Map<String,Object> props = new HashMap<String,Object>();
            props.put( "revision", 2L );
            props.put( "soap", false );
            policyDetail.setProperties( props );
            policyAccessor.putPolicyDetail( "248872960", policyDetail );
        } finally {
            ResourceUtils.closeQuietly( client );
        }
    }

    @Test
    public void testGetServiceDetail() throws Exception {
        setResponse( "Service_GetDetails_Response.xml" );
        Client client = null;
        try {
            client = getClient();
            final ServiceMOAccessor serviceAccessor = (ServiceMOAccessor) client.getAccessor( ServiceMO.class );
            final ServiceDetail detail = serviceAccessor.getServiceDetail( "229376" );
            assertEquals( "id", "229376", detail.getId() );
            assertEquals( "version", (Integer)51, detail.getVersion() );
            assertNull( "folderId", detail.getFolderId() );
            assertEquals( "name", "Warehouse", detail.getName() );
            assertEquals( "enabled", true, detail.getEnabled() );
            final Map<String,Object> props = new HashMap<String,Object>();
            props.put( "internal", false );
            props.put( "policyRevision", 1381L );
            props.put( "soap", true );
            props.put( "wssProcessingEnabled", true );
            assertEquals( "properties", props, detail.getProperties() );
        } finally {
            ResourceUtils.closeQuietly( client );
        }
    }

    @Test
    public void testPutServiceDetail() throws Exception {
        setResponse( "Service_PutDetails_Response.xml" );
        Client client = null;
        try {
            client = getClient();
            final ServiceMOAccessor serviceAccessor = (ServiceMOAccessor) client.getAccessor( ServiceMO.class );
            final ServiceDetail serviceDetail = ManagedObjectFactory.createServiceDetail();
            serviceDetail.setId( "229376" );
            serviceDetail.setVersion( 51 );
            serviceDetail.setName( "Warehouse" );
            final Map<String,Object> props = new HashMap<String,Object>();
            props.put( "internal", false );
            props.put( "policyRevision", 1381L );
            props.put( "soap", true );
            props.put( "wssProcessingEnabled", true );
            serviceDetail.setProperties( props );
            serviceAccessor.putServiceDetail( "229376", serviceDetail );
        } finally {
            ResourceUtils.closeQuietly( client );
        }
    }

    @Test
    public void testGetWsdl() throws Exception {
        setResponse( "Service_GetWsdl_Response.xml" );
        Client client = null;
        try {
            client = getClient();
            final ServiceMOAccessor serviceAccessor = (ServiceMOAccessor) client.getAccessor( ServiceMO.class );
            final ResourceSet wsdlResourceSet = serviceAccessor.getWsdl( "229376" );
            assertEquals( "tag", "wsdl", wsdlResourceSet.getTag() );
            assertEquals( "root url", "http://hugh.l7tech.com/ACMEWarehouseWS/Service1.asmx?wsdl", wsdlResourceSet.getRootUrl() );
            final List<Resource> resources = wsdlResourceSet.getResources();
            assertNotNull( resources );
            assertEquals( "resources size", 1, resources.size() );
            final Resource resource = resources.get( 0 );
            assertEquals( "resource type", "wsdl", resource.getType() );
            assertEquals( "resource url", "http://hugh.l7tech.com/ACMEWarehouseWS/Service1.asmx?wsdl", resource.getSourceUrl() );
            assertNull( "resource id", resource.getId() );
            assertNull( "resource version", resource.getVersion() );
            assertNotNull( "resource content", resource.getContent() );
        } finally {
            ResourceUtils.closeQuietly( client );
        }
    }
    
    @Test
    public void testPutWsdl() throws Exception {
        setResponse( "Service_PutWsdl_Response.xml" );
        Client client = null;
        try {
            client = getClient();
            final ServiceMOAccessor serviceAccessor = (ServiceMOAccessor) client.getAccessor( ServiceMO.class );
            final ResourceSet wsdlResourceSet = ManagedObjectFactory.createResourceSet();
            wsdlResourceSet.setTag( "wsdl" );
            wsdlResourceSet.setRootUrl( "http://hugh.l7tech.com/ACMEWarehouseWS/Service1.asmx?wsdl" );
            final Resource wsdlResource = ManagedObjectFactory.createResource();
            wsdlResource.setType( "wsdl" );
            wsdlResource.setSourceUrl( "http://hugh.l7tech.com/ACMEWarehouseWS/Service1.asmx?wsdl" );
            wsdlResource.setContent( "<wsdl:definitions xmlns:wsdl=\"http://schemas.xmlsoap.org/wsdl/\" targetNamespace=\"http://warehouse.acme.com/ws\"/>" );
            wsdlResourceSet.setResources( Arrays.asList( wsdlResource ) );
            serviceAccessor.putWsdl( "229376", wsdlResourceSet );            
        } finally {
            ResourceUtils.closeQuietly( client );
        }
    }

    @SuppressWarnings({ "ThrowableInstanceNeverThrown" })
    @Test
    public void testSOAPFaults() throws Exception {
        {
            final FaultException fe = buildSOAPFaultException( FAULT_1 );
            final Accessor.AccessorSOAPFaultException accessorSOAPFaultException = new Accessor.AccessorSOAPFaultException( fe );
            assertEquals("Fault reason", "Policy Falsified", accessorSOAPFaultException.getFault());
            assertEquals("Fault role", "http://localhost:8080/wsman", accessorSOAPFaultException.getRole());
            assertEquals("Fault code", "soapenv:Receiver", accessorSOAPFaultException.getCode());
            assertEquals("Fault subcodes", Collections.<String>emptyList(), accessorSOAPFaultException.getSubcodes());
            assertEquals("Fault details", Collections.<String>emptyList(), accessorSOAPFaultException.getDetails());
        }
        {
            final FaultException fe = buildSOAPFaultException( FAULT_2 );
            final Accessor.AccessorSOAPFaultException accessorSOAPFaultException = new Accessor.AccessorSOAPFaultException( fe );
            assertEquals("Fault reason", "Policy Falsified", accessorSOAPFaultException.getFault());
            assertEquals("Fault role", "http://localhost:8080/wsman", accessorSOAPFaultException.getRole());
            assertEquals("Fault code", "soapenv:Receiver", accessorSOAPFaultException.getCode());
            assertEquals("Fault subcodes", Arrays.asList("m:MessageTimeout", "m:Recipient"), accessorSOAPFaultException.getSubcodes());
            assertEquals("Fault details", Collections.<String>emptyList(), accessorSOAPFaultException.getDetails());
        }
        {
            final FaultException fe = buildSOAPFaultException( FAULT_3 );
            final Accessor.AccessorSOAPFaultException accessorSOAPFaultException = new Accessor.AccessorSOAPFaultException( fe );
            assertEquals("Fault reason", "Policy Falsified", accessorSOAPFaultException.getFault());
            assertEquals("Fault role", "http://localhost:8080/wsman", accessorSOAPFaultException.getRole());
            assertEquals("Fault code", "soapenv:Receiver", accessorSOAPFaultException.getCode());
            assertEquals("Fault subcodes", Collections.<String>emptyList(), accessorSOAPFaultException.getSubcodes());
            assertEquals("Fault details", Arrays.asList("Found user: admin", "Authentication failed for identity provider ID -2"), accessorSOAPFaultException.getDetails());
        }
        {
            final FaultException fe = buildSOAPFaultException( FAULT_4 );
            System.out.println(fe.getMessage());
            final Accessor.AccessorSOAPFaultException accessorSOAPFaultException = new Accessor.AccessorSOAPFaultException( fe );
            assertEquals("Fault reason", "The service cannot comply with the request due to internal processing errors.", accessorSOAPFaultException.getFault());
            assertEquals("Fault role", "", accessorSOAPFaultException.getRole());
            assertEquals("Fault code", "env:Receiver", accessorSOAPFaultException.getCode());
            assertEquals("Fault subcodes", Arrays.asList("wsman:InternalError"), accessorSOAPFaultException.getSubcodes());
            assertEquals("Fault details", Arrays.asList("java.lang.NullPointerException", "java.lang.NullPointerException", "WsdlOperationServiceResolver:68ReolutionManagerImpl:171ReolutionManagerImpl:78NativeMethodAccessorImpl:-2NativeMethodAccessorImpl:39DelegatingMethodAccessorImpl:25Method:597AopUtils:307ReflectiveMethodInvocation:182ReflectiveMethodInvocation:149TransactionInterceptor:106ReflectiveMethodInvocation:171JdkDynamicAopProxy:204ServiceManagerImp:146ServiceManagerImp:44NativeMethodAccessorImpl:-2NativeMethodAccessorImpl:39DelegatingMethodAccessorImpl:25Method:597AopUtils:307ReflectiveMethodInvocation:182ReflectiveMethodInvocation:149TransactionInterceptor:106ReflectiveMethodInvocation:171JdkDynamicAopProxy:204PolicyVersioningServiceManager:152PolicyVersioningServiceManager:29EntityManagerResourceFactory:171EntityManagerResourceFactory:140ResourceFactorySupport:305TransactionTemplate:128ResourceFactorySupport:301EntityManagerResourceFactory:140ResourceHandler:167DefaultHandler:65ResourceHandler:72ResourceHandler:101RemoteUtils:32ResourceHandler:97ResourceHandler:93AccessController:-2Subject:396ResourceHandler:93NativeMethodAccessorImpl:-2NativeMethodAccessorImpl:39DelegatingMethodAccessorImpl:25Method:597ReflectiveRequestDispatcher:109ReflectiveRequestDispatcher:51FutureTask:303FutureTask:138Executors:441FutureTask:303FutureTask:138ThreadPoolExecutor:886ThreadPoolExecutor:908Thread:619"), accessorSOAPFaultException.getDetails());
        }
    }

    @Test
    public void testSOAPFaultParsing() throws Exception {
        String faultText1 =
                "SOAP Fault: Policy Falsified\n" +
                "     Actor: http://localhost:8080/wsman\n" +
                "      Code: soapenv:Receiver\n" +
                "  Subcodes:\n" +
                "    Detail: ";

        String faultText2 =
                "SOAP Fault: Policy Falsified\n" +
                "     Actor: http://localhost:8080/wsman\n" +
                "      Code: soapenv:Receiver\n" +
                "    Detail: ";

        String faultText3 =
                "SOAP Fault: Policy Falsified\n" +
                "     Actor: http://localhost:8080/wsman\n" +
                "      Code: soapenv:Receiver\n" +
                "  Subcodes:\n";

        String faultText4 =
                "SOAP Fault: Policy Falsified\n" +
                "     Actor: http://localhost:8080/wsman\n" +
                "      Code: soapenv:Receiver\n";

        String faultText5 =
                "SOAP Fault: Policy Falsified\n" +
                "     Actor: http://localhost:8080/wsman\n" +
                "      Code: soapenv:Receiver\n" +
                "  Subcodes:\n" +
                "    Detail: test fault details\nerserwser\nasdfe ";

        Pattern faultPattern = Accessor.AccessorSOAPFaultException.SOAP_FAULT_PATTERN;
        Matcher matcher1 = faultPattern.matcher( faultText1 );
        if ( matcher1.matches() ) {
            assertEquals("Fault reason", "Policy Falsified", matcher1.group(1) );
            assertEquals("Fault role", "http://localhost:8080/wsman", matcher1.group(2) );
            assertEquals("Fault code", "soapenv:Receiver", matcher1.group(3) );
            assertEquals("Fault subcodes", "", matcher1.group(4) );
            assertEquals("Fault details", "", matcher1.group(5) );
        }

        Matcher matcher2 = faultPattern.matcher( faultText2 );
        if ( matcher2.matches() ) {
            assertEquals("Fault reason", "Policy Falsified", matcher2.group(1) );
            assertEquals("Fault role", "http://localhost:8080/wsman", matcher2.group(2) );
            assertEquals("Fault code", "soapenv:Receiver", matcher2.group(3) );
            assertNull("Fault subcodes", matcher2.group(4) );
            assertEquals("Fault details", "", matcher2.group(5) );
        }

        Matcher matcher3 = faultPattern.matcher( faultText3 );
        if ( matcher3.matches() ) {
            assertEquals("Fault reason", "Policy Falsified", matcher3.group(1) );
            assertEquals("Fault role", "http://localhost:8080/wsman", matcher3.group(2) );
            assertEquals("Fault code", "soapenv:Receiver", matcher3.group(3) );
            assertEquals("Fault subcodes", "", matcher3.group(4) );
            assertNull("Fault details", matcher3.group(5) );
        }

        Matcher matcher4 = faultPattern.matcher( faultText4 );
        if ( matcher4.matches() ) {
            assertEquals("Fault reason", "Policy Falsified", matcher4.group(1) );
            assertEquals("Fault role", "http://localhost:8080/wsman", matcher4.group(2) );
            assertEquals("Fault code", "soapenv:Receiver", matcher4.group(3) );
            assertNull("Fault subcodes", matcher2.group(4) );
            assertNull("Fault details", matcher3.group(5) );
        }

        Matcher matcher5 = faultPattern.matcher( faultText5 );
        if ( matcher5.matches() ) {
            assertEquals("Fault reason", "Policy Falsified", matcher5.group(1) );
            assertEquals("Fault role", "http://localhost:8080/wsman", matcher5.group(2) );
            assertEquals("Fault code", "soapenv:Receiver", matcher5.group(3) );
            assertEquals("Fault subcodes", "", matcher5.group(4) );
            assertEquals("Fault details", "test fault details\nerserwser\nasdfe ", matcher5.group(5) );
        }
    }

    @BeforeClass
    public static void setup() {
        TransportFactory.setTransportStrategy( new TransportFactory.TransportStrategy(){
            @Override
            public TransportClient newTransportClient( final int connectTimeout,
                                                       final int readTimeout,
                                                       final PasswordAuthentication passwordAuthentication,
                                                       final HostnameVerifier hostnameVerifier,
                                                       final SSLSocketFactory sslSocketFactory ) {
                return new TransportClient(){
                    @Override
                    public Addressing sendRequest( final Addressing addressing,
                                                   final Map.Entry<String, String>... entries ) throws IOException, SOAPException, JAXBException {
                        logMessage( addressing );
                        validateRequestMessage(addressing);
                        Addressing response = getResponseMessage();
                        logMessage( response );
                        return response;
                    }

                    @Override
                    public Addressing sendRequest( final SOAPMessage soapMessage,
                                                   final String s,
                                                   final Map.Entry<String, String>... entries ) throws IOException, SOAPException, JAXBException {
                        return getResponseMessage();
                    }

                    private void logMessage( final Addressing addressing ) {
                        if ( logMessages ) {
                            try {
                                addressing.writeTo( System.out );
                                System.out.println();
                            } catch ( Exception e ) {
                                e.printStackTrace();
                            }
                        }
                    }
                };
            }
        } );
    }

    //- PRIVATE

    private static final String NS_GATEWAY_MANAGEMENT = "http://ns.l7tech.com/2010/04/gateway-management";
    private static Functions.UnaryVoid<Addressing> validationCallback;
    private static final Queue<Object> responseObjects = new ArrayDeque<Object>();
    private static final boolean logMessages = SyspropUtil.getBoolean( "com.l7tech.gateway.api.logTestMessages", true );

    private static void setResponse( final String... responseFileNames ) {
        responseObjects.clear();
        for ( String responseFileName : responseFileNames ) {
            responseObjects.add( "testMessages/" + responseFileName );
        }
    }

    private static void validateRequestMessage( final Addressing addressing ) throws IOException, SOAPException {
        if ( validationCallback != null ) {
            validationCallback.call( addressing );
        }
        validationCallback = null;
    }

    private static Management getResponseMessage() throws IOException, SOAPException {
        InputStream messageIn = null;
        try {
            String responseResourceName;
            Object responseObject = responseObjects.remove();
            if ( responseObject instanceof IOException ) {
                throw (IOException) responseObject;
            } else if ( responseObject instanceof SOAPException ) {
                throw (SOAPException) responseObject;
            } else if ( responseObject instanceof RuntimeException ) {
                throw (RuntimeException) responseObject;
            } else if ( responseObject instanceof String ) {
                responseResourceName = (String) responseObject;
            } else {
                throw new IOException("Unexpected response object type");
            }

            messageIn = AccessorTest.class.getResourceAsStream(responseResourceName);
            if ( messageIn == null ) {
                throw new FileNotFoundException(responseResourceName);
            }
            return new Management( messageIn );
        } catch ( NoSuchElementException e ) {
            throw new IOException("No message queued for request.");
        } finally {
            ResourceUtils.closeQuietly( messageIn );
        }

    }

    private Client getClient() {
        ClientFactory factory = ClientFactory.newInstance();
        return factory.createClient( "http://localhost:12345/thisisnotused" );
    }

    @SuppressWarnings({ "ThrowableInstanceNeverThrown" })
    private FaultException buildSOAPFaultException( final String soapFaultMessage ) throws Exception {
        MessageFactory factory = MessageFactory.newInstance( SOAPConstants.SOAP_1_2_PROTOCOL);
        SOAPMessage message = factory.createMessage( null, new ByteArrayInputStream(soapFaultMessage.getBytes()) );
        SOAPFault fault = message.getSOAPBody().getFault();
        return new FaultException(fault);
    }

    private static final String FAULT_1 =
            "<soapenv:Envelope xmlns:soapenv=\"http://www.w3.org/2003/05/soap-envelope\">\n" +
            "    <soapenv:Body>\n" +
            "        <soapenv:Fault>\n" +
            "            <soapenv:Code>\n" +
            "                <soapenv:Value>soapenv:Receiver</soapenv:Value>\n" +
            "            </soapenv:Code>\n" +
            "            <soapenv:Reason>\n" +
            "                <soapenv:Text xml:lang=\"en-US\">Policy Falsified</soapenv:Text>\n" +
            "            </soapenv:Reason>\n" +
            "            <soapenv:Role>http://localhost:8080/wsman</soapenv:Role>\n" +
            "            <soapenv:Detail>\n" +
            "                <l7:policyResult status=\"Something went wrong\" xmlns:l7=\"http://www.layer7tech.com/ws/policy/fault\"/>\n" +
            "            </soapenv:Detail>\n" +
            "        </soapenv:Fault>\n" +
            "    </soapenv:Body>\n" +
            "</soapenv:Envelope>";

    private static final String FAULT_2 = // fault with subcodes
            "<soapenv:Envelope xmlns:soapenv=\"http://www.w3.org/2003/05/soap-envelope\" xmlns:m=\"http://www.example.org/timeouts\">\n" +
            "    <soapenv:Body>\n" +
            "        <soapenv:Fault>\n" +
            "            <soapenv:Code>\n" +
            "                <soapenv:Value>soapenv:Receiver</soapenv:Value>\n" +
            "                <soapenv:Subcode>\n" +
            "                    <soapenv:Value>m:MessageTimeout</soapenv:Value>\n" +
            "                    <soapenv:Subcode>\n" +
            "                        <soapenv:Value>m:Recipient</soapenv:Value>\n" +
            "                    </soapenv:Subcode>" +
            "                </soapenv:Subcode>" +
            "            </soapenv:Code>\n" +
            "            <soapenv:Reason>\n" +
            "                <soapenv:Text xml:lang=\"en-US\">Policy Falsified</soapenv:Text>\n" +
            "            </soapenv:Reason>\n" +
            "            <soapenv:Role>http://localhost:8080/wsman</soapenv:Role>\n" +
            "            <soapenv:Detail>\n" +
            "                <l7:policyResult status=\"Something went wrong\" xmlns:l7=\"http://www.layer7tech.com/ws/policy/fault\"/>\n" +
            "            </soapenv:Detail>\n" +
            "        </soapenv:Fault>\n" +
            "    </soapenv:Body>\n" +
            "</soapenv:Envelope>";

    private static final String FAULT_3 = // fault with details
            "<soapenv:Envelope xmlns:soapenv=\"http://www.w3.org/2003/05/soap-envelope\">\n" +
            "    <soapenv:Body>\n" +
            "        <soapenv:Fault>\n" +
            "            <soapenv:Code>\n" +
            "                <soapenv:Value>soapenv:Receiver</soapenv:Value>\n" +
            "            </soapenv:Code>\n" +
            "            <soapenv:Reason>\n" +
            "                <soapenv:Text xml:lang=\"en-US\">Policy Falsified</soapenv:Text>\n" +
            "            </soapenv:Reason>\n" +
            "            <soapenv:Role>http://localhost:8080/wsman</soapenv:Role>\n" +
            "            <soapenv:Detail>\n" +
            "                <l7:policyResult status=\"Authentication Failed\"\n" +
            "                    xmlns:l7=\"http://www.layer7tech.com/ws/policy/fault\" xmlns:l7p=\"http://www.layer7tech.com/ws/policy\">\n" +
            "                    <l7:assertionResult assertion=\"l7p:AuditAssertion\" status=\"No Error\"/>\n" +
            "                    <l7:assertionResult assertion=\"l7p:FaultLevel\" status=\"No Error\"/>\n" +
            "                    <l7:assertionResult assertion=\"l7p:HttpBasic\" status=\"No Error\">\n" +
            "                        <l7:detailMessage id=\"4104\">Found user: admin</l7:detailMessage>\n" +
            "                    </l7:assertionResult>\n" +
            "                    <l7:assertionResult assertion=\"l7p:Authentication\" status=\"Authentication Failed\">\n" +
            "                        <l7:detailMessage id=\"4208\">Authentication failed for identity provider ID -2</l7:detailMessage>\n" +
            "                    </l7:assertionResult>\n" +
            "                </l7:policyResult>\n" +
            "            </soapenv:Detail>\n" +
            "        </soapenv:Fault>\n" +
            "    </soapenv:Body>\n" +
            "</soapenv:Envelope>";

    private static final String FAULT_4 = // fault with stack
            "<env:Envelope xmlns:env=\"http://www.w3.org/2003/05/soap-envelope\" xmlns:mdo=\"http://schemas.wiseman.dev.java.net/metadata/messagetypes\" xmlns:mex=\"http://schemas.xmlsoap.org/ws/2004/09/mex\" xmlns:wsa=\"http://schemas.xmlsoap.org/ws/2004/08/addressing\" xmlns:wse=\"http://schemas.xmlsoap.org/ws/2004/08/eventing\" xmlns:wsen=\"http://schemas.xmlsoap.org/ws/2004/09/enumeration\" xmlns:wsman=\"http://schemas.dmtf.org/wbem/wsman/1/wsman.xsd\" xmlns:wsmeta=\"http://schemas.dmtf.org/wbem/wsman/1/wsman/version1.0.0.a/default-addressing-model.xsd\" xmlns:wxf=\"http://schemas.xmlsoap.org/ws/2004/09/transfer\" xmlns:xs=\"http://www.w3.org/2001/XMLSchema\"><env:Header><wsa:Action xmlns:ns11=\"http://ns.l7tech.com/2010/04/gateway-management\" env:mustUnderstand=\"true\">http://schemas.dmtf.org/wbem/wsman/1/wsman/fault</wsa:Action><wsa:MessageID xmlns:ns11=\"http://ns.l7tech.com/2010/04/gateway-management\" env:mustUnderstand=\"true\">uuid:657e926d-ba01-4477-b80e-1a5e90daa07d</wsa:MessageID><wsa:RelatesTo xmlns:ns11=\"http://ns.l7tech.com/2010/04/gateway-management\">uuid:cd5208f3-e1f0-4dd1-86fd-715c54ffbd69</wsa:RelatesTo><wsa:To xmlns:ns11=\"http://ns.l7tech.com/2010/04/gateway-management\" env:mustUnderstand=\"true\">http://schemas.xmlsoap.org/ws/2004/08/addressing/role/anonymous</wsa:To></env:Header><env:Body><env:Fault xmlns:ns11=\"http://ns.l7tech.com/2010/04/gateway-management\"><env:Code><env:Value>env:Receiver</env:Value><env:Subcode><env:Value>wsman:InternalError</env:Value></env:Subcode></env:Code><env:Reason><env:Text xml:lang=\"en-US\">The service cannot comply with the request due to internal processing errors.</env:Text></env:Reason><env:Detail><env:Text xml:lang=\"en-US\">java.lang.NullPointerException</env:Text><ex:Exception xmlns:ex=\"http://schemas.sun.com/ws/java/exception\">java.lang.NullPointerException<ex:StackTrace><ex:t>WsdlOperationServiceResolver:68</ex:t><ex:t>ReolutionManagerImpl:171</ex:t><ex:t>ReolutionManagerImpl:78</ex:t><ex:t>NativeMethodAccessorImpl:-2</ex:t><ex:t>NativeMethodAccessorImpl:39</ex:t><ex:t>DelegatingMethodAccessorImpl:25</ex:t><ex:t>Method:597</ex:t><ex:t>AopUtils:307</ex:t><ex:t>ReflectiveMethodInvocation:182</ex:t><ex:t>ReflectiveMethodInvocation:149</ex:t><ex:t>TransactionInterceptor:106</ex:t><ex:t>ReflectiveMethodInvocation:171</ex:t><ex:t>JdkDynamicAopProxy:204</ex:t><ex:t>ServiceManagerImp:146</ex:t><ex:t>ServiceManagerImp:44</ex:t><ex:t>NativeMethodAccessorImpl:-2</ex:t><ex:t>NativeMethodAccessorImpl:39</ex:t><ex:t>DelegatingMethodAccessorImpl:25</ex:t><ex:t>Method:597</ex:t><ex:t>AopUtils:307</ex:t><ex:t>ReflectiveMethodInvocation:182</ex:t><ex:t>ReflectiveMethodInvocation:149</ex:t><ex:t>TransactionInterceptor:106</ex:t><ex:t>ReflectiveMethodInvocation:171</ex:t><ex:t>JdkDynamicAopProxy:204</ex:t><ex:t>PolicyVersioningServiceManager:152</ex:t><ex:t>PolicyVersioningServiceManager:29</ex:t><ex:t>EntityManagerResourceFactory:171</ex:t><ex:t>EntityManagerResourceFactory:140</ex:t><ex:t>ResourceFactorySupport:305</ex:t><ex:t>TransactionTemplate:128</ex:t><ex:t>ResourceFactorySupport:301</ex:t><ex:t>EntityManagerResourceFactory:140</ex:t><ex:t>ResourceHandler:167</ex:t><ex:t>DefaultHandler:65</ex:t><ex:t>ResourceHandler:72</ex:t><ex:t>ResourceHandler:101</ex:t><ex:t>RemoteUtils:32</ex:t><ex:t>ResourceHandler:97</ex:t><ex:t>ResourceHandler:93</ex:t><ex:t>AccessController:-2</ex:t><ex:t>Subject:396</ex:t><ex:t>ResourceHandler:93</ex:t><ex:t>NativeMethodAccessorImpl:-2</ex:t><ex:t>NativeMethodAccessorImpl:39</ex:t><ex:t>DelegatingMethodAccessorImpl:25</ex:t><ex:t>Method:597</ex:t><ex:t>ReflectiveRequestDispatcher:109</ex:t><ex:t>ReflectiveRequestDispatcher:51</ex:t><ex:t>FutureTask:303</ex:t><ex:t>FutureTask:138</ex:t><ex:t>Executors:441</ex:t><ex:t>FutureTask:303</ex:t><ex:t>FutureTask:138</ex:t><ex:t>ThreadPoolExecutor:886</ex:t><ex:t>ThreadPoolExecutor:908</ex:t><ex:t>Thread:619</ex:t></ex:StackTrace></ex:Exception></env:Detail></env:Fault></env:Body></env:Envelope>";
}
