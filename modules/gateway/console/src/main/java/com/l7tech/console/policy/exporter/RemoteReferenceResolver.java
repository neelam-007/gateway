package com.l7tech.console.policy.exporter;

import com.l7tech.gui.util.Utilities;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.console.panels.ResolveExternalPolicyReferencesWizard;
import com.l7tech.console.util.TopComponents;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.composite.CompositeAssertion;
import com.l7tech.policy.wsp.InvalidPolicyStreamException;
import com.l7tech.policy.wsp.WspReader;
import org.w3c.dom.Element;

import java.io.IOException;
import java.util.*;
import java.util.List;
import java.awt.*;

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
 */
public class RemoteReferenceResolver {

    private WspReader wspReader = null;

    private WspReader getWspReader() {
        if (wspReader == null) {
            wspReader = (WspReader)TopComponents.getInstance().getApplicationContext().getBean("wspReader", WspReader.class);
        }
        return wspReader;
    }

    /**
     * Resolve remote references involving the administrator's input when necessary.
     * This method must be invoked before localizePolicy()
     *
     * @param references references parsed from a policy document.
     * @return false if the process cannot continue because the administrator canceled an operation for example.
     */
    public boolean resolveReferences(ExternalReference[] references) throws InvalidPolicyStreamException, PolicyImportCancelledException {
        Set<ExternalReference> unresolved = new LinkedHashSet<ExternalReference>();
        for (ExternalReference reference : references) {
            if (!reference.verifyReference()) {
                // for all references not resolved automatically add a page in a wizard
                unresolved.add(reference);
            }
        }
        if (!unresolved.isEmpty()) {
            ExternalReference[] unresolvedRefsArray = unresolved.toArray(new ExternalReference[unresolved.size()]);
            final Frame mw = TopComponents.getInstance().getTopParent();
            boolean wasCancelled = false;
            try {
                ResolveExternalPolicyReferencesWizard wiz =
                        ResolveExternalPolicyReferencesWizard.fromReferences(mw, unresolvedRefsArray);
                wiz.pack();
                Utilities.centerOnScreen(wiz);
                wiz.setModal(true);
                wiz.setVisible(true);
                // if the wizard returns false, we must return
                if (wiz.wasCanceled()) wasCancelled = true;
            } catch(Exception e) {
                return false;
            }

            if(wasCancelled) {
                throw new PolicyImportCancelledException();
            }
        }
        resolvedReferences = references;
        return true;
    }

    public Assertion localizePolicy(Element policyXML) throws InvalidPolicyStreamException {
        // Go through each assertion and fix the changed references.
        Assertion root;
        try {
            root = getWspReader().parsePermissively( XmlUtil.nodeToString(policyXML));
        } catch (IOException e) {
            throw new InvalidPolicyStreamException(e);
        }
        traverseAssertionTreeForLocalization(root);
        return root;
    }

    public Assertion localizePolicy(Assertion rootAssertion) {
        traverseAssertionTreeForLocalization(rootAssertion);
        return rootAssertion;
    }

    private boolean traverseAssertionTreeForLocalization(Assertion rootAssertion) {
        if (rootAssertion instanceof CompositeAssertion) {
            CompositeAssertion ca = (CompositeAssertion)rootAssertion;
            List children = ca.getChildren();
            Collection<Assertion> childrenToRemoveFromCA = new ArrayList<Assertion>();
            for (Object aChildren : children) {
                Assertion child = (Assertion) aChildren;
                if (!traverseAssertionTreeForLocalization(child)) {
                    childrenToRemoveFromCA.add(child);
                }
            }
            // remove the children that are no longer wanted
            for (Assertion aChildrenToRemoveFromCA : childrenToRemoveFromCA) {
                ca.removeChild(aChildrenToRemoveFromCA);
            }
            return true;
        } else {
            if (resolvedReferences == null)
                return true;
            boolean ret = true;
            for (ExternalReference resolvedReference : resolvedReferences) {
                if (!resolvedReference.localizeAssertion(rootAssertion)) {
                    ret = false;
                    break;
                }
            }
            return ret;
        }
    }

    private ExternalReference[] resolvedReferences = null;
}
