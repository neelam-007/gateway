package com.l7tech.policy.exporter;

import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.composite.CompositeAssertion;
import com.l7tech.policy.wsp.InvalidPolicyStreamException;
import com.l7tech.policy.wsp.WspReader;
import org.w3c.dom.Element;

import java.util.Iterator;
import java.util.List;

/**
 * This class takes a set of remote references that were exported with a policy
 * and find corresponding match with local entities. When the resolution cannot
 * be made automatically, it prompts the administrator for manual resolution.
 *
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Jul 22, 2004<br/>
 * $Id$
 */
public class RemoteReferenceResolver {

    /**
     * Resolve remote references involving the administrator's input when necessary.
     * This method must be invoked before localizePolicy()
     *
     * @param references references parsed from a policy document.
     * @return false if the process cannot continue because the administrator canceled an operation for example.
     */
    public boolean resolveReferences(ExternalReference[] references) {
        for (int i = 0; i < references.length; i++) {
            ExternalReference reference = references[i];
            if (!reference.verifyReference()) {
                // todo, get the administrator involved somehow
            }
        }
        resolvedReferences = references;
        return true;
    }

    public Assertion localizePolicy(Element policyXML) throws InvalidPolicyStreamException {
        // Go through each assertion and fix the changed references.
        Assertion root = WspReader.parse(policyXML);
        traverseAssertionTreeForLocalization(root);
        return root;
    }

    private void traverseAssertionTreeForLocalization(Assertion rootAssertion) {
        if (rootAssertion instanceof CompositeAssertion) {
            CompositeAssertion ca = (CompositeAssertion)rootAssertion;
            List children = ca.getChildren();
            for (Iterator i = children.iterator(); i.hasNext();) {
                Assertion child = (Assertion)i.next();
                traverseAssertionTreeForLocalization(child);
            }
        } else {
            for (int i = 0; i < resolvedReferences.length; i++) {
                resolvedReferences[i].localizeAssertion(rootAssertion);
            }
        }
    }

    private ExternalReference[] resolvedReferences = null;
}
