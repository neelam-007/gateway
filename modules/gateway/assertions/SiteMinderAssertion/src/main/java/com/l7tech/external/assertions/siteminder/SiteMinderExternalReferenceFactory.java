package com.l7tech.external.assertions.siteminder;

import com.l7tech.external.assertions.siteminder.console.ResolveForeignSiteMinderPanel;
import com.l7tech.gateway.common.export.ExternalReferenceFactory;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.exporter.ExternalReference;
import com.l7tech.policy.exporter.ExternalReferenceFinder;
import com.l7tech.util.InvalidDocumentFormatException;
import org.w3c.dom.Element;


public class SiteMinderExternalReferenceFactory extends ExternalReferenceFactory<ExternalReference, ExternalReferenceFinder> {

    public SiteMinderExternalReferenceFactory() {
        super(SiteMinderCheckProtectedAssertion.class, SiteMinderExternalReference.class);
    }

    public SiteMinderExternalReferenceFactory(Class modularAssertionClass, Class externalReferenceClass) {
        super(modularAssertionClass, externalReferenceClass);
    }

    @Override
    public ExternalReference createExternalReference(ExternalReferenceFinder finder, Assertion assertion) {
        if (!(assertion instanceof SiteMinderCheckProtectedAssertion)) {
            throw new IllegalArgumentException("The assertion is not a SiteMinder Check Protected Assertion.");
        }
        return new SiteMinderExternalReference(finder, ((SiteMinderCheckProtectedAssertion) assertion).getAgentID());
    }

    @Override
    public ExternalReference parseFromElement(ExternalReferenceFinder finder, Element el) throws InvalidDocumentFormatException {
        return SiteMinderExternalReference.parseFromElement(finder, el);
    }

    @Override
    public Object getResolveExternalReferenceWizardStepPanel(ExternalReference externalReference) {
        if (! (externalReference instanceof SiteMinderExternalReference)) return null;

        return new ResolveForeignSiteMinderPanel(null, (SiteMinderExternalReference)externalReference);
    }
}
