package com.l7tech.console.policy.exporter;

import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.encass.EncapsulatedAssertionConfig;
import com.l7tech.policy.exporter.EncapsulatedAssertionExportUtil;
import org.apache.commons.lang.Validate;
import org.jetbrains.annotations.NotNull;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

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
    public static Document exportConfigAndPolicy(@NotNull final EncapsulatedAssertionConfig config) throws IOException, SAXException, FindException {
        Validate.notNull(config.getPolicy(), "EncapsulatedAssertionConfig is missing its backing policy.");

        final ConsoleExternalReferenceFinder finder = new ConsoleExternalReferenceFinder();
        return EncapsulatedAssertionExportUtil.exportEncass(config, finder, finder);
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
        return EncapsulatedAssertionExportUtil.importFromNode(node, resetOidsAndVersions);
    }
}
