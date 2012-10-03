package com.l7tech.external.assertions.apiportalintegration.server.resource;

import org.jetbrains.annotations.NotNull;

import javax.xml.bind.JAXBException;

/**
 * Marshaller for obtaining XML representations of portal resources.
 */
public interface JAXBResourceMarshaller {
    public static final String NAMESPACE = "http://ns.l7tech.com/2012/04/api-management";
    public static final String PREFIX = "l7";
    /**
     * Get an XML representation of the given resource.
     *
     *
     * @param resource the resource for which to get an XML representation.
     * @return an XML representation of the given resource.
     * @throws JAXBException if a marshalling error occurs.
     */
    public <T extends Resource> String marshal(@NotNull final T resource) throws JAXBException;
}
