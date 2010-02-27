package com.l7tech.gateway.api.impl;

import com.l7tech.gateway.api.ManagementRuntimeException;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.SchemaOutputResolver;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import java.io.IOException;
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
                                        sources.put( suggestedFileName, toString() );
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

    //- PRIVATE

    private static Schema schema;
    private static Map<String,String> schemaSources;
}
