package com.l7tech.external.assertions.mqnative;

import com.l7tech.external.assertions.mqnative.console.ResolveForeignMqNativePanel;
import com.l7tech.gateway.common.export.ExternalReferenceFactory;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.exporter.ExternalReference;
import com.l7tech.policy.exporter.ExternalReferenceFinder;
import com.l7tech.util.InvalidDocumentFormatException;
import org.w3c.dom.Element;

/**
 * @author ghuang
 */
public class MqNativeExternalReferenceFactory extends ExternalReferenceFactory<ExternalReference, ExternalReferenceFinder> {

    public MqNativeExternalReferenceFactory() {
        super(MqNativeRoutingAssertion.class, MqNativeExternalReference.class);
    }

    public ExternalReference createExternalReference(ExternalReferenceFinder finder, Assertion assertion) {
        if (! (assertion instanceof MqNativeRoutingAssertion)) {
            throw new IllegalArgumentException("The assertion is not a MQ Native Routing Assertion.");
        }
        MqNativeRoutingAssertion mqAss = ((MqNativeRoutingAssertion) assertion);

        return new MqNativeExternalReference(
            finder,                                                          // Finder
            mqAss.getSsgActiveConnectorId()
        );
    }

    @Override
    public ExternalReference parseFromElement(ExternalReferenceFinder finder, Element el) throws InvalidDocumentFormatException {
        return MqNativeExternalReference.parseFromElement(finder, el);
    }

    @Override
    public Object getResolveExternalReferenceWizardStepPanel(ExternalReference externalReference) {
        if (! (externalReference instanceof MqNativeExternalReference)) return null;

        return new ResolveForeignMqNativePanel(null, (MqNativeExternalReference)externalReference);
    }
}
