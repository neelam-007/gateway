package com.l7tech.console.policy.exporter;

import com.l7tech.objectmodel.encass.EncapsulatedAssertionArgumentDescriptor;
import com.l7tech.objectmodel.encass.EncapsulatedAssertionConfig;
import com.l7tech.objectmodel.encass.EncapsulatedAssertionResultDescriptor;
import com.sun.xml.bind.marshaller.NamespacePrefixMapper;
import org.jetbrains.annotations.NotNull;
import org.w3c.dom.Node;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.Result;
import java.io.IOException;

/**
 * Utility for importing and exporting an EncapsulatedAssertionConfig.
 */
public class EncapsulatedAssertionConfigExportUtil {
    /**
     * Export an EncapsulatedAssertionConfig to a Result.
     *
     * @param config the EncapsulatedAssertionConfig to export.
     * @param result the Result which will hold the exported EncapsulatedAssertionConfig xml.
     * @throws IOException if unable to export the EncapsulatedAssertionConfig.
     */
    public void export(@NotNull EncapsulatedAssertionConfig config, @NotNull final Result result) throws IOException {
        final Marshaller marshaller;
        try {
            marshaller = createMarshaller();
            marshaller.marshal(config, result);
        } catch (final JAXBException e) {
            throw new IOException(e);
        }
    }

    /**
     * Imports an EncapsulatedAssertionConfig from a Node.
     *
     * @param node the Node which contains the EncapsulatedAssertionConfig xml.
     * @return the imported EncapsulatedAssertionConfig.
     * @throws IOException if unable to import an EncapsulatedAssertionConfig.
     */
    public EncapsulatedAssertionConfig importFromNode(@NotNull Node node) throws IOException {
        try {
            final Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
            final Object unmarshalled = unmarshaller.unmarshal(node);
            if (!(unmarshalled instanceof EncapsulatedAssertionConfig)) {
                throw new IOException("Expected EncapsulatedAssertionConfig but received " + unmarshalled.getClass());
            }
            final EncapsulatedAssertionConfig config = (EncapsulatedAssertionConfig) unmarshalled;
            for (final EncapsulatedAssertionArgumentDescriptor arg : config.getArgumentDescriptors()) {
                arg.setEncapsulatedAssertionConfig(config);
            }
            for (final EncapsulatedAssertionResultDescriptor result : config.getResultDescriptors()) {
                result.setEncapsulatedAssertionConfig(config);
            }
            return config;
        } catch (final JAXBException e) {
            throw new IOException(e);
        }
    }

    public static EncapsulatedAssertionConfigExportUtil getInstance() throws IOException {
        if (instance == null) {
            instance = new EncapsulatedAssertionConfigExportUtil();
        }
        return instance;
    }

    EncapsulatedAssertionConfigExportUtil() throws IOException {
        try {
            jaxbContext = JAXBContext.newInstance("com.l7tech.objectmodel.encass");
        } catch (final JAXBException e) {
            throw new IOException(e);
        }
    }

    private final JAXBContext jaxbContext;
    private static EncapsulatedAssertionConfigExportUtil instance;
    private static final String JAXB_FORMATTED_OUTPUT = "jaxb.formatted.output";
    private static final String JAXB_FRAGMENT = "jaxb.fragment";
    private static final String COM_SUN_XML_BIND_NAMESPACE_PREFIX_MAPPER = "com.sun.xml.bind.namespacePrefixMapper";

    private Marshaller createMarshaller() throws JAXBException {
        final Marshaller marshaller = jaxbContext.createMarshaller();
        marshaller.setProperty(JAXB_FORMATTED_OUTPUT, true);
        marshaller.setProperty(JAXB_FRAGMENT, true);
        marshaller.setProperty(COM_SUN_XML_BIND_NAMESPACE_PREFIX_MAPPER, new EncapsulatedAssertionConfigNamespacePrefixMapper());
        return marshaller;
    }

    private class EncapsulatedAssertionConfigNamespacePrefixMapper extends NamespacePrefixMapper {

        private static final String L7_NS = "http://ns.l7tech.com/secureSpan/1.0/core";
        private static final String XSI_NS = "http://www.w3.org/2001/XMLSchema-instance";
        private static final String XS_NS = "http://www.w3.org/2001/XMLSchema";
        private static final String ENCASS_NS = "http://ns.l7tech.com/secureSpan/1.0/encass";
        private static final String L7 = "L7";
        private static final String XSI = "xsi";
        private static final String XS = "xs";
        private static final String ENC = "enc";

        @Override
        public String getPreferredPrefix(final String namespaceUri, final String suggestion, boolean requirePrefix) {
            String prefix = null;
            if (L7_NS.equals(namespaceUri)) {
                prefix = L7;
            } else if (XSI_NS.equals(namespaceUri)) {
                prefix = XSI;
            } else if (XS_NS.equals(namespaceUri)) {
                prefix = XS;
            } else if (ENCASS_NS.endsWith(namespaceUri)) {
                prefix = ENC;
            }
            return prefix;
        }
    }
}
