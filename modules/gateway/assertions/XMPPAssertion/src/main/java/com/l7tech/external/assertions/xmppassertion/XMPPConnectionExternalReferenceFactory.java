package com.l7tech.external.assertions.xmppassertion;

import com.l7tech.external.assertions.xmppassertion.console.ResolveForeignXMPPConnectionPanel;
import com.l7tech.gateway.common.export.ExternalReferenceFactory;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.exporter.ExternalReferenceFinder;
import com.l7tech.util.InvalidDocumentFormatException;
import org.w3c.dom.Element;

/**
 * User: njordan
 * Date: 20/03/12
 * Time: 10:35 AM
 */
public class XMPPConnectionExternalReferenceFactory extends ExternalReferenceFactory {
    @SuppressWarnings({"UnusedDeclaration"})
    public XMPPConnectionExternalReferenceFactory(Class modularAssertionClass, Class externalReferenceClass) {
        super(modularAssertionClass, externalReferenceClass);
    }

    public XMPPConnectionExternalReferenceFactory() {
        super(XMPPOpenServerSessionAssertion.class, XMPPConnectionReference.class);
    }

    @Override
    public Object createExternalReference(Object finder, Assertion assertion) {
        if (!(assertion instanceof XMPPOpenServerSessionAssertion)) {
            throw new IllegalArgumentException("The assertion is not an assertion that references an XMPP connection.");
        }

        return new XMPPConnectionReference((ExternalReferenceFinder) finder, (XMPPOpenServerSessionAssertion)assertion);
    }

    @Override
    public Object parseFromElement(Object finder, Element el) throws InvalidDocumentFormatException {
        return XMPPConnectionReference.parseFromElement(finder, el);
    }

    @Override
    public Object getResolveExternalReferenceWizardStepPanel(Object externalReference) {
        if (! (externalReference instanceof XMPPConnectionReference)) return null;

        return new ResolveForeignXMPPConnectionPanel(null, (XMPPConnectionReference)externalReference);
    }
}
