package com.l7tech.skunkworks.tarari;

import com.tarari.xml.rax.schema.SchemaLoader;
import com.tarari.xml.rax.schema.SchemaResolver;

import java.io.ByteArrayInputStream;
import java.nio.charset.Charset;

/**
 * Test for crash importing schema by namespace.
 */
public class TarariBug8892Test {

    private static final Charset charset = Charset.forName( "UTF-8" );

    public static void main(String[] args) throws Exception {
        SchemaLoader.unloadAllSchemas();

        SchemaLoader.setSchemaResolver(new SchemaResolver() {
            @Override
            public byte[] resolveSchema(String namespaceUri, String locationHint, String baseUri) {
                System.out.println("Resolving schema for namespaceUri="+namespaceUri+", locationHint="+locationHint+", baseUri="+baseUri);
                if ( "http://example.com/namespaces/example_import".equals(namespaceUri) ||
                     "http://example.com/example_import.xsd".equals(locationHint) ) {
                    return
                         "<xs:schema targetNamespace=\"http://example.com/namespaces/example_import\" elementFormDefault=\"qualified\" version=\"1.0\" xmlns:xs=\"http://www.w3.org/2001/XMLSchema\"/>"
                        .getBytes( charset );
                }

                return new byte[0];
            }
        });

        // Import by schemaLocation and namespace
        System.out.println( "Importing by schemaLocation and namespace" );
        SchemaLoader.unloadAllSchemas();
        SchemaLoader.loadSchema(
                new ByteArrayInputStream(
                        ("<xs:schema targetNamespace=\"http://example.com/namespaces/example\" elementFormDefault=\"qualified\" version=\"1.0\" xmlns:xs=\"http://www.w3.org/2001/XMLSchema\">" +
                         "<xs:import schemaLocation=\"http://example.com/example_import.xsd\" namespace=\"http://example.com/namespaces/example_import\"/>" +
                         "</xs:schema>")
                        .getBytes( charset )
                ),
                "http://example.com/example.xsd");
        System.out.println( "Schema loaded successfully" );

        // Import by namespace
        System.out.println( "Importing by namespace" );
        SchemaLoader.unloadAllSchemas();
        SchemaLoader.loadSchema(
                new ByteArrayInputStream(
                        ("<xs:schema targetNamespace=\"http://example.com/namespaces/example\" elementFormDefault=\"qualified\" version=\"1.0\" xmlns:xs=\"http://www.w3.org/2001/XMLSchema\">" +
                         "<xs:import namespace=\"http://example.com/namespaces/example_import\"/>" +
                         "</xs:schema>")
                        .getBytes( charset )
                ),
                "http://example.com/example.xsd");
        System.out.println( "Schema loaded successfully" );
    }

}
