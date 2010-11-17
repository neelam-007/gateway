package com.l7tech.console.panels;

import static org.junit.Assert.*;
import org.junit.Test;

import java.net.URI;
import java.net.URISyntaxException;

/**
 *
 */
public class GlobalResourceImportContextTest {

    @Test
    public void testRelativizeUri() throws Exception {
        testRelativizeUri( "simple relativize", "http://host/path/to/file.txt", "http://host/path/to/file2.txt", "file2.txt" );
        testRelativizeUri( "query relativize", "http://host/path/to/file.txt?q1&b=c", "http://host/path/to/file2.txt?q2&a=b", "file2.txt?q2&a=b" );
        testRelativizeUri( "fragment relativize", "http://host/path/to/file.txt#f1", "http://host/path/to/file2.txt#f2", "file2.txt#f2" );
        testRelativizeUri( "sub-directory relativize", "http://host/path/file.txt", "http://host/path/to/file2.txt", "to/file2.txt" );
        testRelativizeUri( "root sub-directory relativize", "http://host/file.txt", "http://host/path/to/file2.txt", "path/to/file2.txt" );
        testRelativizeUri( "skinny sub-directory relativize", "http://host/1/file.txt", "http://host/1/2/3/file2.txt", "2/3/file2.txt" );
        testRelativizeUri( "root skinny sub-directory relativize", "http://host/file.txt", "http://host/1/2/3/file2.txt", "1/2/3/file2.txt" );
        testRelativizeUri( "super relativize", "http://host/path/to/file.txt", "http://host/path/file2.txt", "../file2.txt" );
        testRelativizeUri( "super sub relativize", "http://host/path/to/file.txt", "http://host/path/to2/file2.txt", "../to2/file2.txt" );
        testRelativizeUri( "skinny super relativize", "http://host/1/2/file.txt", "http://host/1/file2.txt", "../file2.txt" );
        testRelativizeUri( "skinny sub relativize", "http://host/1/2/3/4/file.txt", "http://host/1/5/6/7/file2.txt", "../../../5/6/7/file2.txt" );
        testRelativizeUri( "file relativize", "file:/1/2/3/4/file.txt", "file:/1/2/3/4/file2.txt", "file2.txt" );
        testRelativizeUri( "https relativize", "https://host/1/2/3/4/file.txt", "https://host/1/2/3/4/file2.txt", "file2.txt" );
        testRelativizeUri( "distinct relativize", "http://host/1/2/3/4/file.txt", "https://host/1/2/3/4/file2.txt", "https://host/1/2/3/4/file2.txt" );
        testRelativizeUri( "distinct relativize 2", "file:/home/steve/www/warehouse_schema_include_parent.xsd", "http://localhost:8888/warehouse_schema_include_child.xsd", "http://localhost:8888/warehouse_schema_include_child.xsd");
        testRelativizeUri( "bug 9437", "http://localhost:8888/path/to/schema/schema_dtd_abs.xsd", "http://localhost:8888/dtds/dtd1.dtd", "../../../dtds/dtd1.dtd" );
    }

    private void testRelativizeUri( final String description,
                                    final String baseUri,
                                    final String uri,
                                    final String expectedResult ) throws URISyntaxException {
        String result = GlobalResourceImportContext.relativizeUri( new URI(baseUri), new URI(uri) ).toString();
        assertEquals( description, expectedResult, result );
    }
}
