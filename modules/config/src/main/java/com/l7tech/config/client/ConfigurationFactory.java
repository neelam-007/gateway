package com.l7tech.config.client;

import com.l7tech.config.client.options.OptionSet;
import com.l7tech.util.ResourceUtils;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.JAXBException;
import javax.xml.bind.SchemaOutputResolver;
import javax.xml.transform.Result;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Schema;
import javax.xml.XMLConstants;
import java.net.URL;
import java.io.InputStream;
import java.io.IOException;

import org.xml.sax.SAXException;

/**
 * Factory for construction of configurations.
 *
 * 
 */
public class ConfigurationFactory {

    //- PUBLIC

    public static OptionSet newConfiguration( final Class context, final String resource ) throws ConfigurationException {
        URL resourceUrl = context.getResource( resource );

        if ( resourceUrl == null ) {
            throw new ConfigurationException( "Invalid configuration resource '"+resource+"'." );
        }

        return loadConfiguration( resourceUrl );
    }

    public static OptionSet newConfiguration( final String resource ) throws ConfigurationException {
        return newConfiguration( ConfigurationFactory.class, resource );
    }

    //- PRIVATE

    private static OptionSet loadConfiguration( final URL configurationUrl ) throws ConfigurationException {
        OptionSet optionSet;

        InputStream in = null;
        try {
            in = configurationUrl.openStream();
            JAXBContext context = JAXBContext.newInstance("com.l7tech.config.client.options");
            final DOMResult result = new DOMResult();
            context.generateSchema( new SchemaOutputResolver(){
                public Result createOutput(String namespaceUri, String suggestedFileName) throws IOException {
                    result.setSystemId( suggestedFileName );
                    return result;
                }
            } );

            Unmarshaller unmarshaller = context.createUnmarshaller();
            try {
                Schema schema = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI).newSchema(new DOMSource(result.getNode(), result.getSystemId()));
                unmarshaller.setSchema(schema);
            } catch ( SAXException se ) {
                throw new ConfigurationException("Error generating option set schema.", se);            
            }

            OptionSet target = (OptionSet) unmarshaller.unmarshal( in );
            if ( target.getParentName() != null ) {
                OptionSet parentOptions = loadConfiguration( new URL(configurationUrl, target.getParentName()) );

                // Note that due to set semantics the parent options do not override the childs
                target.getOptionGroups().addAll( parentOptions.getOptionGroups() );
                target.getOptions().addAll( parentOptions.getOptions() );
            }
            optionSet = target;
        } catch ( JAXBException je ) {
            throw new ConfigurationException( "Error loading configuration set from '"+configurationUrl+"'.", je );        
        } catch ( IOException ie ) {
            throw new ConfigurationException( "IO error loading configuration set from '"+configurationUrl+"'.", ie );
        } finally {
            ResourceUtils.closeQuietly( in );
        }

        return optionSet;
    }
}
