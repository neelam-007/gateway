package com.l7tech.external.assertions.remotecacheassertion;

import com.l7tech.external.assertions.remotecacheassertion.console.ResolveForeignRemoteCachePanel;
import com.l7tech.gateway.common.export.ExternalReferenceFactory;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.exporter.ExternalReferenceFinder;
import com.l7tech.util.InvalidDocumentFormatException;
import org.w3c.dom.Element;

import java.util.logging.Logger;

/**
 * Created by IntelliJ IDEA.
 * User: njordan
 * Date: 20/03/12
 * Time: 10:35 AM
 * To change this template use File | Settings | File Templates.
 */
public class RemoteCacheExternalReferenceFactory extends ExternalReferenceFactory {
    private static final Logger logger = Logger.getLogger(RemoteCacheExternalReferenceFactory.class.getName());

    public RemoteCacheExternalReferenceFactory(Class modularAssertionClass, Class externalReferenceClass) {
        super(modularAssertionClass, externalReferenceClass);
    }

    public RemoteCacheExternalReferenceFactory() {
        super(RemoteCacheAssertion.class, RemoteCacheReference.class);
    }

    @Override
    public Object createExternalReference(Object finder, Assertion assertion) {
        if (!(assertion instanceof RemoteCacheAssertion)) {
            throw new IllegalArgumentException("The assertion is not an assertion that references an RemoteCache connection.");
        }
        return new RemoteCacheReference((ExternalReferenceFinder) finder, (RemoteCacheAssertion) assertion);
    }

    @Override
    public Object parseFromElement(Object finder, Element el) throws InvalidDocumentFormatException {
        return RemoteCacheReference.parseFromElement(finder, el);
    }

    @Override
    public Object getResolveExternalReferenceWizardStepPanel(Object externalReference) {
        if (!(externalReference instanceof RemoteCacheReference)) {
            return null;
        }

        return new ResolveForeignRemoteCachePanel(null, (RemoteCacheReference) externalReference);
    }
}
