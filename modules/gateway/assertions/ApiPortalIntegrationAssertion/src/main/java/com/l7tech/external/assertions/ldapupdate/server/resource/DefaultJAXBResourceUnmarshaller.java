package com.l7tech.external.assertions.ldapupdate.server.resource;

import org.apache.commons.lang.Validate;
import org.jetbrains.annotations.NotNull;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.ByteArrayInputStream;

public class DefaultJAXBResourceUnmarshaller implements JAXBResourceUnmarshaller {
    public static JAXBResourceUnmarshaller getInstance() throws JAXBException {
        if (instance == null) {
            instance = new DefaultJAXBResourceUnmarshaller();
        }
        return instance;
    }

    public Resource unmarshal(@NotNull String xml, @NotNull Class clazz) throws JAXBException {
        Validate.isTrue(Resource.class.isAssignableFrom(clazz), "Class must be a type of Resource");
        final Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
        final Object unmarshalled = unmarshaller.unmarshal(new ByteArrayInputStream(xml.getBytes()));
        if (!clazz.isAssignableFrom(unmarshalled.getClass())) {
            throw new JAXBException("Expected " + clazz + " but found " + unmarshalled.getClass());
        }
        return (Resource) unmarshalled;
    }

    DefaultJAXBResourceUnmarshaller() throws JAXBException {
        this.jaxbContext = JAXBContext.newInstance(LDAPOperation.class, LDAPOperations.class);
    }

    final JAXBContext jaxbContext;
    private static DefaultJAXBResourceUnmarshaller instance;
}
