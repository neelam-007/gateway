package com.l7tech.external.assertions.gatewaymanagement.server;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.gateway.api.EncapsulatedAssertionExportResult;
import com.l7tech.gateway.api.ManagedObjectFactory;
import com.l7tech.gateway.api.Resource;
import com.l7tech.gateway.api.impl.EncassImportContext;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.encass.EncapsulatedAssertionConfig;
import com.l7tech.policy.exporter.EncapsulatedAssertionExportUtil;
import com.l7tech.util.ExceptionUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.EntityResolver;
import org.xml.sax.SAXException;

import java.io.IOException;

/**
 * Helper class for implementing encass import/export.
 *
 * @author Victor Kazakov
 */
public class EncapsulatedAssertionHelper {
    private EntityResolver entityResolver;
    private PolicyHelper.GatewayExternalReferenceFinder referenceFinder;

    public EncapsulatedAssertionHelper(final PolicyHelper.GatewayExternalReferenceFinder referenceFinder,
                                       final EntityResolver entityResolver) throws IOException {
        this.referenceFinder = referenceFinder;
        this.entityResolver = entityResolver;
    }

    /**
     * Export the given encass.
     *
     * @param encass The encass to export.
     * @return The policy export result.
     */
    public EncapsulatedAssertionExportResult exportEncass(final EncapsulatedAssertionConfig encass) throws FindException {
        try {
            final Document exportDoc = EncapsulatedAssertionExportUtil.exportEncass(encass, referenceFinder, entityResolver);
            final EncapsulatedAssertionExportResult encassExportResult = ManagedObjectFactory.createEncapsulatedAssertionExportResult();
            final Resource resource = ManagedObjectFactory.createResource();
            resource.setType(ResourceHelper.ENCASS_EXPORT_TYPE);
            resource.setContent(XmlUtil.nodeToFormattedString(exportDoc));
            encassExportResult.setResource(resource);

            return encassExportResult;
        } catch (SAXException | IOException e) {
            throw new ResourceFactory.ResourceAccessException("Error creating encass export '" + ExceptionUtils.getMessage(e) + "'.", e);
        }
    }

    public static EncapsulatedAssertionConfig importFromNode(final EncassImportContext resource, final boolean resetOidsAndVersions) {
        try {
            final Document exportDoc = XmlUtil.parse(resource.getResource().getContent());
            final Element encassElement = XmlUtil.findFirstChildElementByName(exportDoc.getDocumentElement(), "http://ns.l7tech.com/secureSpan/1.0/encass", "EncapsulatedAssertion");
            return EncapsulatedAssertionExportUtil.importFromNode(encassElement, resetOidsAndVersions);
        } catch (SAXException | IOException e) {
            throw new ResourceFactory.ResourceAccessException("Error parsing encass import '" + ExceptionUtils.getMessage(e) + "'.", e);
        }
    }
}
