package com.l7tech.gateway.common.export;

import com.l7tech.policy.assertion.Assertion;
import com.l7tech.util.InvalidDocumentFormatException;
import org.w3c.dom.Element;

import java.io.Serializable;

/**
 * The factory can create an external reference, parse reference, and deliver a wizard step panel.
 * ER - ExternalReference or its subclass
 * F  - ExternalReferenceFinder
 *
 * @author ghuang
 */
public abstract class ExternalReferenceFactory<ER, F> implements Serializable, Comparable<ExternalReferenceFactory> {
    private Class modularAssertionClass;  // used to match this factory with a modular assertion in policy exporter
    private Class externalReferenceClass; // used to match this factory with an external reference in policy importer

    protected ExternalReferenceFactory(Class modularAssertionClass, Class externalReferenceClass) {
        this.modularAssertionClass = modularAssertionClass;
        this.externalReferenceClass = externalReferenceClass;
    }

    /**
     * Create an external reference by using finder and assertion to retrieve all reference details
     * @param finder: External Reference Finder
     * @param assertion: provide searching information to the finder to retrieve reference details
     * @return a particular ExternalReference associated with the modular assertion
     */
    public abstract ER createExternalReference(F finder, Assertion assertion);

    /**
     * Parse reference from external reference element in a policy document
     * @param finder: ExternalReferenceFinder
     * @param el: external reference element
     * @return a particular ExternalReference
     * @throws InvalidDocumentFormatException throw when the element is invalid
     */
    public abstract ER parseFromElement(final F finder, final Element el) throws InvalidDocumentFormatException;

    /**
     * Get a particular WizardStepPanel to resolve external dependency
     * @param externalReference: used to fill out the reference details in the wizard step panel
     * @return a particular WizardStepPanel for resolving external dependency
     */
    public abstract Object getResolveExternalReferenceWizardStepPanel(ER externalReference);

    /**
     * Use a modular assertion class to match this factory, whose constructor has registered modular assertion class.
     * @param assertionClass: a modular assertion class object
     * @return true if matched
     */
    public boolean matchByModularAssertion(Class assertionClass) {
        if (assertionClass == null || modularAssertionClass == null) return false;

        return assertionClass.getName().equals(modularAssertionClass.getName());
    }

    /**
     * Use an external reference class name to match this factory, whose constructor has registered external reference class name.
     * @param referenceClassName: an external reference class name
     * @return true if matched
     */
    public boolean matchByExternalReference(String referenceClassName) {
        if (referenceClassName == null || externalReferenceClass == null) return false;

        return referenceClassName.equals(externalReferenceClass.getName());
    }

    @Override
    public int compareTo(ExternalReferenceFactory o) {
        if (modularAssertionClass == null || o == null || o.modularAssertionClass == null) return 0;
        return modularAssertionClass.getName().compareToIgnoreCase(o.modularAssertionClass.getName());
    }
}