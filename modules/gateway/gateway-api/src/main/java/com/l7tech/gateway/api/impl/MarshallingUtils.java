package com.l7tech.gateway.api.impl;

import com.l7tech.util.ExceptionUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.PropertyException;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.ValidationEvent;
import javax.xml.bind.ValidationEventHandler;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;
import java.io.IOException;
import java.util.logging.Logger;

/**
 *
 */
public class MarshallingUtils {

    //- PUBLIC

    public static void marshal( final Object mo, final Result result ) throws IOException {
        try {
            final JAXBContext context = createJAXBContext();
            final Marshaller marshaller = context.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FRAGMENT, Boolean.TRUE);

            try {
                marshaller.setProperty( Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE );
            } catch ( PropertyException e) {
                logger.info( "Unable to set marshaller for formatted output '"+ ExceptionUtils.getMessage(e)+"'." );
            }

            marshaller.marshal(mo, result );
        } catch ( JAXBException e ) {
            throw new IOException( "Error writing object '"+ExceptionUtils.getMessage(e)+"'.", e );
        }
    }

    public static <MO> MO unmarshal( final Class<MO> objectClass, final Source source ) throws IOException {
        return unmarshal( objectClass, source, true );
    }

    @SuppressWarnings( { "unchecked" } )
    public static <MO> MO unmarshal( final Class<MO> objectClass, final Source source, final boolean validate ) throws IOException {
        try {
            final JAXBContext context = createJAXBContext();
            final Unmarshaller unmarshaller = context.createUnmarshaller();

            if ( validate ) {
                unmarshaller.setSchema( ValidationUtils.getSchema() );
            }
            unmarshaller.setEventHandler( new ValidationEventHandler(){
                @Override
                public boolean handleEvent( final ValidationEvent event ) {
                    return false;
                }
            } );

            Object read = validate ?
                    unmarshaller.unmarshal( source ) :
                    unmarshaller.unmarshal( source, objectClass ).getValue(); // Could be a fragment
            if ( !objectClass.isInstance(read) ) {
                throw new IOException("Unexpected object type '"+read.getClass().getName()+"'.");
            }
            return (MO) read;
        } catch ( JAXBException e ) {
            throw new IOException( "Error writing object '"+ ExceptionUtils.getMessage(e)+"'.", e );
        }
    }

    //- PACKAGE

    static <MO> MO unmarshalFragment( final Class<MO> objectClass, final Document document ) throws IOException {
        DOMSource domSource = new DOMSource();
        if ( WSMAN_XMLFRAGMENT.equals( document.getDocumentElement().getLocalName() ) ) {
            Node element = document.getDocumentElement().getFirstChild();
            while ( element != null && element.getNodeType() != Node.ELEMENT_NODE ) {
                element = element.getNextSibling();
            }
            if ( element != null ) {
                domSource.setNode( element );
            }
        } else {
            domSource.setNode( document.getDocumentElement() );
        }
        return domSource.getNode()==null ? null : unmarshal( objectClass, domSource, false );
    }

    //- PRIVATE

    private static final Logger logger = Logger.getLogger(MarshallingUtils.class.getName());

    private static final String WSMAN_XMLFRAGMENT = "XmlFragment";

    private static JAXBContext createJAXBContext() throws JAXBException {
        return JAXBContext.newInstance("com.l7tech.gateway.api");
    }
}
