package com.l7tech.server.communityschemas;

import com.l7tech.common.http.GenericHttpHeaders;
import com.l7tech.common.http.HttpHeader;
import com.l7tech.security.MockGenericHttpClient;
import com.l7tech.server.util.TestingHttpClientFactory;
import com.l7tech.util.MockConfig;
import com.l7tech.util.MockTimer;
import com.l7tech.xml.tarari.TarariMessageContext;
import com.l7tech.xml.tarari.TarariSchemaHandler;
import com.l7tech.xml.tarari.TarariSchemaSource;
import org.xml.sax.SAXException;

import static org.junit.Assert.*;
import org.junit.Test;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 *
 */
public class SchemaManagerImplTest {

    @Test
    public void testHttpFail() throws Exception {
        try {
            final SchemaManager manager = getTestSchemaManager( null );
            manager.getSchemaByUri( "http://bad" ).close();
        } catch ( IOException ioe ) {
            ioe.printStackTrace();
            assertTrue( "404 error", ioe.getMessage().contains( "404" ));
        }
    }

    @Test(expected=SAXException.class)
    public void testParseFail() throws Exception {
        final SchemaManager manager = getTestSchemaManager( null );
        manager.registerSchema( "http://bad/html.html", null, "<html><body>HTML content<br></body></html>" );
        manager.getSchemaByUri( "http://bad/html.html" ).close();
    }

    /**
     * Test that a schema with no TNS is loaded to hardware.
     */
    @Test
    public void testHardwareEnabledNoTargetNamespace() throws Exception {
        final Map<String,TarariSchemaSource> hardwareSchemas = new HashMap<String,TarariSchemaSource>();
        final SchemaManager manager = getTestSchemaManager( hardwareSchemas );
        manager.registerSchema( "http://host/schema.xsd", null, "<schema xmlns=\"http://www.w3.org/2001/XMLSchema\"/>" );
        manager.getSchemaByUri( "http://host/schema.xsd" ).close();
        assertTrue( "Hardware loaded", !hardwareSchemas.isEmpty() );
    }

    /**
     * Test that an included schema without TNS is loaded to hardware.
     */
    @Test
    public void testHardwareEnabledIncludeNoTNS() throws Exception {
        final Map<String,TarariSchemaSource> hardwareSchemas = new HashMap<String,TarariSchemaSource>();
        final SchemaManager manager = getTestSchemaManager( hardwareSchemas );
        manager.registerSchema( "http://host/schema_child.xsd", null, "<schema xmlns=\"http://www.w3.org/2001/XMLSchema\"/>" );
        manager.registerSchema( "http://host/schema_parent.xsd", null, "<schema xmlns=\"http://www.w3.org/2001/XMLSchema\"><include schemaLocation=\"schema_child.xsd\"/></schema>" );
        manager.getSchemaByUri( "http://host/schema_parent.xsd" ).close();

        assertFalse( "Hardware loaded", hardwareSchemas.isEmpty() );
        assertNotNull( "Hardware loaded child", hardwareSchemas.get("http://host/schema_child.xsd") );
        assertNotNull( "Hardware loaded parent", hardwareSchemas.get("http://host/schema_parent.xsd") );
        assertTrue( "Hardware child is include", hardwareSchemas.get("http://host/schema_child.xsd").isInclude() );
        assertFalse( "Hardware parent is include", hardwareSchemas.get("http://host/schema_parent.xsd").isInclude() );
    }

    /**
     * Test that an included schema with TNS is loaded to hardware.
     */
    @Test
    public void testHardwareEnabledIncludeWithTNS() throws Exception {
        final Map<String,TarariSchemaSource> hardwareSchemas = new HashMap<String,TarariSchemaSource>();
        final SchemaManager manager = getTestSchemaManager( hardwareSchemas );
        manager.registerSchema( "http://host/schema_child.xsd", null, "<schema xmlns=\"http://www.w3.org/2001/XMLSchema\" targetNamespace=\"urn:test\"/>" );
        manager.registerSchema( "http://host/schema_parent.xsd", null, "<schema xmlns=\"http://www.w3.org/2001/XMLSchema\"  targetNamespace=\"urn:test\"><include schemaLocation=\"schema_child.xsd\"/></schema>" );
        manager.getSchemaByUri( "http://host/schema_parent.xsd" ).close();

        assertFalse( "Hardware loaded", hardwareSchemas.isEmpty() );
        assertNotNull( "Hardware loaded child", hardwareSchemas.get("http://host/schema_child.xsd") );
        assertNotNull( "Hardware loaded parent", hardwareSchemas.get("http://host/schema_parent.xsd") );
        assertTrue( "Hardware child is include", hardwareSchemas.get("http://host/schema_child.xsd").isInclude() );
        assertFalse( "Hardware parent is include", hardwareSchemas.get("http://host/schema_parent.xsd").isInclude() );
    }

    /**
     * Test that a schema that is both an import and an include is not hardware
     * eligible if there is a TNS conflict (with the parent in this case)
     */
    @Test
    public void testHardwareEnabledImportedAndIncluded() throws Exception {
        final Map<String,TarariSchemaSource> hardwareSchemas = new HashMap<String,TarariSchemaSource>();
        final SchemaManager manager = getTestSchemaManager( hardwareSchemas );
        manager.registerSchema( "http://host/schema_child.xsd", null, "<schema xmlns=\"http://www.w3.org/2001/XMLSchema\" targetNamespace=\"urn:test\"/>" );
        manager.registerSchema( "http://host/schema_includer.xsd", null, "<schema xmlns=\"http://www.w3.org/2001/XMLSchema\"  targetNamespace=\"urn:test\"><include schemaLocation=\"schema_child.xsd\"/></schema>" );
        manager.registerSchema( "http://host/schema_importer.xsd", null, "<schema xmlns=\"http://www.w3.org/2001/XMLSchema\"  targetNamespace=\"urn:test2\"><import schemaLocation=\"schema_child.xsd\" namespace=\"urn:test\"/></schema>" );
        manager.getSchemaByUri( "http://host/schema_includer.xsd" ).close();
        manager.getSchemaByUri( "http://host/schema_importer.xsd" ).close();

        assertTrue( "Not hardware loaded", hardwareSchemas.isEmpty() );
    }

    /**
     * Test that there can be multiple includes with the same TNS loaded to hardware.
     */
    @Test
    public void testHardwareEnabledIncludesWithDuplicateTNS() throws Exception {
        final Map<String,TarariSchemaSource> hardwareSchemas = new HashMap<String,TarariSchemaSource>();
        final SchemaManager manager = getTestSchemaManager( hardwareSchemas );
        manager.registerSchema( "http://host/schema_child1.xsd", null, "<schema xmlns=\"http://www.w3.org/2001/XMLSchema\" targetNamespace=\"urn:test\"/>" );
        manager.registerSchema( "http://host/schema_child2.xsd", null, "<schema xmlns=\"http://www.w3.org/2001/XMLSchema\" targetNamespace=\"urn:test\"/>" );
        manager.registerSchema( "http://host/schema_child3.xsd", null, "<schema xmlns=\"http://www.w3.org/2001/XMLSchema\" targetNamespace=\"urn:test\"/>" );
        manager.registerSchema( "http://host/schema_child4.xsd", null, "<schema xmlns=\"http://www.w3.org/2001/XMLSchema\" targetNamespace=\"urn:test\"/>" );
        manager.registerSchema( "http://host/schema_child5.xsd", null, "<schema xmlns=\"http://www.w3.org/2001/XMLSchema\" targetNamespace=\"urn:test\"/>" );
        manager.registerSchema( "http://host/schema_parent.xsd", null, "<schema xmlns=\"http://www.w3.org/2001/XMLSchema\"  targetNamespace=\"urn:test\">\n" +
                "<include schemaLocation=\"schema_child1.xsd\"/>\n" +
                "<include schemaLocation=\"schema_child2.xsd\"/>\n" +
                "<include schemaLocation=\"schema_child3.xsd\"/>\n" +
                "<include schemaLocation=\"schema_child4.xsd\"/>\n" +
                "<include schemaLocation=\"schema_child5.xsd\"/>\n" +
                "</schema>" );
        manager.getSchemaByUri( "http://host/schema_parent.xsd" ).close();

        assertFalse( "Hardware loaded", hardwareSchemas.isEmpty() );
        assertNotNull( "Hardware loaded child 1", hardwareSchemas.get("http://host/schema_child1.xsd") );
        assertTrue( "Hardware child 1 is include", hardwareSchemas.get("http://host/schema_child1.xsd").isInclude() );
        assertNotNull( "Hardware loaded child 2", hardwareSchemas.get("http://host/schema_child2.xsd") );
        assertTrue( "Hardware child 2 is include", hardwareSchemas.get("http://host/schema_child2.xsd").isInclude() );
        assertNotNull( "Hardware loaded child 3", hardwareSchemas.get("http://host/schema_child3.xsd") );
        assertTrue( "Hardware child 3 is include", hardwareSchemas.get("http://host/schema_child3.xsd").isInclude() );
        assertNotNull( "Hardware loaded child 4", hardwareSchemas.get("http://host/schema_child4.xsd") );
        assertTrue( "Hardware child 4 is include", hardwareSchemas.get("http://host/schema_child4.xsd").isInclude() );
        assertNotNull( "Hardware loaded child 5", hardwareSchemas.get("http://host/schema_child5.xsd") );
        assertTrue( "Hardware child 5 is include", hardwareSchemas.get("http://host/schema_child5.xsd").isInclude() );
        assertNotNull( "Hardware loaded parent", hardwareSchemas.get("http://host/schema_parent.xsd") );
        assertFalse( "Hardware parent is include", hardwareSchemas.get("http://host/schema_parent.xsd").isInclude() );
    }

    /**
     * Test that there can be multiple includes with no TNS loaded to hardware.
     */
    @Test
    public void testHardwareEnabledIncludesWithNoTNS() throws Exception {
        final Map<String,TarariSchemaSource> hardwareSchemas = new HashMap<String,TarariSchemaSource>();
        final SchemaManager manager = getTestSchemaManager( hardwareSchemas );
        manager.registerSchema( "http://host/schema_child1.xsd", null, "<schema xmlns=\"http://www.w3.org/2001/XMLSchema\" />" );
        manager.registerSchema( "http://host/schema_child2.xsd", null, "<schema xmlns=\"http://www.w3.org/2001/XMLSchema\" />" );
        manager.registerSchema( "http://host/schema_child3.xsd", null, "<schema xmlns=\"http://www.w3.org/2001/XMLSchema\" />" );
        manager.registerSchema( "http://host/schema_parent1.xsd", null, "<schema xmlns=\"http://www.w3.org/2001/XMLSchema\" targetNamespace=\"urn:test1\"><include schemaLocation=\"schema_child1.xsd\"/></schema>" );
        manager.registerSchema( "http://host/schema_parent2.xsd", null, "<schema xmlns=\"http://www.w3.org/2001/XMLSchema\" targetNamespace=\"urn:test2\"><include schemaLocation=\"schema_child2.xsd\"/></schema>" );
        manager.registerSchema( "http://host/schema_parent3.xsd", null, "<schema xmlns=\"http://www.w3.org/2001/XMLSchema\" targetNamespace=\"urn:test3\"><include schemaLocation=\"schema_child3.xsd\"/></schema>" );
        manager.getSchemaByUri( "http://host/schema_parent1.xsd" ).close();
        manager.getSchemaByUri( "http://host/schema_parent2.xsd" ).close();
        manager.getSchemaByUri( "http://host/schema_parent3.xsd" ).close();

        assertFalse( "Hardware loaded", hardwareSchemas.isEmpty() );
        assertNotNull( "Hardware loaded child 1", hardwareSchemas.get("http://host/schema_child1.xsd") );
        assertTrue( "Hardware child 1 is include", hardwareSchemas.get("http://host/schema_child1.xsd").isInclude() );
        assertNotNull( "Hardware loaded child 2", hardwareSchemas.get("http://host/schema_child2.xsd") );
        assertTrue( "Hardware child 2 is include", hardwareSchemas.get("http://host/schema_child2.xsd").isInclude() );
        assertNotNull( "Hardware loaded child 3", hardwareSchemas.get("http://host/schema_child3.xsd") );
        assertTrue( "Hardware child 3 is include", hardwareSchemas.get("http://host/schema_child3.xsd").isInclude() );
        assertNotNull( "Hardware loaded parent 1", hardwareSchemas.get("http://host/schema_parent1.xsd") );
        assertFalse( "Hardware parent 1 is include", hardwareSchemas.get("http://host/schema_parent1.xsd").isInclude() );
        assertNotNull( "Hardware loaded parent 2", hardwareSchemas.get("http://host/schema_parent2.xsd") );
        assertFalse( "Hardware parent 2 is include", hardwareSchemas.get("http://host/schema_parent2.xsd").isInclude() );
        assertNotNull( "Hardware loaded parent 3", hardwareSchemas.get("http://host/schema_parent3.xsd") );
        assertFalse( "Hardware parent 3 is include", hardwareSchemas.get("http://host/schema_parent3.xsd").isInclude() );
    }

    /**
     * Test hardware enabled redefine
     */
    @Test
    public void testHardwareEnabledRedefine() throws Exception {
        final Map<String,TarariSchemaSource> hardwareSchemas = new HashMap<String,TarariSchemaSource>();
        final SchemaManager manager = getTestSchemaManager( hardwareSchemas );
        manager.registerSchema( "http://host/schema_child.xsd", null, "<schema xmlns=\"http://www.w3.org/2001/XMLSchema\" targetNamespace=\"urn:test\"/>" );
        manager.registerSchema( "http://host/schema_parent.xsd", null, "<schema xmlns=\"http://www.w3.org/2001/XMLSchema\"  targetNamespace=\"urn:test\"><redefine schemaLocation=\"schema_child.xsd\"/></schema>" );
        manager.getSchemaByUri( "http://host/schema_parent.xsd" ).close();

        assertFalse( "Hardware loaded", hardwareSchemas.isEmpty() );
        assertNotNull( "Hardware loaded child", hardwareSchemas.get("http://host/schema_child.xsd") );
        assertNotNull( "Hardware loaded parent", hardwareSchemas.get("http://host/schema_parent.xsd") );
        assertTrue( "Hardware child is include", hardwareSchemas.get("http://host/schema_child.xsd").isInclude() );
        assertFalse( "Hardware parent is include", hardwareSchemas.get("http://host/schema_parent.xsd").isInclude() );
    }

    private SchemaManager getTestSchemaManager( final Map<String,TarariSchemaSource> hardwareLoaded ) {
        final TarariSchemaHandler testSchemaHandler = new TarariSchemaHandler() {
            @Override
            public Map<TarariSchemaSource, Exception> setHardwareSchemas( final HashMap<String, ? extends TarariSchemaSource> hardwareSchemas ) {
                if ( hardwareLoaded != null ) {
                    hardwareLoaded.clear();
                    hardwareLoaded.putAll( hardwareSchemas );
                }
                return Collections.emptyMap();
            }

            @Override
            public boolean validate( final TarariMessageContext tarariMsg ) throws SAXException {
                throw new SAXException( "Validation not supported with mock schema handler" );
            }
        };

        final TestingHttpClientFactory httpClientFactory = new TestingHttpClientFactory();
        httpClientFactory.setMockHttpClient( new MockGenericHttpClient( 404, new GenericHttpHeaders( new HttpHeader[0]), null, null, null ) );

        final SchemaConfiguration schemaConfiguration = new SchemaConfiguration(new MockConfig(new Properties(){{ setProperty("schemaRecompileLatency", "0"); }}));

        return new SchemaManagerImpl(
                schemaConfiguration,
                new MockTimer(),
                testSchemaHandler,
                new SchemaSourceResolver[]{
                        new HttpSchemaSourceResolver( schemaConfiguration, httpClientFactory )
                }
                ){
            @Override
            public SchemaHandle getSchemaByUri( final String url ) throws IOException, SAXException {
                final SchemaHandle handle = super.getSchemaByUri( url );
                maybeRebuildHardwareCache(0);
                return handle;
            }

            @Override
            boolean shouldRebuildNow(long scheduledTime) {
                return true;
            }
        };
    }
}
