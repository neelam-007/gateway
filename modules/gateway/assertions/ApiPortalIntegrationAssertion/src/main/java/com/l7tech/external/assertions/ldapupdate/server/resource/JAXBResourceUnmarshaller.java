package com.l7tech.external.assertions.ldapupdate.server.resource;

import org.jetbrains.annotations.NotNull;

import javax.xml.bind.JAXBException;

public interface JAXBResourceUnmarshaller {
    public static final String NAMESPACE = "http://ns.l7tech.com/2013/01/ldap-manage";

    public Resource unmarshal(@NotNull String xml, @NotNull Class clazz) throws JAXBException;
}
