package com.l7tech.external.assertions.apiportalintegration.server.resource;

import org.jetbrains.annotations.NotNull;

import javax.xml.bind.JAXBException;

public interface JAXBResourceUnmarshaller {
    public Resource unmarshal(@NotNull String xml, @NotNull Class clazz) throws JAXBException;
}
