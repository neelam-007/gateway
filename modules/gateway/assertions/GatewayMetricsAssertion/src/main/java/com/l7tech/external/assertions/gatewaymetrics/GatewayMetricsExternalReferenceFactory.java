package com.l7tech.external.assertions.gatewaymetrics;

import com.l7tech.external.assertions.gatewaymetrics.console.ResolveForeignGatewayMetricsPanel;
import com.l7tech.gateway.common.export.ExternalReferenceFactory;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.exporter.ExternalReference;
import com.l7tech.policy.exporter.ExternalReferenceFinder;
import com.l7tech.util.InvalidDocumentFormatException;
import org.w3c.dom.Element;

/**
 * Created with IntelliJ IDEA.
 * User: kpak
 * Date: 3/18/13
 * Time: 5:14 PM
 * To change this template use File | Settings | File Templates.
 */
public class GatewayMetricsExternalReferenceFactory extends ExternalReferenceFactory<ExternalReference, ExternalReferenceFinder> {

    public GatewayMetricsExternalReferenceFactory() {
        super(GatewayMetricsAssertion.class, GatewayMetricsExternalReference.class);
    }

    @Override
    public ExternalReference createExternalReference(ExternalReferenceFinder finder, Assertion assertion) {
        if (!(assertion instanceof GatewayMetricsAssertion)) {
            throw new IllegalArgumentException("The assertion is not a Gateway Metrics Assertion.");
        }

        return new GatewayMetricsExternalReference(finder, (GatewayMetricsAssertion) assertion);
    }

    @Override
    public ExternalReference parseFromElement(ExternalReferenceFinder finder, Element el) throws InvalidDocumentFormatException {
        return GatewayMetricsExternalReference.parseFromElement(finder, el);
    }

    @Override
    public Object getResolveExternalReferenceWizardStepPanel(ExternalReference externalReference) {
        if (!(externalReference instanceof GatewayMetricsExternalReference)) {
            throw null;
        }

        return new ResolveForeignGatewayMetricsPanel(null, (GatewayMetricsExternalReference) externalReference);
    }
}