package com.l7tech.common.io;

import com.l7tech.util.IOUtils;
import com.l7tech.util.Pair;
import static org.junit.Assert.*;
import org.junit.Test;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 *
 */
public class DtdUtilsTest {

    @Test
    public void testProcessReferencesSimple() throws IOException, SAXException {
        final Set<String> resolved = new HashSet<String>();
        DtdUtils.processReferences( "http://host/test.dtd", "<!ELEMENT element ANY>\n", new DtdUtils.Resolver(){
            @Override
            public Pair<String, String> call( final String publicId, final String baseUri, final String systemId ) throws IOException {
                System.out.println( "Resolving : " + systemId );
                resolved.add( systemId );
                return null;
            }
        } );
        assertEquals("Dependency count", 0, resolved.size());
    }

    @Test
    public void testProcessReferencesXMLSchemaDTD() throws IOException, SAXException {
        final Set<String> resolved = new HashSet<String>();
        DtdUtils.processReferences(
                "http://www.w3.org/2001/XMLSchema.dtd",
                new String( IOUtils.slurpStream( DtdUtils.class.getResourceAsStream( "/com/l7tech/common/resources/XMLSchema.dtd" ) ) ),
                new DtdUtils.Resolver(){
            @Override
            public Pair<String, String> call( final String publicId, final String baseUri, final String systemId ) throws IOException {
                System.out.println( "Resolving : " + systemId );

                try {
                    final String absoluteUri = new URI(baseUri).resolve( systemId ).toString();                
                    resolved.add( absoluteUri );

                    if ( "http://www.w3.org/2001/datatypes.dtd".equals( absoluteUri )) {
                        return new Pair<String,String>( absoluteUri, new String( IOUtils.slurpStream( DtdUtils.class.getResourceAsStream( "/com/l7tech/common/resources/datatypes.dtd" ))  ) );
                    }
                } catch ( URISyntaxException e ) {
                    throw new IOException(e);
                }
                return null;
            }
        }  );
        assertEquals("Dependency count", 1, resolved.size());
        assertEquals("Dependencies", Collections.singleton( "http://www.w3.org/2001/datatypes.dtd" ), resolved);
    }

    /**
     * Test all kinds of potential reference.
     */
    @Test
    public void testProcessReferencesFull() throws IOException, SAXException {
        // Test parameter entity
        validateReferenceProcessed( "<!ENTITY % test SYSTEM 'test.dtd' >\n", true );
        validateReferenceProcessed( "<!ENTITY % test PUBLIC 'test' 'test.dtd' >\n", true );

        // Test parsed entity
        validateReferenceProcessed( "<!ENTITY test SYSTEM 'test.dtd' >\n", true );
        validateReferenceProcessed( "<!ENTITY test PUBLIC 'test' 'test.dtd' >\n", true );
        
        // Test unparsed entity
        validateReferenceProcessed( "<!ENTITY test SYSTEM 'test.dtd' NDATA dtd >\n", false );
        validateReferenceProcessed( "<!ENTITY test PUBLIC 'test' 'test.dtd' NDATA dtd >\n", false );

        // Test notation
        validateReferenceProcessed( "<!NOTATION test SYSTEM 'test.dtd'>\n", false );
        validateReferenceProcessed( "<!NOTATION test PUBLIC 'test' 'test.dtd'>\n", false );
    }

    private void validateReferenceProcessed( final String dtdDocument,
                                             final boolean processed ) throws IOException, SAXException {
        final Set<String> resolved = new HashSet<String>();
        DtdUtils.processReferences( "http://host/test_document.dtd", dtdDocument, new DtdUtils.Resolver(){
            @Override
            public Pair<String, String> call( final String publicId, final String baseUri, final String systemId ) throws IOException {
                resolved.add( systemId );

                if ( "http://host/test.dtd".equals( systemId )) {
                    return new Pair<String,String>( systemId, "" );
                }
                return null;
            }
        } );

        if ( processed ) {
            assertEquals("Dependency count", 1, resolved.size());
            assertEquals("Dependencies", Collections.singleton( "http://host/test.dtd" ), resolved);
        } else {
            assertEquals("Dependency count", 0, resolved.size());
        }
    }

    @Test
    public void testPublicIdentifierNormalization() {
        assertEquals( "Leading space removed", "test", DtdUtils.normalizePublicId( " test" ) );
        assertEquals( "Trailing space removed", "test", DtdUtils.normalizePublicId( "test " ) );
        assertEquals( "Leading and trailing space removed", "test", DtdUtils.normalizePublicId( " test " ) );
        assertEquals( "Newlines normalized", "te st", DtdUtils.normalizePublicId( "te\n\rst" ) );
        assertEquals( "Tab normalized", "te st", DtdUtils.normalizePublicId( "te\t st" ) );
    }

    @Test
    public void testUpdateExternalSystemIdentifier() {
        final String[][] testData = {
                { "No external identifier", "<!DOCTYPE test [<!ELEMENT test (#PCDATA)>]><test/>", "unused.dtd", null, null },
                { "Invalid replacement system id", "<!DOCTYPE test SYSTEM 'test.dtd'><test/>", "test\"'2.dtd", null, null },
                { "Simple system id", "<!DOCTYPE test SYSTEM 'test.dtd'><test/>", "test2.dtd", " 'test2.dtd'", " 'test.dtd'" },
                { "Simple public id", "<!DOCTYPE test PUBLIC '-//LAYER 7/DTD TEST/EN' 'test.dtd'><test/>", "test2.dtd", " 'test2.dtd'", " 'test.dtd'" },
                { "Doctype with preceding space", "          <!DOCTYPE test SYSTEM 'test.dtd' [<!ELEMENT test (#PCDATA)>]><test/>", "test2.dtd", " 'test2.dtd' ", " 'test.dtd' " },
                { "Doctype with preceding comment", "<!-- <!DOCTYPE      --><!DOCTYPE test SYSTEM 'test.dtd' [<!ELEMENT test (#PCDATA)>]><test/>", "test2.dtd", " 'test2.dtd' ", " 'test.dtd' " },
                { "Doctype with preceding lines", "\n\n\r\n<!DOCTYPE test SYSTEM 'test.dtd' [<!ELEMENT test (#PCDATA)>]><test/>", "test2.dtd", " 'test2.dtd' ", " 'test.dtd' " },
                { "Doc with xml declaration", "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n\n\r\n<!DOCTYPE test SYSTEM 'test.dtd' [<!ELEMENT test (#PCDATA)>]><test/>", "test2.dtd", " 'test2.dtd' ", " 'test.dtd' " },
                { "Complex system id", "<!DOCTYPE\t\r\ntest\t\r \nSYSTEM\t \r \n\"test.dtd\" [<!ELEMENT test (#PCDATA)>]><test/>", "test2.dtd", "\n\"test2.dtd\" ", "\n\"test.dtd\" " },
                { "Complex public id", "<!DOCTYPE\rtest\nPUBLIC\t\"-//LAYER 7/DTD TEST/EN\"\t\"test.dtd\" \n[\n<!ELEMENT test (#PCDATA)\n>\n]>\n<test/>", "test2.dtd", "\t\"test2.dtd\" ", "\t\"test.dtd\" " },
                { "System id in comment", "<!--'test.dtd' --><!DOCTYPE test SYSTEM 'test.dtd'><test/>", "test2.dtd", " 'test2.dtd'", " 'test.dtd'" },
                { "Absolute system id", "<!DOCTYPE test SYSTEM 'http://localhost:8888/test.dtd'><test/>", "http://localhost:8888/test2.dtd", " 'http://localhost:8888/test2.dtd'", " 'http://localhost:8888/test.dtd'" },
                { "Absolute public id", "<!DOCTYPE test PUBLIC '-//LAYER 7/DTD TEST/EN' 'http://localhost:8888/test.dtd'><test/>", "http://localhost:8888/test2.dtd", " 'http://localhost:8888/test2.dtd'", " 'http://localhost:8888/test.dtd'" },
        };

        for ( final String[] test : testData ) {
            final String testId = test[0];
            final String document = test[1];
            final String systemId = test[2];
            final String contains = test[3];
            final String matchReplace = test[4];

            final String updatedDoc = DtdUtils.updateExternalSystemId( document, systemId );

            if ( contains == null ) {
                assertEquals( testId + " not updated", document, updatedDoc );
            } else {
                assertTrue( testId + " contains " + contains, updatedDoc.contains(contains) );
            }

            if ( contains!=null && matchReplace != null ) {
                assertEquals( testId + " matches", document.replace( matchReplace, contains ), updatedDoc );
            }
        }
    }

}
