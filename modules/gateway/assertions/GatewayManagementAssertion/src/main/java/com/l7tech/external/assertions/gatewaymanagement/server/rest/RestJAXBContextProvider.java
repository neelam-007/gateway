package com.l7tech.external.assertions.gatewaymanagement.server.rest;

import com.l7tech.gateway.api.ManagedObjectFactory;

import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.Provider;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;

/**
 * This is needed so that jersey can properly marshal and unmarshall all objects.
 */
@Provider
public class RestJAXBContextProvider implements ContextResolver<JAXBContext> {
    public JAXBContext getContext(Class<?> type) {
        try {
            return ManagedObjectFactory.getJAXBContext();
        } catch (JAXBException e) {
            throw new RuntimeException(e);
        }
    }
}
