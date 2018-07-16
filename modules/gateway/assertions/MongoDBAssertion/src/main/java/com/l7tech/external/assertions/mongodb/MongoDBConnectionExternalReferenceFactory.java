package com.l7tech.external.assertions.mongodb;

import com.l7tech.external.assertions.mongodb.console.ResolveMongoDBConnectionPanel;
import com.l7tech.gateway.common.export.ExternalReferenceFactory;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.exporter.ExternalReferenceFinder;
import com.l7tech.util.InvalidDocumentFormatException;
import org.w3c.dom.Element;

import java.util.logging.Logger;

/**
 * Created by chaja24 on 9/2/2015.
 */
public class MongoDBConnectionExternalReferenceFactory extends ExternalReferenceFactory {
    private static final Logger logger = Logger.getLogger(MongoDBConnectionExternalReferenceFactory.class.getName());

    public MongoDBConnectionExternalReferenceFactory(Class modularAssertionClass, Class externalReferenceClass) {
        super(modularAssertionClass, externalReferenceClass);
    }

    public MongoDBConnectionExternalReferenceFactory() {
        super(MongoDBAssertion.class, MongoDBReference.class);
    }

    @Override
    public Object createExternalReference(Object finder, Assertion assertion) {
        if (!(assertion instanceof MongoDBAssertion)) {
            throw new IllegalArgumentException("The assertion is not an assertion that references an MongoDB connection.");
        }
        return new MongoDBReference((ExternalReferenceFinder) finder, (MongoDBAssertion) assertion);
    }

    @Override
    public Object parseFromElement(Object finder, Element el) throws InvalidDocumentFormatException {
        return MongoDBReference.parseFromElement(finder, el);
    }

    @Override
    public Object getResolveExternalReferenceWizardStepPanel(Object externalReference) {
        if (!(externalReference instanceof MongoDBReference)) {
            return null;
        }

        return new ResolveMongoDBConnectionPanel(null, (MongoDBReference) externalReference);

    }

}
