package com.l7tech.external.assertions.websocket;

import com.l7tech.gateway.common.export.ExternalReferenceFactory;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.exporter.ExternalReferenceFinder;
import com.l7tech.util.InvalidDocumentFormatException;
import org.w3c.dom.Element;

/**
 * Created with IntelliJ IDEA.
 * User: cirving
 * Date: 8/28/12
 * Time: 3:05 PM
 * To change this template use File | Settings | File Templates.
 */
public class WebSocketConnectionExternalReferenceFactory extends ExternalReferenceFactory {

    @SuppressWarnings({"UnusedDeclaration"})
    public WebSocketConnectionExternalReferenceFactory(Class modularAssertionClass, Class externalReferenceClass) {
        super(modularAssertionClass, externalReferenceClass);
    }

    public WebSocketConnectionExternalReferenceFactory() {
        super(WebSocketMessageInjectionAssertion.class, WebSocketConnectionReference.class);
    }

    @Override
    public Object createExternalReference(Object finder, Assertion assertion) {
        if (!(assertion instanceof WebSocketMessageInjectionAssertion)) {
            throw new IllegalArgumentException("The assertion is not a WebSocket Message Injection Assertion.");
        }

        return new WebSocketConnectionReference((ExternalReferenceFinder) finder, (WebSocketMessageInjectionAssertion)assertion);
    }

    @Override
    public Object parseFromElement(Object finder, Element el) throws InvalidDocumentFormatException {
        return WebSocketConnectionReference.parseFromElement(finder, el);
    }

    @Override
    public Object getResolveExternalReferenceWizardStepPanel(Object externalReference) {
        //No panel needed.
        return null;
    }
}
