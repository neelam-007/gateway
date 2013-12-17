package com.l7tech.gateway.api.impl;

import com.l7tech.gateway.api.ManagedObject;
import com.l7tech.util.ConfigFactory;
import com.l7tech.util.ExceptionUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.PropertyException;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.ValidationEvent;
import javax.xml.bind.ValidationEventHandler;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.namespace.QName;
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

    @SuppressWarnings({ "unchecked" })
    public static void marshal( final Object mo, final Result result, final boolean isFragment ) throws IOException {
        try {
            final JAXBContext context = getJAXBContext();
            final Marshaller marshaller = context.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FRAGMENT, isFragment);

            try {
                marshaller.setProperty( Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE );
            } catch ( PropertyException e) {
                logger.info( "Unable to set marshaller for formatted output '"+ ExceptionUtils.getMessage(e)+"'." );
            }

            Object data = mo;
            if ( !(mo instanceof ManagedObject) && mo.getClass().getAnnotation(XmlRootElement.class) == null ) {
                XmlType type = mo.getClass().getAnnotation(XmlType.class);
                if ( type == null ) {
                    throw new IOException( "Cannot marshal object without XmlType '" + mo.getClass() + "'" );
                }
                data = new JAXBElement( new QName(type.namespace(), asElementName(type.name())), mo.getClass(), mo );
            }
            
            marshaller.marshal(data, result );
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
            final JAXBContext context = getJAXBContext();
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

    public static JAXBContext getJAXBContext() throws JAXBException {
        JAXBContext context = MarshallingUtils.context;

        if ( context == null ) {
            context = JAXBContext.newInstance("com.l7tech.gateway.api:com.l7tech.gateway.api.impl");

            if ( USE_STATIC_CONTEXT ) {
                MarshallingUtils.context = context;
            }
        }

        return context;
    }

    //- PRIVATE

    private static final Logger logger = Logger.getLogger(MarshallingUtils.class.getName());

    private static final String WSMAN_XMLFRAGMENT = "XmlFragment";

    private static final boolean USE_STATIC_CONTEXT;
    static {
        boolean staticContext = false;
        try {
            staticContext = Boolean.valueOf( ConfigFactory.getProperty( "com.l7tech.gateway.api.useStaticJAXBContext", "true" ) );
        } catch ( SecurityException se ) {
            // use safe default (non static)            
        }
        USE_STATIC_CONTEXT = staticContext;
    }

    private static JAXBContext context;

    private static String asElementName( final String typeName ) {
        String elementName = typeName;

        if ( elementName.endsWith( "Type" )) {
            elementName = elementName.substring( 0, elementName.length() - 4 );
        }

        return elementName;
    }
}
