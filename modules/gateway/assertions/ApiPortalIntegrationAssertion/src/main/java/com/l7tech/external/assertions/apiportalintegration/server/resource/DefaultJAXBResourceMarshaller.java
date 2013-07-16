package com.l7tech.external.assertions.apiportalintegration.server.resource;

import com.sun.xml.bind.marshaller.NamespacePrefixMapper;
import org.jetbrains.annotations.NotNull;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import java.io.ByteArrayOutputStream;

/**
 * Default implementation of JAXBResourceMarshaller that can marshall portal resources.
 */
public class DefaultJAXBResourceMarshaller implements JAXBResourceMarshaller {
    @Override
    public <T extends Resource> String marshal(@NotNull T resource) throws JAXBException {
        final ByteArrayOutputStream stream = new ByteArrayOutputStream();
        final Marshaller marshaller = jaxbContext.createMarshaller();
        marshaller.setProperty("jaxb.formatted.output", true);
        marshaller.setProperty("jaxb.fragment", true);
        marshaller.setProperty("com.sun.xml.bind.namespacePrefixMapper", new NamespacePrefixMapper() {
            @Override
            public String getPreferredPrefix(final String namespaceUri, final String suggestion, boolean requirePrefix) {
                String prefix = null;
                if (NAMESPACE.equals(namespaceUri)) {
                    prefix = PREFIX;
                } else if ("http://www.w3.org/2001/XMLSchema-instance".equals(namespaceUri)) {
                    prefix = "xsi";
                }
                return prefix;
            }
        });
        marshaller.marshal(resource, stream);
        return stream.toString();
    }

    public static JAXBResourceMarshaller getInstance() throws JAXBException {
        if (instance == null) {
            instance = new DefaultJAXBResourceMarshaller();
        }
        return instance;
    }

    DefaultJAXBResourceMarshaller() throws JAXBException {
        jaxbContext = JAXBContext.newInstance(ApiListResource.class, ApiPlanListResource.class, ApiKeyResource.class,
                GatewayResource.class, ApiKeyListResource.class, AccountPlanListResource.class);
    }

    final JAXBContext jaxbContext;
    private static DefaultJAXBResourceMarshaller instance;
}
