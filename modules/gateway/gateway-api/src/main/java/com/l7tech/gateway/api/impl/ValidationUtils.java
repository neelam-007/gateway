package com.l7tech.gateway.api.impl;

import com.l7tech.gateway.api.ManagementRuntimeException;
import com.l7tech.util.CollectionUtils;
import com.l7tech.util.IOUtils;
import com.l7tech.util.ResourceUtils;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.SchemaOutputResolver;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 *
 */
public class ValidationUtils {

    //- PUBLIC

    public static Source[] getSchemaSources() {
        final List<Source> schemaResources = new ArrayList<Source>();

        Map<String,String> schemaSources = ValidationUtils.schemaSources;
        if ( schemaSources == null ) {
            try {
                final Map<String,String> sources = schemaSources = new LinkedHashMap<String,String>();
                final JAXBContext context = MarshallingUtils.getJAXBContext();
                context.generateSchema( new SchemaOutputResolver(){
                    @Override
                    public Result createOutput( final String namespaceUri, final String suggestedFileName ) throws IOException {
                        return new StreamResult( suggestedFileName ){
                            {
                                setWriter( new StringWriter(){
                                    @Override
                                    public void close() throws IOException {
                                        super.close();
                                        sources.put( suggestedFileName, transform(toString()) );
                                    }
                                } );
                            }
                        };
                    }
                } );
            } catch ( IOException e ) {
                throw new ManagementRuntimeException("Error generating XML Schema", e);
            } catch ( JAXBException e ) {
                throw new ManagementRuntimeException("Error generating XML Schema", e);
            }

            ValidationUtils.schemaSources = Collections.unmodifiableMap( schemaSources );
        }

        for ( final Map.Entry<String,String> entry : schemaSources.entrySet() ) {
            schemaResources.add( new StreamSource( new StringReader(entry.getValue()), entry.getKey() ) );
        }

        return schemaResources.toArray( new Source[schemaResources.size()] );
    }

    public static Schema getSchema() {
        Schema schema = ValidationUtils.schema;

        if ( schema == null ) {
            SchemaFactory factory = SchemaFactory.newInstance( XMLConstants.W3C_XML_SCHEMA_NS_URI );
            try {
                schema = ValidationUtils.schema = factory.newSchema( getSchemaSources() );
            } catch ( SAXException e ) {
                throw new ManagementRuntimeException("Error creating schema instance", e);
            }
        }

        return schema;
    }

    /**
     * Get a keyword map suitable for use with the X500Principal class.
     *
     * @return The map of OIDs to keywords.
     */
    public static Map<String,String> getOidKeywordMap() {
        return OID_KEYWORD_MAP;
    }

    /**
     * Utility to write the XML Schema to file.
     *
     * @param args The first argument it the target directory.
     */
    public static void main(final String[] args) throws IOException {
        if ( args.length < 1 ) throw new IllegalArgumentException("Output directory not specified");

        final File outputDirectory = new File(args[0]);
        if ( !outputDirectory.isDirectory() ) {
            throw new IllegalArgumentException("Invalid output directory: " + outputDirectory.getAbsolutePath());
        }

        // Write XML Schema(s)
        final Source[] schemaSources = getSchemaSources();
        for ( final Source source : schemaSources ) {
            final String name = "schema1.xsd".equals( source.getSystemId() ) ? "gateway-management.xsd" : source.getSystemId();
            final File schemaFile = new File( outputDirectory, name );

            OutputStreamWriter out = null;
            try {
                out = new OutputStreamWriter(new FileOutputStream( schemaFile ));
                IOUtils.copyStream( ((StreamSource) source).getReader(), out );
            } finally {
                ResourceUtils.closeQuietly( out );
            }
        }
    }

    //- PRIVATE

    private static Schema schema;
    private static Map<String,String> schemaSources;

    private static final Map<String,String> OID_KEYWORD_MAP = CollectionUtils.<String,String>mapBuilder()
        .put( "1.2.840.113549.1.9.1", "EMAILADDRESS" )
        .put( "2.5.4.4", "SURNAME" )
        .put( "2.5.4.5", "SERIALNUMBER" )
        .put( "2.5.4.12", "T" )
        .put( "2.5.4.42", "GIVENNAME" )
        .put( "2.5.4.43", "INITIALS" )
        .put( "2.5.4.44", "GENERATION" )
        .put( "2.5.4.46", "DNQ" )
        .map();

    private static String transform( final String schema ) {
        InputStream schemaIn = null;
        try {
            final TransformerFactory transformerFactory = TransformerFactory.newInstance();
            final Transformer transformer = transformerFactory.newTransformer(
                    new StreamSource( schemaIn = ValidationUtils.class.getResourceAsStream( "schema-transform.xsl" ) ) );
            final StringWriter result = new StringWriter();
            transformer.transform( new StreamSource(new StringReader(schema)), new StreamResult(result) );
            return result.toString();
        } catch ( TransformerConfigurationException e ) {
            throw new ManagementRuntimeException("Unable to generate XML Schema", e);
        } catch ( TransformerException e ) {
            throw new ManagementRuntimeException("Unable to generate XML Schema", e);
        } finally {
            ResourceUtils.closeQuietly( schemaIn );
        }
    }
}
