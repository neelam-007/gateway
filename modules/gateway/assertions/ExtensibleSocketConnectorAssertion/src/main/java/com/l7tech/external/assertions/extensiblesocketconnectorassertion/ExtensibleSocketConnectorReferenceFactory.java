package com.l7tech.external.assertions.extensiblesocketconnectorassertion;

import com.l7tech.external.assertions.extensiblesocketconnectorassertion.console.ResolveForeignExtensibleSocketConnectorPanel;
import com.l7tech.gateway.common.export.ExternalReferenceFactory;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.exporter.ExternalReferenceFinder;
import com.l7tech.util.InvalidDocumentFormatException;
import org.w3c.dom.Element;

/**
 * Created by IntelliJ IDEA.
 * User: njordan
 * Date: 28/03/12
 * Time: 11:49 AM
 * To change this template use File | Settings | File Templates.
 */
public class ExtensibleSocketConnectorReferenceFactory extends ExternalReferenceFactory {
    @SuppressWarnings({"UnusedDeclaration"})
    public ExtensibleSocketConnectorReferenceFactory(Class modularAssertionClass, Class externalReferenceClass) {
        super(modularAssertionClass, externalReferenceClass);
    }

    public ExtensibleSocketConnectorReferenceFactory() {
        super(ExtensibleSocketConnectorAssertion.class, ExtensibleSocketConnectorReference.class);
    }

    @Override
    public Object createExternalReference(Object finder, Assertion assertion) {
        if (!(assertion instanceof ExtensibleSocketConnectorAssertion)) {
            throw new IllegalArgumentException("The assertion is not an assertion that references an extensible socket connector.");
        }

        return new ExtensibleSocketConnectorReference((ExternalReferenceFinder) finder, (ExtensibleSocketConnectorAssertion) assertion);
    }

    @Override
    public Object parseFromElement(Object finder, Element el) throws InvalidDocumentFormatException {
        return ExtensibleSocketConnectorReference.parseFromElement(finder, el);
    }

    @Override
    public Object getResolveExternalReferenceWizardStepPanel(Object externalReference) {
        if (!(externalReference instanceof ExtensibleSocketConnectorReference)) return null;

        return new ResolveForeignExtensibleSocketConnectorPanel(null, (ExtensibleSocketConnectorReference) externalReference);
    }
}
