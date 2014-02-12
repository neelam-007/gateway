package com.l7tech.external.assertions.gatewaymanagement.server.rest;

import com.l7tech.gateway.api.ManagedObjectFactory;
import com.l7tech.gateway.api.impl.ValidationUtils;
import com.l7tech.util.ExceptionUtils;

import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.Provider;
import javax.xml.bind.*;
import java.util.logging.Logger;

/**
 * This is needed so that jersey can properly marshal and unmarshall all objects.
 */
@SuppressWarnings("UnusedDeclaration")
@Provider
public class RestJAXBContextProvider implements ContextResolver<JAXBContext> {

    private static final Logger logger = Logger.getLogger(RestJAXBContextProvider.class.getName());

    public JAXBContext getContext(Class<?> type) {
        try {
            return new StrictJAXBContext();
        } catch (JAXBException e) {
            throw new RuntimeException(e);
        }
    }

    public static class StrictJAXBContext extends JAXBContext {
        private final JAXBContext contextDelegate;

        public StrictJAXBContext() throws JAXBException {
            contextDelegate = ManagedObjectFactory.getJAXBContext();
        }

        @Override
        public javax.xml.bind.Unmarshaller createUnmarshaller() throws JAXBException {
            javax.xml.bind.Unmarshaller unmarshaller = contextDelegate.createUnmarshaller();

            // Set the Schema
            unmarshaller.setSchema(ValidationUtils.getSchema());
            unmarshaller.setEventHandler( new ValidationEventHandler(){
                @Override
                public boolean handleEvent( final ValidationEvent event ) {
                    return false;
                }
            } );
            return unmarshaller;
        }

        @Override
        public javax.xml.bind.Marshaller createMarshaller() throws JAXBException {
            final Marshaller marshaller = contextDelegate.createMarshaller();

            try {
                marshaller.setProperty( Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE );
            } catch ( PropertyException e) {
                logger.info( "Unable to set marshaller for formatted output '"+ ExceptionUtils.getMessage(e)+"'." );
            }
            return marshaller;
        }

        @SuppressWarnings("deprecation")
        @Override
        public Validator createValidator() throws JAXBException {
            return contextDelegate.createValidator();
        }

    }
}
