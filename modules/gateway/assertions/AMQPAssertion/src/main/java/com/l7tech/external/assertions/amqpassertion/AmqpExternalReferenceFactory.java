package com.l7tech.external.assertions.amqpassertion;

import com.l7tech.external.assertions.amqpassertion.console.ResolveForeignAmqpPanel;
import com.l7tech.gateway.common.export.ExternalReferenceFactory;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.exporter.ExternalReference;
import com.l7tech.policy.exporter.ExternalReferenceFinder;
import com.l7tech.util.InvalidDocumentFormatException;
import org.w3c.dom.Element;

/**
 * Created by IntelliJ IDEA.
 * User: ashah
 * Date: 13/03/12
 * Time: 10:28 AM
 * To change this template use File | Settings | File Templates.
 */
public class AmqpExternalReferenceFactory extends ExternalReferenceFactory<ExternalReference, ExternalReferenceFinder> {

    public AmqpExternalReferenceFactory() {
        super(RouteViaAMQPAssertion.class, AmqpExternalReference.class);
    }

    @Override
    public ExternalReference createExternalReference(ExternalReferenceFinder finder, Assertion assertion) {
        if (!(assertion instanceof RouteViaAMQPAssertion)) {
            throw new IllegalArgumentException("The assertion is not a AMQP Assertion.");
        }

        return new AmqpExternalReference(
                finder,                                                  // Finder
                ((RouteViaAMQPAssertion) assertion).getSsgActiveConnectorGoid()  // GOID
        );
    }

    @Override
    public ExternalReference parseFromElement(ExternalReferenceFinder finder, Element el) throws InvalidDocumentFormatException {
        return AmqpExternalReference.parseFromElement(finder, el);
    }

    @Override
    public Object getResolveExternalReferenceWizardStepPanel(ExternalReference externalReference) {
        if (!(externalReference instanceof AmqpExternalReference)) return null;

        return new ResolveForeignAmqpPanel(null, (AmqpExternalReference) externalReference);
    }
}
