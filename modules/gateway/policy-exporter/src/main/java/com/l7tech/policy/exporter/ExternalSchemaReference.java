package com.l7tech.policy.exporter;

import com.l7tech.gateway.common.resources.ResourceType;
import com.l7tech.util.InvalidDocumentFormatException;
import org.w3c.dom.Element;
import org.xml.sax.EntityResolver;

/**
 * A reference to an imported schema from a schema validation assertion
 * <p/>
 * This class is largely redundant since the introduction of the resource
 * entry reference but is kept for reading / writing backwards compatible
 * references for XML Schemas.
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Oct 19, 2005<br/>
 */
public class ExternalSchemaReference extends GlobalResourceReference {

    public ExternalSchemaReference( final ExternalReferenceFinder finder,
                                    final EntityResolver entityResolver,
                                    final String name,
                                    final String tns ) {
        super( finder, entityResolver, name, ResourceType.XML_SCHEMA, tns, null, null );
    }

    public static ExternalSchemaReference parseFromElement( final ExternalReferenceFinder finder,
                                                            final EntityResolver entityResolver,
                                                            final Element el ) throws InvalidDocumentFormatException {
        if (!el.getNodeName().equals(TOPEL_NAME)) {
            throw new InvalidDocumentFormatException("Expecting element of name " + TOPEL_NAME);
        }
        String name = null;
        if (el.hasAttribute(LOC_ATTR_NAME)) {
            name = el.getAttribute(LOC_ATTR_NAME);
        }
        String tns = null;
        if (el.hasAttribute(TNS_ATTR_NAME)) {
            tns = el.getAttribute(TNS_ATTR_NAME);
        }
        return new ExternalSchemaReference(finder, entityResolver, name, tns);
    }

    public String getName() {
        return getSystemIdentifier();
    }

    public String getTns() {
        return getResourceKey1();
    }

    @Override
    public void serializeToRefElement(Element referencesParentElement) {
        Element refEl = referencesParentElement.getOwnerDocument().createElement(TOPEL_NAME);
        setTypeAttribute( refEl );
        if (getName() != null) {
            refEl.setAttributeNS(null, LOC_ATTR_NAME, getName());
        }
        if (getTns() != null) {
            refEl.setAttributeNS(null, TNS_ATTR_NAME, getTns());
        }
        referencesParentElement.appendChild(refEl);
    }

    private static final String TOPEL_NAME = "ExternalSchema";
    private static final String LOC_ATTR_NAME = "schemaLocation";
    private static final String TNS_ATTR_NAME = "targetNamespace";
}