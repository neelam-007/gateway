package com.l7tech.console.panels;

import com.l7tech.common.io.*;
import com.l7tech.gateway.common.resources.*;
import com.l7tech.objectmodel.Goid;
import com.l7tech.test.BugNumber;
import com.l7tech.util.Functions;
import org.junit.Test;

import java.io.IOException;
import java.net.URI;
import java.util.*;
import java.util.logging.Logger;

import static com.l7tech.console.panels.GlobalResourceImportContext.*;
import static com.l7tech.console.panels.GlobalResourceImportWizard.*;
import static org.junit.Assert.*;

/**
 * Unit tests for global resource import wizard.
 */
public class GlobalResourceImportWizardTest {

    @Test
    public void testResourceInitialization() throws Exception {
        final ResourceEntry initial1 = resource( "http://localhost:8080/schema1.xsd", SCHEMA1, targetNamespace(SCHEMA1) );
        final ResourceEntry initial2 = resource( "http://localhost:8080/schema2.xsd", SCHEMA1, targetNamespace(SCHEMA1) );
        final Collection<ResourceEntryHeader> initialHeaders = Arrays.asList( header(initial1), header(initial2) );
        final ResourceAdmin resourceAdmin = new ResourceAdminStub( Arrays.asList( initial1, initial2 ) );
        final GlobalResourceImportContext context = GlobalResourceImportWizard.buildContext( null, initialHeaders, resourceAdmin, getLoggingErrorListener() );
        assertNotNull( "Import context", context );
        assertNotNull( "Import context input sources", context.getResourceInputSources() );
        assertEquals( "Import context input source count", 2, context.getResourceInputSources().size() );
        assertEquals( "Import context input source [0] uri", initial1.getUri(), context.getResourceInputSources().get( 0 ).getUri().toString() );
        assertEquals( "Import context input source [1] uri", initial2.getUri(), context.getResourceInputSources().get( 1 ).getUri().toString() );
    }

    @Test
    public void testDependencyResolve() throws Exception {
        final ResourceEntry initial1 = resource( "http://localhost:8080/schema1.xsd", SCHEMA1, targetNamespace(SCHEMA1) );
        final ResourceEntry initial2 = resource( "http://localhost:8080/schema2.xsd", SCHEMA1, targetNamespace(SCHEMA1) );
        final Collection<ResourceEntryHeader> initialHeaders = Arrays.asList( header(initial1), header(initial2) );
        final ResourceAdmin resourceAdmin = new ResourceAdminStub( Arrays.asList( initial1, initial2 ) );
        final List<GlobalResourceImportContext.ResourceHolder> resourceHolders = new ArrayList<GlobalResourceImportContext.ResourceHolder>(GlobalResourceImportWizard.resolveDependencies(
                new HashSet<String>( Functions.map( initialHeaders, Functions.<String,ResourceEntryHeader>propertyTransform( ResourceEntryHeader.class, "uri" ))),
                resourceAdmin,
                getLoggingErrorListener() ) );
        assertNotNull( "Resource holders", resourceHolders );
        assertEquals( "Resource holders count", 2, resourceHolders.size() );
        assertEquals( "Resource holders [0] uri", initial1.getUri(), resourceHolders.get( 0 ).getSystemId() );
        assertEquals( "Resource holders [1] uri", initial2.getUri(), resourceHolders.get( 1 ).getSystemId() );
    }

    @BugNumber(9410) // Global Resources: Datatypes DTD is invalid when analyzed
    @Test
    public void testDependencyResolvePartialDTD() throws Exception {
        final ResourceEntry initial = resource( "http://localhost:8080/dtd_partial1.dtd", DTD_PARTIAL1, "partial1" );
        final Collection<ResourceEntryHeader> initialHeaders = Arrays.asList( header(initial) );
        final ResourceAdmin resourceAdmin = new ResourceAdminStub( Arrays.asList( initial ) );
        final List<GlobalResourceImportContext.ResourceHolder> resourceHolders = new ArrayList<GlobalResourceImportContext.ResourceHolder>(GlobalResourceImportWizard.resolveDependencies(
                new HashSet<String>( Functions.map( initialHeaders, Functions.<String,ResourceEntryHeader>propertyTransform( ResourceEntryHeader.class, "uri" ))),
                resourceAdmin,
                getLoggingErrorListener() ) );
        assertNotNull( "Resource holders", resourceHolders );
        assertEquals( "Resource holders count", 1, resourceHolders.size() );
        logger.info( "Resource holder [0] status: " + resourceHolders.get( 0 ).getStatus() );
        assertEquals( "Resource holders [0] uri", initial.getUri(), resourceHolders.get( 0 ).getSystemId() );
        assertFalse( "Resource holders [0] error", resourceHolders.get( 0 ).isError() );
        assertFalse( "Resource holders [0] xml", resourceHolders.get( 0 ).isXml() );
    }

    @Test
    public void testDependencyImportNoDependencies() throws Exception {
        final GlobalResourceImportContext context = new GlobalResourceImportContext();
        final ResourceAdmin resourceAdmin = new ResourceAdminStub();
        final ImportAdvisor advisor = buildAdvisor( null, null, null, null );
        final ChoiceSelector choiceSelector = buildChoiceSelector();
        final Functions.UnaryThrows<ResourceEntryHeader, Collection<ResourceEntryHeader>, IOException> entitySelector = buildEntitySelector();
        final ResourceTherapist resourceTherapist = buildResourceTherapist();

        boolean proceed = importDependencies( context, "urn:test", ResourceType.XML_SCHEMA, SCHEMA1, resourceAdmin, null, advisor, null, getLoggingErrorListener(), choiceSelector, entitySelector, resourceTherapist );
        assertTrue( "Import success", proceed );
    }

    @Test
    public void testDependencyImportSimpleDependencies() throws Exception {
        final GlobalResourceImportContext context = new GlobalResourceImportContext();
        final ResourceAdminStub resourceAdmin = new ResourceAdminStub();
        resourceAdmin.setResolver( new Functions.UnaryThrows<String,String,IOException>(){
            @Override
            public String call( final String uri ) throws IOException {
                if ( !uri.equals( "http://localhost:8888/schema1.xsd" ) ) {
                    throw new IOException("Cannot find resource : " + uri);
                }
                return SCHEMA1;
            }
        } );
        final boolean[] importConfirmed = {false};
        final List<ResourceHolder> confirmedResources = new ArrayList<ResourceHolder>();
        final ImportAdvisor advisor = buildAdvisor( DependencyImportChoice.IMPORT, DependencyImportChoice.IMPORT, importConfirmed, confirmedResources );
        final ChoiceSelector choiceSelector = buildChoiceSelector();
        final Functions.UnaryThrows<ResourceEntryHeader, Collection<ResourceEntryHeader>, IOException> entitySelector = buildEntitySelector();
        final ResourceTherapist resourceTherapist = buildResourceTherapist();

        boolean proceed = importDependencies( context, "http://localhost:8888/schema2.xsd", ResourceType.XML_SCHEMA, SCHEMA2, resourceAdmin, null, advisor, null, getLoggingErrorListener(), choiceSelector, entitySelector, resourceTherapist );
        assertTrue( "Import success", proceed );
        assertTrue( "Dependency import confirmed", importConfirmed[0] );
        assertEquals( "Imported resource count", 1, confirmedResources.size() );
        assertEquals( "Imported resource uri", "http://localhost:8888/schema1.xsd", confirmedResources.get(0).getSystemId() );
        assertEquals( "Imported resource type", ResourceType.XML_SCHEMA, confirmedResources.get(0).getType() );
        assertTrue( "Imported resource persist", confirmedResources.get(0).isPersist() );
        assertTrue( "Imported resource xml", confirmedResources.get(0).isXml() );
        assertFalse( "Imported resource error", confirmedResources.get(0).isError() );
    }

    @Test
    public void testDependencyImportMissingDependency() throws Exception {
        final GlobalResourceImportContext context = new GlobalResourceImportContext();
        final ResourceAdminStub resourceAdmin = new ResourceAdminStub();
        resourceAdmin.setResolver( new Functions.UnaryThrows<String,String,IOException>(){
            @Override
            public String call( final String uri ) throws IOException {
                throw new IOException("Cannot find resource : " + uri);
            }
        } );
        final boolean[] importConfirmed = {false};
        final List<ResourceHolder> confirmedResources = new ArrayList<ResourceHolder>();
        final ImportAdvisor advisor = buildAdvisor( DependencyImportChoice.IMPORT, DependencyImportChoice.IMPORT, importConfirmed, confirmedResources );

        final ChoiceSelector choiceSelector = new ChoiceSelector(){
            @Override
            public ImportChoice selectChoice( final ImportOption option, final String optionDetail, final ImportChoice defaultChoice, final String conflictDetail, final String resourceUri, final String resourceDescription ) {
                ImportChoice choice = defaultChoice;
                switch( option ) {
                    case MISSING_RESOURCE:
                        choice = ImportChoice.IMPORT;
                        break;
                    default:
                        fail("Should not be called, choice not required");
                }
                return choice;
            }
        };

        final Functions.UnaryThrows<ResourceEntryHeader, Collection<ResourceEntryHeader>, IOException> entitySelector = buildEntitySelector();

        final ResourceTherapist resourceTherapist = new ResourceTherapist(){
            @Override
            public ResourceDocument consult( final ResourceType resourceType, final String resourceDescription, final ResourceDocument invalidResource, final String invalidDetail ) {
                logger.info("ResourceTherapist: " + resourceDescription);
                logger.info("ResourceTherapist: " + invalidDetail);
                if ( !resourceDescription.contains( "urn:schema1" ) ||
                     !invalidDetail.contains( "http://localhost:8888/schema1.xsd" )) {
                    fail( "Expected failure for urn:schema1/http://localhost:8888/schema1.xsd only" );
                }
                return new URIResourceDocument( URI.create( "http://localhost:8888/schema1Moved.xsd" ), SCHEMA1, null );
            }
        };

        boolean proceed = importDependencies( context, "http://localhost:8888/schema2.xsd", ResourceType.XML_SCHEMA, SCHEMA2, resourceAdmin, null, advisor, null, getLoggingErrorListener(), choiceSelector, entitySelector, resourceTherapist );
        assertTrue( "Import success", proceed );
        assertTrue( "Dependency import confirmed", importConfirmed[0] );
        assertEquals( "Imported resource count", 1, confirmedResources.size() );
        assertEquals( "Imported resource uri", "http://localhost:8888/schema1Moved.xsd", confirmedResources.get(0).getSystemId() );
        assertEquals( "Imported resource type", ResourceType.XML_SCHEMA, confirmedResources.get(0).getType() );
        assertTrue( "Imported resource persist", confirmedResources.get(0).isPersist() );
        assertTrue( "Imported resource xml", confirmedResources.get(0).isXml() );
        assertFalse( "Imported resource error", confirmedResources.get(0).isError() );
    }

    @Test
    public void testDependencyImportInvalidDependency() throws Exception {
        final GlobalResourceImportContext context = new GlobalResourceImportContext();
        final ResourceAdminStub resourceAdmin = new ResourceAdminStub();
        resourceAdmin.setResolver( new Functions.UnaryThrows<String,String,IOException>(){
            @Override
            public String call( final String uri ) throws IOException {
                if ( !uri.equals( "http://localhost:8888/schema1.xsd" ) ) {
                    throw new IOException("Cannot find resource : " + uri);
                }
                return SCHEMA_INVALID;
            }
        } );
        final boolean[] importConfirmed = {false};
        final List<ResourceHolder> confirmedResources = new ArrayList<ResourceHolder>();
        final ImportAdvisor advisor = buildAdvisor( DependencyImportChoice.IMPORT, DependencyImportChoice.IMPORT, importConfirmed, confirmedResources );

        final ChoiceSelector choiceSelector = new ChoiceSelector(){
            @Override
            public ImportChoice selectChoice( final ImportOption option, final String optionDetail, final ImportChoice defaultChoice, final String conflictDetail, final String resourceUri, final String resourceDescription ) {
                ImportChoice choice = defaultChoice;
                switch( option ) {
                    case MISSING_RESOURCE:
                        choice = ImportChoice.IMPORT;
                        break;
                    default:
                        fail("Should not be called, choice not required");
                }
                return choice;
            }
        };

        final Functions.UnaryThrows<ResourceEntryHeader, Collection<ResourceEntryHeader>, IOException> entitySelector = buildEntitySelector();

        final ResourceTherapist resourceTherapist = new ResourceTherapist(){
            @Override
            public ResourceDocument consult( final ResourceType resourceType, final String resourceDescription, final ResourceDocument invalidResource, final String invalidDetail ) {
                logger.info("ResourceTherapist: " + resourceDescription);
                logger.info("ResourceTherapist: " + invalidDetail);
                if ( !resourceDescription.contains( "urn:schema1" ) ) {
                    fail( "Expected failure for urn:schema1 only" );
                }
                return new URIResourceDocument( URI.create( "http://localhost:8888/schema1.xsd" ), SCHEMA1, null );
            }
        };

        boolean proceed = importDependencies( context, "http://localhost:8888/schema2.xsd", ResourceType.XML_SCHEMA, SCHEMA2, resourceAdmin, null, advisor, null, getLoggingErrorListener(), choiceSelector, entitySelector, resourceTherapist );
        assertTrue( "Import success", proceed );
        assertTrue( "Dependency import confirmed", importConfirmed[0] );
        assertEquals( "Imported resource count", 1, confirmedResources.size() );
        assertEquals( "Imported resource uri", "http://localhost:8888/schema1.xsd", confirmedResources.get(0).getSystemId() );
        assertEquals( "Imported resource type", ResourceType.XML_SCHEMA, confirmedResources.get(0).getType() );
        assertTrue( "Imported resource persist", confirmedResources.get(0).isPersist() );
        assertTrue( "Imported resource xml", confirmedResources.get(0).isXml() );
        assertFalse( "Imported resource error", confirmedResources.get(0).isError() );
    }

    @BugNumber(9285) // Global Resources: Target namespace conflicts not detected during import
    @Test
    public void testDependencyImportDuplicateTNS() throws Exception {
        final GlobalResourceImportContext context = new GlobalResourceImportContext();
        final ResourceEntry resourceEntry1 = resource( "http://localhost:8888/schema1_existing.xsd", SCHEMA1, targetNamespace(SCHEMA1) );
        final ResourceAdminStub resourceAdmin = new ResourceAdminStub( Arrays.asList( resourceEntry1 ) );
        resourceAdmin.setResolver( new Functions.UnaryThrows<String,String,IOException>(){
            @Override
            public String call( final String uri ) throws IOException {
                if ( !uri.equals( "http://localhost:8888/schema1.xsd" ) ) {
                    throw new IOException("Cannot find resource : " + uri);
                }
                return SCHEMA1;
            }
        } );
        final boolean[] importConfirmed = {false};
        final List<ResourceHolder> confirmedResources = new ArrayList<ResourceHolder>();
        final ImportAdvisor advisor = buildAdvisor( DependencyImportChoice.IMPORT, DependencyImportChoice.IMPORT, importConfirmed, confirmedResources );

        final ChoiceSelector choiceSelector = new ChoiceSelector(){
            @Override
            public ImportChoice selectChoice( final ImportOption option, final String optionDetail, final ImportChoice defaultChoice, final String conflictDetail, final String resourceUri, final String resourceDescription ) {
                ImportChoice choice = defaultChoice;
                switch( option ) {
                    case CONFLICTING_TARGET_NAMESPACE:
                        choice = ImportChoice.EXISTING;
                        break;
                    default:
                        fail("Should not be called, choice not required");
                }
                return choice;
            }
        };

        final Functions.UnaryThrows<ResourceEntryHeader, Collection<ResourceEntryHeader>, IOException> entitySelector = buildEntitySelector();
        final ResourceTherapist resourceTherapist = buildResourceTherapist();
        final String[] updatedContent = {null};
        final Functions.UnaryVoid<String> contentCallback = new Functions.UnaryVoid<String>(){
            @Override
            public void call( final String content ) {
                updatedContent[0] = content;
            }
        };

        boolean proceed = importDependencies( context, "http://localhost:8888/schema2.xsd", ResourceType.XML_SCHEMA, SCHEMA2, resourceAdmin, null, advisor, contentCallback, getLoggingErrorListener(), choiceSelector, entitySelector, resourceTherapist );
        assertTrue( "Import success", proceed );
        assertTrue( "Dependency import confirmed", importConfirmed[0] );
        assertEquals( "Imported resource count", 1, confirmedResources.size() );
        assertEquals( "Imported resource uri", "http://localhost:8888/schema1_existing.xsd", confirmedResources.get(0).getSystemId() );
        assertEquals( "Imported resource type", ResourceType.XML_SCHEMA, confirmedResources.get(0).getType() );
        assertFalse( "Imported resource persist", confirmedResources.get(0).isPersist() );
        assertTrue( "Imported resource xml", confirmedResources.get(0).isXml() );
        assertFalse( "Imported resource error", confirmedResources.get(0).isError() );
        assertNotNull( "Updated content", updatedContent[0] );
        assertTrue( "Updated content URI", updatedContent[0].contains( "schemaLocation=\"schema1_existing.xsd\"" ));
    }

    @BugNumber(9352) // Global Resources: Import DTDs is not setting Public ID
    @Test
    public void testDependencyImportDTD() throws Exception {
        final GlobalResourceImportContext context = new GlobalResourceImportContext();
        final ResourceAdminStub resourceAdmin = new ResourceAdminStub();
        resourceAdmin.setResolver( new Functions.UnaryThrows<String,String,IOException>(){
            @Override
            public String call( final String uri ) throws IOException {
                if ( !uri.equals( "http://localhost:8888/dtd_partial1.dtd" ) ) {
                    throw new IOException("Cannot find resource : " + uri);
                }
                return DTD_PARTIAL1;
            }
        } );
        final boolean[] importConfirmed = {false};
        final List<ResourceHolder> confirmedResources = new ArrayList<ResourceHolder>();
        final ImportAdvisor advisor = buildAdvisor( DependencyImportChoice.IMPORT, DependencyImportChoice.IMPORT, importConfirmed, confirmedResources );
        final ChoiceSelector choiceSelector = buildChoiceSelector();
        final Functions.UnaryThrows<ResourceEntryHeader, Collection<ResourceEntryHeader>, IOException> entitySelector = buildEntitySelector();
        final ResourceTherapist resourceTherapist = buildResourceTherapist();

        boolean proceed = importDependencies( context, "http://localhost:8888/dtd1.dtd", ResourceType.DTD, DTD1, resourceAdmin, null, advisor, null, getLoggingErrorListener(), choiceSelector, entitySelector, resourceTherapist );
        assertTrue( "Import success", proceed );
        assertTrue( "Dependency import confirmed", importConfirmed[0] );
        assertEquals( "Imported resource count", 1, confirmedResources.size() );
        assertEquals( "Imported resource uri", "http://localhost:8888/dtd_partial1.dtd", confirmedResources.get(0).getSystemId() );
        assertEquals( "Imported resource type", ResourceType.DTD, confirmedResources.get(0).getType() );
        assertEquals( "Imported resource public id", "partial1", confirmedResources.get(0).getPublicId() );
        assertTrue( "Imported resource persist", confirmedResources.get(0).isPersist() );
        assertFalse( "Imported resource xml", confirmedResources.get(0).isXml() );
        assertFalse( "Imported resource error", confirmedResources.get(0).isError() );
    }

    @BugNumber(9349) // Global Resources: Unable to import transitive DTD dependencies
    @Test
    public void testDependencyImportTransitiveDTDs() throws Exception {
        final GlobalResourceImportContext context = new GlobalResourceImportContext();
        final ResourceAdminStub resourceAdmin = new ResourceAdminStub();
        resourceAdmin.setResolver( new Functions.UnaryThrows<String,String,IOException>(){
            @Override
            public String call( final String uri ) throws IOException {
                if ( uri.equals( "http://localhost:8888/dtd_partial1.dtd" ) ) {
                    return DTD_PARTIAL1;
                } else if ( uri.equals( "http://localhost:8888/dtd1.dtd" ) ) {
                    return DTD1;
                }
                throw new IOException("Cannot find resource : " + uri);
            }
        } );
        final boolean[] importConfirmed = {false};
        final List<ResourceHolder> confirmedResources = new ArrayList<ResourceHolder>();
        final ImportAdvisor advisor = buildAdvisor( DependencyImportChoice.IMPORT, DependencyImportChoice.IMPORT, importConfirmed, confirmedResources );
        final ChoiceSelector choiceSelector = buildChoiceSelector();
        final Functions.UnaryThrows<ResourceEntryHeader, Collection<ResourceEntryHeader>, IOException> entitySelector = buildEntitySelector();
        final ResourceTherapist resourceTherapist = buildResourceTherapist();

        boolean proceed = importDependencies( context, "http://localhost:8888/schema_dtd.xsd", ResourceType.XML_SCHEMA, SCHEMA_DTD, resourceAdmin, null, advisor, null, getLoggingErrorListener(), choiceSelector, entitySelector, resourceTherapist );
        assertTrue( "Import success", proceed );
        assertTrue( "Dependency import confirmed", importConfirmed[0] );
        assertEquals( "Imported resource count", 2, confirmedResources.size() );

        assertEquals( "Imported resource uri [0]", "http://localhost:8888/dtd1.dtd", confirmedResources.get(0).getSystemId() );
        assertEquals( "Imported resource type [0]", ResourceType.DTD, confirmedResources.get(0).getType() );
        assertEquals( "Imported resource public id [0]", "dtd1", confirmedResources.get(0).getPublicId() );
        assertTrue( "Imported resource persist [0]", confirmedResources.get(0).isPersist() );
        assertFalse( "Imported resource xml [0]", confirmedResources.get(0).isXml() );
        assertFalse( "Imported resource error [0]", confirmedResources.get(0).isError() );

        assertEquals( "Imported resource uri [1]", "http://localhost:8888/dtd_partial1.dtd", confirmedResources.get(1).getSystemId() );
        assertEquals( "Imported resource type [1]", ResourceType.DTD, confirmedResources.get(1).getType() );
        assertEquals( "Imported resource public id [1]", "partial1", confirmedResources.get(1).getPublicId() );
        assertTrue( "Imported resource persist [1]", confirmedResources.get(1).isPersist() );
        assertFalse( "Imported resource xml [1]", confirmedResources.get(1).isXml() );
        assertFalse( "Imported resource error [1]", confirmedResources.get(1).isError() );
    }

    @Test
    public void testDependencyImportExtraResolver() {
        final GlobalResourceImportContext context = new GlobalResourceImportContext();
        final ResourceAdminStub resourceAdmin = new ResourceAdminStub();
        final boolean[] importConfirmed = {false};
        final List<ResourceHolder> confirmedResources = new ArrayList<ResourceHolder>();
        final ImportAdvisor advisor = buildAdvisor( DependencyImportChoice.IMPORT, DependencyImportChoice.IMPORT, importConfirmed, confirmedResources );
        final ChoiceSelector choiceSelector = buildChoiceSelector();
        final Functions.UnaryThrows<ResourceEntryHeader, Collection<ResourceEntryHeader>, IOException> entitySelector = buildEntitySelector();
        final ResourceTherapist resourceTherapist = buildResourceTherapist();

        final ResourceDocumentResolver extraResolver = new ResourceDocumentResolverSupport(){
            @Override
            public ResourceDocument resolveByUri( final String uri ) throws IOException {
                if ( "http://localhost:8888/schema1.xsd".equals(uri)) {
                    return newResourceDocument(uri, SCHEMA1);
                } else {
                    return null;
                }
            }
        };
        
        boolean proceed = importDependencies( context, "http://localhost:8888/schema2.xsd", ResourceType.XML_SCHEMA, SCHEMA2, resourceAdmin, Collections.singleton( extraResolver ), advisor, null, getLoggingErrorListener(), choiceSelector, entitySelector, resourceTherapist );
        assertTrue( "Import success", proceed );
        assertTrue( "Dependency import confirmed", importConfirmed[0] );
        assertEquals( "Imported resource count", 1, confirmedResources.size() );
        assertEquals( "Imported resource uri", "http://localhost:8888/schema1.xsd", confirmedResources.get(0).getSystemId() );
        assertEquals( "Imported resource type", ResourceType.XML_SCHEMA, confirmedResources.get(0).getType() );
        assertTrue( "Imported resource persist", confirmedResources.get(0).isPersist() );
        assertTrue( "Imported resource xml", confirmedResources.get(0).isXml() );
        assertFalse( "Imported resource error", confirmedResources.get(0).isError() );
    }

    @BugNumber(9437) // Global Resources: Import updates incorrectly the System ID for DTD
    @Test
    public void testDependencyImportExistingDTD() throws Exception {
        final GlobalResourceImportContext context = new GlobalResourceImportContext();
        final ResourceEntry resourceEntry1 = resource( "http://localhost:8888/dtds/dtd1.dtd", DTD1, "dtd1" );
        final ResourceEntry resourceEntry2 = resource( "http://localhost:8888/dtds/dtd_partial1.dtd", DTD_PARTIAL1, "partial1" );
        final ResourceAdminStub resourceAdmin = new ResourceAdminStub( Arrays.asList( resourceEntry1, resourceEntry2 ));
        resourceAdmin.setResolver( new Functions.UnaryThrows<String,String,IOException>(){
            @Override
            public String call( final String uri ) throws IOException {
                if ( uri.equals( "http://localhost:8888/dtds/dtd_partial1.dtd" ) ) {
                    return DTD_PARTIAL1;
                } else if ( uri.equals( "http://localhost:8888/dtds/dtd1.dtd" ) ) {
                    return DTD1;
                }
                throw new IOException("Cannot find resource : " + uri);
            }
        } );
        final boolean[] importConfirmed = {false};
        final List<ResourceHolder> confirmedResources = new ArrayList<ResourceHolder>();
        final ImportAdvisor advisor = buildAdvisor( DependencyImportChoice.IMPORT, DependencyImportChoice.IMPORT, importConfirmed, confirmedResources );
        final ChoiceSelector choiceSelector = new ChoiceSelector(){
            @Override
            public ImportChoice selectChoice( final ImportOption option, final String optionDetail, final ImportChoice defaultChoice, final String conflictDetail, final String resourceUri, final String resourceDescription ) {
                ImportChoice choice = null;
                switch( option ) {
                    case CONFLICTING_URI:
                        choice = ImportChoice.EXISTING;
                        break;
                    default:
                        fail("Unexpected option: " + option);
                }
                return choice;
            }
        };
        final Functions.UnaryThrows<ResourceEntryHeader, Collection<ResourceEntryHeader>, IOException> entitySelector = buildEntitySelector();
        final ResourceTherapist resourceTherapist = buildResourceTherapist();

        // Since the dependencies are already present (we select use existing resource) it
        // is expected that no changes are made to the content and no dependencies are added.
        // The DTD reference in the schema should not be updated since the existing URI will
        // work.
        boolean proceed = importDependencies( context, "http://localhost:8888/path/to/schema/schema_dtd_abs.xsd", ResourceType.XML_SCHEMA, SCHEMA_DTD_ABS, resourceAdmin, null, advisor, null, getLoggingErrorListener(), choiceSelector, entitySelector, resourceTherapist );
        assertTrue( "Import success", proceed );
        assertFalse( "Dependency import confirmed", importConfirmed[0] );
        assertEquals( "Imported resource count", 0, confirmedResources.size() );
    }

    @BugNumber(9475) // Global Resources: Validate assertion unable to import schema that refers to DTDs
    @Test
    public void testDependencyImportDependencyExistingDTD() throws Exception {
        final GlobalResourceImportContext context = new GlobalResourceImportContext();
        final ResourceEntry resourceEntry1 = resource( "http://localhost:8888/dtd1.dtd", DTD1, "dtd1" );
        final ResourceEntry resourceEntry2 = resource( "http://localhost:8888/dtd_partial1.dtd", DTD_PARTIAL1, "partial1" );
        final ResourceAdminStub resourceAdmin = new ResourceAdminStub( Arrays.asList( resourceEntry1, resourceEntry2 ));
        resourceAdmin.setResolver( new Functions.UnaryThrows<String,String,IOException>(){
            @Override
            public String call( final String uri ) throws IOException {
                if ( uri.equals( "http://localhost:8888/dtd_partial1.dtd" ) ) {
                    return DTD_PARTIAL1;
                } else if ( uri.equals( "http://localhost:8888/dtd1.dtd" ) ) {
                    return DTD1;
                } else if ( uri.equals( "http://localhost:8888/schema.xsd" ) ) {
                    return SCHEMA_DTD;
                }
                throw new IOException("Cannot find resource : " + uri);
            }
        } );
        final boolean[] importConfirmed = {false};
        final List<ResourceHolder> confirmedResources = new ArrayList<ResourceHolder>();
        final ImportAdvisor advisor = buildAdvisor( DependencyImportChoice.IMPORT, DependencyImportChoice.IMPORT, importConfirmed, confirmedResources );
        final ChoiceSelector choiceSelector = new ChoiceSelector(){
            @Override
            public ImportChoice selectChoice( final ImportOption option, final String optionDetail, final ImportChoice defaultChoice, final String conflictDetail, final String resourceUri, final String resourceDescription ) {
                ImportChoice choice = null;
                switch( option ) {
                    case CONFLICTING_URI:
                        choice = ImportChoice.EXISTING;
                        break;
                    default:
                        fail("Unexpected option: " + option);
                }
                return choice;
            }
        };
        final Functions.UnaryThrows<ResourceEntryHeader, Collection<ResourceEntryHeader>, IOException> entitySelector = buildEntitySelector();
        final ResourceTherapist resourceTherapist = buildResourceTherapist();

        boolean proceed = importDependencies( context, "http://localhost:8888/schema_import_dtd.xsd", ResourceType.XML_SCHEMA, SCHEMA_IMPORT_DTD, resourceAdmin, null, advisor, null, getLoggingErrorListener(), choiceSelector, entitySelector, resourceTherapist );
        assertTrue( "Import success", proceed );
        assertTrue( "Dependency import confirmed", importConfirmed[0] );
        assertEquals( "Imported resource count", 3, confirmedResources.size() );

        assertEquals( "Imported resource uri [0]", "http://localhost:8888/schema.xsd", confirmedResources.get(0).getSystemId() );
        assertEquals( "Imported resource type [0]", ResourceType.XML_SCHEMA, confirmedResources.get(0).getType() );
        assertEquals( "Imported resource target namespace [0]", null, confirmedResources.get(0).getTargetNamespace() );
        assertTrue( "Imported resource persist [0]", confirmedResources.get(0).isPersist() );
        assertTrue( "Imported resource xml [0]", confirmedResources.get(0).isXml() );
        assertFalse( "Imported resource error [0]", confirmedResources.get(0).isError() );

        assertEquals( "Imported resource uri [1]", "http://localhost:8888/dtd1.dtd", confirmedResources.get(1).getSystemId() );
        assertEquals( "Imported resource type [1]", ResourceType.DTD, confirmedResources.get(1).getType() );
        assertEquals( "Imported resource public id [1]", "dtd1", confirmedResources.get(1).getPublicId() );
        assertFalse( "Imported resource persist [1]", confirmedResources.get(1).isPersist() );
        assertFalse( "Imported resource xml [1]", confirmedResources.get(1).isXml() );
        assertFalse( "Imported resource error [1]", confirmedResources.get(1).isError() );

        assertEquals( "Imported resource uri [2]", "http://localhost:8888/dtd_partial1.dtd", confirmedResources.get(2).getSystemId() );
        assertEquals( "Imported resource type [2]", ResourceType.DTD, confirmedResources.get(2).getType() );
        assertEquals( "Imported resource public id [2]", "partial1", confirmedResources.get(2).getPublicId() );
        assertFalse( "Imported resource persist [2]", confirmedResources.get(2).isPersist() );
        assertFalse( "Imported resource xml [2]", confirmedResources.get(2).isXml() );
        assertFalse( "Imported resource error [2]", confirmedResources.get(2).isError() );

    }

    //- PRIVATE

    private static final Logger logger = Logger.getLogger( GlobalResourceImportWizardTest.class.getName() );

    private GlobalResourceImportWizard.ErrorListener getLoggingErrorListener() {
        return new GlobalResourceImportWizard.ErrorListener(){
            @Override
            public void notifyError( final String title, final String message ) {
                logger.warning( message );
            }
        };
    }

    private ResourceEntry resource( final String uri, final String content, final String key ) {
        final ResourceEntry resourceEntry = new ResourceEntry();

        resourceEntry.setGoid(new Goid(0,oid()));
        resourceEntry.setUri( uri );
        resourceEntry.setType( uri.endsWith( ".dtd" ) ? ResourceType.DTD : ResourceType.XML_SCHEMA );
        resourceEntry.setContentType( resourceEntry.getType().getMimeType() );
        resourceEntry.setResourceKey1( key );
        resourceEntry.setContent( content );

        return resourceEntry;
    }

    private ResourceEntryHeader header( final ResourceEntry resourceEntry ) {
        return new ResourceEntryHeader( resourceEntry );
    }

    private String targetNamespace( final String content ) throws XmlUtil.BadSchemaException {
        return XmlUtil.getSchemaTNS( content );
    }

    private long oid() {
        return oid++;
    }

    private ImportAdvisor buildAdvisor( final DependencyImportChoice importChoice,
                                        final DependencyImportChoice confirmChoice,
                                        final boolean[] importConfirmed,
                                        final List<ResourceHolder> confirmedResources ) {
        return new ImportAdvisor(){
            @Override
            public DependencyImportChoice confirmCompleteImport( final Collection<ResourceHolder> resourceHolders ) {
                if ( confirmChoice==null ) fail("Should not be called, nothing to confirm");
                confirmedResources.clear();
                confirmedResources.addAll( resourceHolders );
                return confirmChoice;
            }

            @Override
            public DependencyImportChoice confirmImportDependencies() {
                if ( importChoice==null ) fail("Should not be called, nothing to confirm");
                importConfirmed[0] = true;
                return importChoice;
            }
        };
    }

    private ChoiceSelector buildChoiceSelector() {
        return new ChoiceSelector(){
            @Override
            public ImportChoice selectChoice( final ImportOption option, final String optionDetail, final ImportChoice defaultChoice, final String conflictDetail, final String resourceUri, final String resourceDescription ) {
                fail("Should not be called, no choice required");
                return defaultChoice;
            }
        };
    }

    private Functions.UnaryThrows<ResourceEntryHeader, Collection<ResourceEntryHeader>, IOException> buildEntitySelector() {
        return new  Functions.UnaryThrows<ResourceEntryHeader,Collection<ResourceEntryHeader>, IOException>(){
            @Override
            public ResourceEntryHeader call( final Collection<ResourceEntryHeader> resourceEntryHeaders ) throws IOException {
                fail("Should not be called, no resource conflicts");
                return resourceEntryHeaders.iterator().next();
            }
        };
    }

    private ResourceTherapist buildResourceTherapist() {
        return new ResourceTherapist(){
            @Override
            public ResourceDocument consult( final ResourceType resourceType, final String resourceDescription, final ResourceDocument invalidResource, final String invalidDetail ) {
                fail("Should not be called, no resource errors");
                return null;
            }
        };
    }

    private static long oid = 1;
    private static final String SCHEMA1 = "<schema xmlns=\"http://www.w3.org/2001/XMLSchema\" targetNamespace=\"urn:schema1\"/>";
    private static final String SCHEMA2 = "<schema xmlns=\"http://www.w3.org/2001/XMLSchema\" targetNamespace=\"urn:schema2\"><import schemaLocation=\"schema1.xsd\" namespace=\"urn:schema1\"/></schema>";
    private static final String SCHEMA_DTD = "<!DOCTYPE schema PUBLIC \"dtd1\" \"dtd1.dtd\"><schema xmlns=\"http://www.w3.org/2001/XMLSchema\"/>";
    private static final String SCHEMA_IMPORT_DTD = "<schema xmlns=\"http://www.w3.org/2001/XMLSchema\" targetNamespace=\"urn:schema1\"><import schemaLocation=\"schema.xsd\"/></schema>";
    private static final String SCHEMA_DTD_ABS = "<!DOCTYPE schema\nPUBLIC \"dtd1\" \"http://localhost:8888/dtds/dtd1.dtd\"\n[\n]><schema xmlns=\"http://www.w3.org/2001/XMLSchema\"/>";
    private static final String SCHEMA_INVALID = "invalid xml schema content";

    private static final String DTD1 = "<!ENTITY % partial1 PUBLIC 'partial1' 'dtd_partial1.dtd' >\n<!ENTITY % p1 'element'>\n%partial1;";
    private static final String DTD_PARTIAL1 = "<!ELEMENT %p1; (#PCDATA)>";
}
