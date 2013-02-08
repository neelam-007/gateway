package com.l7tech.console.policy.exporter;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.objectmodel.encass.EncapsulatedAssertionArgumentDescriptor;
import com.l7tech.objectmodel.encass.EncapsulatedAssertionConfig;
import com.l7tech.objectmodel.encass.EncapsulatedAssertionResultDescriptor;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.exporter.PolicyExporter;
import com.l7tech.util.HexUtils;
import org.apache.commons.lang.Validate;
import org.jetbrains.annotations.NotNull;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.dom.DOMResult;
import java.io.IOException;

/**
 * Utility for importing and exporting an EncapsulatedAssertionConfig.
 */
public class EncapsulatedAssertionConfigExportUtil {
    /**
     * Exports an EncapsulatedAssertionConfig and its backing Policy to a Document.
     * <p/>
     * If the EncapsulatedAssertionConfig has an EncapsulatedAssertionConfig.PROP_ARTIFACT_VERSION it could be replaced with a new value.
     *
     * @param config the EncapsulatedAssertionConfig to export which contains a backing Policy.
     * @return a Document representing the exported EncapsulatedAssertionConfig and its backing Policy.
     * @throws IOException
     * @throws SAXException
     */
    public Document exportConfigAndPolicy(@NotNull final EncapsulatedAssertionConfig config) throws IOException, SAXException {
        Validate.notNull(config.getPolicy(), "EncapsulatedAssertionConfig is missing its backing policy.");

        // build backing policy xml
        final Assertion assertion = config.getPolicy().getAssertion();
        final ConsoleExternalReferenceFinder finder = new ConsoleExternalReferenceFinder();
        final PolicyExporter exporter = new PolicyExporter(finder, finder);
        final Document doc = exporter.exportToDocument(assertion, PolicyExportUtils.getExternalReferenceFactories());

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
     * The EncapsulatedAssertionConfig id and version will be default values regardless of the id and version found in the Node.
     *
     * @param node the Node which contains the EncapsulatedAssertionConfig xml.
     * @return the imported EncapsulatedAssertionConfig.
     * @throws IOException if unable to import an EncapsulatedAssertionConfig.
     */
    public EncapsulatedAssertionConfig importFromNode(@NotNull final Node node) throws IOException {
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
    public EncapsulatedAssertionConfig importFromNode(@NotNull final Node node, final boolean resetOidsAndVersions) throws IOException {
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
                    arg.setOid(EncapsulatedAssertionArgumentDescriptor.DEFAULT_OID);
                    arg.setVersion(0);
                }
            }
            for (final EncapsulatedAssertionResultDescriptor result : config.getResultDescriptors()) {
                result.setEncapsulatedAssertionConfig(config);
                if (resetOidsAndVersions) {
                    result.setOid(EncapsulatedAssertionResultDescriptor.DEFAULT_OID);
                    result.setVersion(0);
                }
            }
            if (resetOidsAndVersions) {
                config.setOid(EncapsulatedAssertionConfig.DEFAULT_OID);
                config.setVersion(0);
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

    /**
     * Generates an artifact version from an xml Document which represents an EncapsulatedAssertionConfig and backing Policy.
     *
     * @param exportDocument the Document which contains the EncapsulatedAssertionConfig and its backing Policy xml.
     * @return an artifact version for the exportDocument.
     * @throws IOException
     */
    String generateArtifactVersion(@NotNull final Document exportDocument) throws IOException {
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
    void exportConfig(@NotNull final EncapsulatedAssertionConfig config, @NotNull final DOMResult result) throws IOException {
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
    private static final String ENCASS_NS = "http://ns.l7tech.com/secureSpan/1.0/encass";
    private static final String ENCAPSULATED_ASSERTION = "EncapsulatedAssertion";

    private Marshaller createMarshaller() throws JAXBException {
        final Marshaller marshaller = jaxbContext.createMarshaller();
        marshaller.setProperty(JAXB_FORMATTED_OUTPUT, true);
        marshaller.setProperty(JAXB_FRAGMENT, true);
        return marshaller;
    }
}
