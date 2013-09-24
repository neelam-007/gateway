package com.l7tech.policy.exporter;

import com.l7tech.common.io.UncheckedIOException;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.encass.EncapsulatedAssertionArgumentDescriptor;
import com.l7tech.objectmodel.encass.EncapsulatedAssertionConfig;
import com.l7tech.objectmodel.encass.EncapsulatedAssertionResultDescriptor;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.util.HexUtils;
import org.apache.commons.lang.Validate;
import org.jetbrains.annotations.NotNull;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.EntityResolver;
import org.xml.sax.SAXException;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.dom.DOMResult;
import java.io.IOException;

/**
 * Utility for importing and exporting an EncapsulatedAssertionConfig.
 *
 * @author Victor Kazakov
 */
public class EncapsulatedAssertionExportUtil {
    public static final String ENCASS_NS = "http://ns.l7tech.com/secureSpan/1.0/encass";
    public static final String ENCAPSULATED_ASSERTION = "EncapsulatedAssertion";

    /**
     * Exports an EncapsulatedAssertionConfig and its backing Policy to a Document.
     * <p/>
     * If the EncapsulatedAssertionConfig has an EncapsulatedAssertionConfig.PROP_ARTIFACT_VERSION it could be replaced
     * with a new value.
     *
     * @param config the EncapsulatedAssertionConfig to export which contains a backing Policy.
     * @return a Document representing the exported EncapsulatedAssertionConfig and its backing Policy.
     * @throws java.io.IOException
     * @throws org.xml.sax.SAXException
     */
    public static Document exportEncass(@NotNull final EncapsulatedAssertionConfig config,
                                        final ExternalReferenceFinder referenceFinder,
                                        final EntityResolver entityResolver) throws IOException, SAXException, FindException {
        Validate.notNull(config.getPolicy(), "EncapsulatedAssertionConfig is missing its backing policy.");

        // build backing policy xml
        final Assertion assertion = config.getPolicy().getAssertion();
        final PolicyExporter exporter = new PolicyExporter(referenceFinder, entityResolver);
        final Document doc = exporter.exportToDocument(assertion, referenceFinder.findAllExternalReferenceFactories());

        // append encass xml without artifact version
        final DocumentFragment withoutArtifactVersion = doc.createDocumentFragment();
        config.removeProperty(EncapsulatedAssertionConfig.PROP_ARTIFACT_VERSION);
        exportConfig(config, new DOMResult(withoutArtifactVersion));
        doc.getDocumentElement().appendChild(withoutArtifactVersion);

        // generate artifact version from backing policy (and its references) and encass config xml
        final String artifactVersion = generateArtifactVersion(doc);

        // rebuild xml with artifact version
        config.putProperty(EncapsulatedAssertionConfig.PROP_ARTIFACT_VERSION, artifactVersion);
        final DocumentFragment withArtifactVersion = doc.createDocumentFragment();
        exportConfig(config, new DOMResult(withArtifactVersion));
        XmlUtil.removeChildElementsByName(doc.getDocumentElement(), ENCASS_NS, "EncapsulatedAssertion");
        doc.getDocumentElement().appendChild(withArtifactVersion);
        return doc;
    }

    /**
     * Imports an EncapsulatedAssertionConfig with a simplified backing Policy from a Node.
     * <p/>
     * The EncapsulatedAssertionConfig id and version will be default values regardless of the id and version found in
     * the Node.
     *
     * @param node the Node which contains the EncapsulatedAssertionConfig xml.
     * @return the imported EncapsulatedAssertionConfig.
     * @throws IOException if unable to import an EncapsulatedAssertionConfig.
     */
    public static EncapsulatedAssertionConfig importFromNode(@NotNull final Node node) throws IOException {
        return importFromNode(node, false);
    }

    /**
     * Imports an EncapsulatedAssertionConfig with a simplified backing Policy from a Node.
     *
     * @param node                 the Node which contains the EncapsulatedAssertionConfig xml.
     * @param resetOidsAndVersions true if the oids and versions of the entities should be reset to default values.
     * @return the imported EncapsulatedAssertionConfig.
     * @throws IOException if unable to import an EncapsulatedAssertionConfig.
     */
    public static EncapsulatedAssertionConfig importFromNode(@NotNull final Node node, final boolean resetOidsAndVersions) throws IOException {
        try {
            final Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
            final Object unmarshalled = unmarshaller.unmarshal(node);
            if (!(unmarshalled instanceof EncapsulatedAssertionConfig)) {
                throw new IOException("Expected EncapsulatedAssertionConfig but received " + unmarshalled.getClass());
            }
            final EncapsulatedAssertionConfig config = (EncapsulatedAssertionConfig) unmarshalled;
            for (final EncapsulatedAssertionArgumentDescriptor arg : config.getArgumentDescriptors()) {
                arg.setEncapsulatedAssertionConfig(config);
                if (resetOidsAndVersions) {
                    arg.setGoid(EncapsulatedAssertionArgumentDescriptor.DEFAULT_GOID);
                    arg.setVersion(0);
                }
            }
            for (final EncapsulatedAssertionResultDescriptor result : config.getResultDescriptors()) {
                result.setEncapsulatedAssertionConfig(config);
                if (resetOidsAndVersions) {
                    result.setGoid(EncapsulatedAssertionResultDescriptor.DEFAULT_GOID);
                    result.setVersion(0);
                }
            }
            if (resetOidsAndVersions) {
                config.setGoid(EncapsulatedAssertionConfig.DEFAULT_GOID);
                config.setVersion(0);
            }
            return config;
        } catch (final JAXBException e) {
            throw new IOException(e);
        }
    }

    /**
     * Generates an artifact version from an xml Document which represents an EncapsulatedAssertionConfig and backing
     * Policy.
     *
     * @param exportDocument the Document which contains the EncapsulatedAssertionConfig and its backing Policy xml.
     * @return an artifact version for the exportDocument.
     * @throws IOException
     */
    static String generateArtifactVersion(@NotNull final Document exportDocument) throws IOException {
        final String xml = XmlUtil.nodeToFormattedString(exportDocument.getDocumentElement());
        final byte[] sha1Digest = HexUtils.getSha1Digest(xml.getBytes());
        return HexUtils.hexDump(sha1Digest);
    }

    /**
     * Export an EncapsulatedAssertionConfig with a simplified backing Policy to a Result.
     * <p/>
     * Also removes id and version of the EncapsulatedAssertionConfig.
     *
     * @param config the EncapsulatedAssertionConfig to export.
     * @param result the Result which will hold the exported EncapsulatedAssertionConfig xml.
     * @throws IOException if unable to export the EncapsulatedAssertionConfig.
     */
    static void exportConfig(@NotNull final EncapsulatedAssertionConfig config, @NotNull final DOMResult result) throws IOException {
        final Marshaller marshaller;
        try {
            marshaller = createMarshaller();
            marshaller.marshal(config, result);
            // manually remove id and version
            // this is because we cannot override jaxb annotations for id and version on the child class without modifying
            // the parent class (by adding @XmlTransient annotation to the parent at a class level) which is dangerous
            // as PersistentEntityImp is heavily used.
            final Element encassElement = XmlUtil.findFirstChildElementByName(result.getNode(), ENCASS_NS, ENCAPSULATED_ASSERTION);
            if (encassElement != null) {
                encassElement.removeAttribute("id");
                encassElement.removeAttribute("version");
            }
        } catch (final JAXBException e) {
            throw new IOException(e);
        }
    }

    private static JAXBContext createJAXBContext() {
        try {
            return JAXBContext.newInstance("com.l7tech.objectmodel.encass");
        } catch (final JAXBException e) {
            throw new UncheckedIOException(new IOException(e));
        }
    }

    private static final JAXBContext jaxbContext = createJAXBContext();
    private static final String JAXB_FORMATTED_OUTPUT = "jaxb.formatted.output";
    private static final String JAXB_FRAGMENT = "jaxb.fragment";

    private static Marshaller createMarshaller() throws JAXBException {
        final Marshaller marshaller = jaxbContext.createMarshaller();
        marshaller.setProperty(JAXB_FORMATTED_OUTPUT, true);
        marshaller.setProperty(JAXB_FRAGMENT, true);
        return marshaller;
    }
}
