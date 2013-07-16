package com.l7tech.external.assertions.apiportalintegration.server;

import com.l7tech.external.assertions.apiportalintegration.server.resource.JAXBResourceMarshaller;
import com.l7tech.gateway.api.PolicyValidationResult;
import com.sun.xml.bind.marshaller.NamespacePrefixMapper;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import java.io.ByteArrayOutputStream;

/**
 * JAXB Marshaller that can marshall Policy Validation.
 */
public class PolicyValidationMarshaller {

    public String marshal(PolicyValidationResult resource) throws JAXBException {
        if (resource == null) {
            return null;
        }
        final ByteArrayOutputStream stream = new ByteArrayOutputStream();
        final Marshaller marshaller = jaxbContext.createMarshaller();
        marshaller.setProperty("jaxb.formatted.output", true);
        marshaller.setProperty("jaxb.fragment", true);
        marshaller.setProperty("com.sun.xml.bind.namespacePrefixMapper", new NamespacePrefixMapper() {
            @Override
            public String getPreferredPrefix(final String namespaceUri, final String suggestion, boolean requirePrefix) {
                String prefix = null;
                if (JAXBResourceMarshaller.NAMESPACE.equals(namespaceUri)) {
                    prefix = JAXBResourceMarshaller.PREFIX;
                } else if ("http://www.w3.org/2001/XMLSchema-instance".equals(namespaceUri)) {
                    prefix = "xsi";
                }
                return prefix;
            }
        });
        marshaller.marshal(resource, stream);
        return stream.toString();
    }

    public static PolicyValidationMarshaller getInstance() throws JAXBException {
        if (instance == null) {
            instance = new PolicyValidationMarshaller();
        }
        return instance;
    }

    PolicyValidationMarshaller() throws JAXBException {
        jaxbContext = JAXBContext.newInstance(PolicyValidationResult.class);
    }

    final JAXBContext jaxbContext;
    private static PolicyValidationMarshaller instance;
}
