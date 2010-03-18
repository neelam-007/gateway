package com.l7tech.external.assertions.gatewaymanagement.server;

import static org.junit.Assert.*;

import com.l7tech.gateway.api.ManagedObjectFactory;
import com.l7tech.gateway.api.Resource;
import com.l7tech.gateway.api.ResourceSet;
import com.l7tech.util.Functions;
import org.junit.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

/**
 *
 */
public class ResourceHelperTest {

    //- PUBLIC

    @Test
    public void testGetResourceSetMap() throws ResourceFactory.InvalidResourceException {
        final ResourceHelper helper = getResourceHelper();

        final ResourceSet resourceSet = ManagedObjectFactory.createResourceSet();
        resourceSet.setTag( ResourceHelper.POLICY_TAG );

        final Map<String,ResourceSet> map = helper.getResourceSetMap( Arrays.asList( resourceSet ));
        assertEquals( "ResourceSet map size", 1, map.size() );
        assertEquals( "ResourceSet map key", ResourceHelper.POLICY_TAG, map.keySet().iterator().next() );
    }

    @Test(expected=ResourceFactory.InvalidResourceException.class)
    public void testGetResourceSetMapInvalidTag() throws ResourceFactory.InvalidResourceException {
        final ResourceHelper helper = getResourceHelper();

        final ResourceSet resourceSet = ManagedObjectFactory.createResourceSet();
        helper.getResourceSetMap( Arrays.asList( resourceSet ));
    }
    
    @Test(expected=ResourceFactory.InvalidResourceException.class)
    public void testGetResourceSetMapDuplicateTag() throws ResourceFactory.InvalidResourceException {
        final ResourceHelper helper = getResourceHelper();

        final ResourceSet resourceSet1 = ManagedObjectFactory.createResourceSet();
        resourceSet1.setTag( ResourceHelper.POLICY_TAG );

        final ResourceSet resourceSet2 = ManagedObjectFactory.createResourceSet();
        resourceSet2.setTag( ResourceHelper.POLICY_TAG );

        helper.getResourceSetMap( Arrays.asList( resourceSet1, resourceSet2 ));
    }

    @Test
    public void testGetResource() throws ResourceFactory.InvalidResourceException {
        final ResourceHelper helper = getResourceHelper();

        final Resource resource = ManagedObjectFactory.createResource();
        resource.setType( ResourceHelper.WSDL_TYPE );
        resource.setContent( WSDL );

        final ResourceSet resourceSet = ManagedObjectFactory.createResourceSet();
        resourceSet.setTag( ResourceHelper.WSDL_TAG );
        resourceSet.setResources( Arrays.asList( resource ) );

        final Map<String,ResourceSet> map = helper.getResourceSetMap( Arrays.asList( resourceSet ));

        final Resource resourceResult = helper.getResource( map, ResourceHelper.WSDL_TAG, ResourceHelper.WSDL_TYPE, true, null );
        assertNotNull( "resource", resourceResult );
        assertEquals( "resource type", ResourceHelper.WSDL_TYPE, resourceResult.getType() );
    }

    @Test(expected=ResourceFactory.InvalidResourceException.class)
    public void testGetResourceMissingRequired() throws ResourceFactory.InvalidResourceException {
        final ResourceHelper helper = getResourceHelper();

        final ResourceSet resourceSet = ManagedObjectFactory.createResourceSet();
        resourceSet.setTag( ResourceHelper.WSDL_TAG );

        final Map<String,ResourceSet> map = helper.getResourceSetMap( Arrays.asList( resourceSet ));

        helper.getResource( map, ResourceHelper.WSDL_TAG, ResourceHelper.WSDL_TYPE, true, null );
    }

    @Test(expected=ResourceFactory.InvalidResourceException.class)
    public void testGetResourceIncorrectType() throws ResourceFactory.InvalidResourceException {
        final ResourceHelper helper = getResourceHelper();

        final Resource resource = ManagedObjectFactory.createResource();
        resource.setType( ResourceHelper.SCHEMA_TYPE );
        resource.setContent( XSD );

        final ResourceSet resourceSet = ManagedObjectFactory.createResourceSet();
        resourceSet.setTag( ResourceHelper.WSDL_TAG );
        resourceSet.setResources( Arrays.asList( resource ) );

        final Map<String,ResourceSet> map = helper.getResourceSetMap( Arrays.asList( resourceSet ));

        helper.getResource( map, ResourceHelper.WSDL_TAG, ResourceHelper.WSDL_TYPE, true, null );
    }

    @Test(expected=ResourceFactory.InvalidResourceException.class)
    public void testGetResourcesNoRoot() throws ResourceFactory.InvalidResourceException {
        final ResourceHelper helper = getResourceHelper();

        final Resource resourceWsdl = ManagedObjectFactory.createResource();
        resourceWsdl.setType( ResourceHelper.WSDL_TYPE );
        resourceWsdl.setContent( WSDL );

        final Resource resourceXsd = ManagedObjectFactory.createResource();
        resourceXsd.setType( ResourceHelper.SCHEMA_TYPE );
        resourceXsd.setContent( XSD );

        final ResourceSet resourceSet = ManagedObjectFactory.createResourceSet();
        resourceSet.setTag( ResourceHelper.WSDL_TAG );
        resourceSet.setResources( Arrays.asList( resourceWsdl, resourceXsd ) );

        final Map<String,ResourceSet> map = helper.getResourceSetMap( Arrays.asList( resourceSet ));

        helper.getResources( map, ResourceHelper.WSDL_TAG, true, null );
    }

    @Test(expected=ResourceFactory.InvalidResourceException.class)
    public void testGetResourcesInvalidRoot() throws ResourceFactory.InvalidResourceException {
        final ResourceHelper helper = getResourceHelper();

        final Resource resourceWsdl = ManagedObjectFactory.createResource();
        resourceWsdl.setType( ResourceHelper.WSDL_TYPE );
        resourceWsdl.setContent( WSDL );

        final Resource resourceXsd = ManagedObjectFactory.createResource();
        resourceXsd.setType( ResourceHelper.SCHEMA_TYPE );
        resourceXsd.setContent( XSD );

        final ResourceSet resourceSet = ManagedObjectFactory.createResourceSet();
        resourceSet.setTag( ResourceHelper.WSDL_TAG );
        resourceSet.setRootUrl( "http://localhost/nomatch" );
        resourceSet.setResources( Arrays.asList( resourceWsdl, resourceXsd ) );

        final Map<String,ResourceSet> map = helper.getResourceSetMap( Arrays.asList( resourceSet ));

        helper.getResources( map, ResourceHelper.WSDL_TAG, true, null );
    }

    @Test
    public void testGetResourcesResolve() throws ResourceFactory.InvalidResourceException {
        final ResourceHelper helper = getResourceHelper();

        final ResourceSet resourceSet = ManagedObjectFactory.createResourceSet();
        resourceSet.setTag( ResourceHelper.WSDL_TAG );
        resourceSet.setRootUrl( "http://localhost/test.wsdl" );

        final Map<String,ResourceSet> map = helper.getResourceSetMap( Arrays.asList( resourceSet ));

        final Collection<Resource> resources = helper.getResources( map, ResourceHelper.WSDL_TAG, false, new Functions.UnaryThrows<String, String, IOException>(){
            @Override
            public String call( final String url ) throws IOException {
                if ( "http://localhost/test.wsdl".equals( url ) ) {
                    return WSDL;
                }
                return null;
            }
        } );

        assertEquals( "resources size", 1, resources.size() );
        final Resource resource = resources.iterator().next();
        assertNotNull( "resource", resource );
        assertEquals( "resource source url", "http://localhost/test.wsdl", resource.getSourceUrl() );
        assertEquals( "resource type", ResourceHelper.WSDL_TYPE, resource.getType() );
        assertEquals( "resource content", WSDL, resource.getContent() );
        assertNull( "resource id", resource.getId());
        assertNull( "resource version", resource.getVersion());
    }

    @Test(expected=ResourceFactory.InvalidResourceException.class)
    public void testGetResourcesResolveInvalid() throws ResourceFactory.InvalidResourceException {
        final ResourceHelper helper = getResourceHelper();

        final ResourceSet resourceSet = ManagedObjectFactory.createResourceSet();
        resourceSet.setTag( ResourceHelper.WSDL_TAG );
        resourceSet.setRootUrl( "http://localhost/test.wsdl" );

        final Map<String,ResourceSet> map = helper.getResourceSetMap( Arrays.asList( resourceSet ));

        helper.getResources( map, ResourceHelper.WSDL_TAG, false, new Functions.UnaryThrows<String, String, IOException>(){
            @Override
            public String call( final String url ) throws IOException {
                return null;
            }
        } );
    }

    @Test
    public void testGetType() {
        final ResourceHelper helper = getResourceHelper();

        { // test get from URL file extension
            final String type = helper.getType( "http://localhost/test.wsdl", XSD, ResourceHelper.SCHEMA_TYPE );
            assertEquals( "wsdl type from url", ResourceHelper.WSDL_TYPE, type );
        }

        { // test get from URL file extension
            final String type = helper.getType( "http://localhost/test.xsd", WSDL, ResourceHelper.WSDL_TYPE );
            assertEquals( "schema type from url", ResourceHelper.SCHEMA_TYPE, type );
        }

        { // test get from content
            final String type = helper.getType( "http://localhost/testwsdl.xml", WSDL, ResourceHelper.SCHEMA_TYPE );
            assertEquals( "wsdl type from content", ResourceHelper.WSDL_TYPE, type );
        }

        { // test get from content
            final String type = helper.getType( "http://localhost/testschema.xml", XSD, ResourceHelper.WSDL_TYPE );
            assertEquals( "schema type from content", ResourceHelper.SCHEMA_TYPE, type );
        }

        { // test get from default
            final String type = helper.getType( "http://localhost/some.xml", "<test/>", ResourceHelper.SCHEMA_TYPE );
            assertEquals( "schema type from default", ResourceHelper.SCHEMA_TYPE, type );
        }
    }

    @Test(expected=ResourceFactory.InvalidResourceException.class)
    public void testGetTypeFailure() throws ResourceFactory.InvalidResourceException {
        final ResourceHelper helper = getResourceHelper();
        helper.getType( "http://localhost/test.xml", "", ResourceHelper.SCHEMA_TYPE, true );
    }

    //- PRIVATE

    private static final String WSDL = "<wsdl:definitions xmlns:wsdl=\"http://schemas.xmlsoap.org/wsdl/\" targetNamespace=\"http://warehouse.acme.com/ws\"/>";
    private static final String XSD = "<schema xmlns=\"http://www.w3.org/2001/XMLSchema\"/>";

    private ResourceHelper getResourceHelper() {
        return new ResourceHelper();
    }
}
