package com.l7tech.policy.exporter;

import com.l7tech.common.gui.util.Utilities;
import com.l7tech.common.util.XmlUtil;
import com.l7tech.console.MainWindow;
import com.l7tech.console.panels.ResolveExternalPolicyReferencesWizard;
import com.l7tech.console.util.Registry;
import com.l7tech.console.util.TopComponents;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.composite.CompositeAssertion;
import com.l7tech.policy.assertion.ext.CustomAssertionsRegistrar;
import com.l7tech.policy.wsp.InvalidPolicyStreamException;
import org.w3c.dom.Element;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
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
        Collection unresolved = new ArrayList();
        for (int i = 0; i < references.length; i++) {
            ExternalReference reference = references[i];
            if (!reference.verifyReference()) {
                // for all references not resolved automatically add a page in a wizard
                unresolved.add(reference);
            }
        }
        if (!unresolved.isEmpty()) {
            ExternalReference[] unresolvedRefsArray = (ExternalReference[])unresolved.toArray(new ExternalReference[0]);
            final MainWindow mw = TopComponents.getInstance().getMainWindow();
            ResolveExternalPolicyReferencesWizard wiz =
                    ResolveExternalPolicyReferencesWizard.fromReferences(mw, unresolvedRefsArray);
            wiz.pack();
            wiz.setSize(1000, 560);
            Utilities.centerOnScreen(wiz);
            wiz.setModal(true);
            wiz.setVisible(true);
            // if the wizard returns false, we must return
            if (wiz.wasCanceled()) return false;
        }
        resolvedReferences = references;
        return true;
    }

    public Assertion localizePolicy(Element policyXML) throws InvalidPolicyStreamException {
        // Go through each assertion and fix the changed references.
        final CustomAssertionsRegistrar cr = Registry.getDefault().getCustomAssertionsRegistrar();
        Assertion root = null;
        try {
            root = cr.resolvePolicy(XmlUtil.nodeToString(policyXML));
        } catch (IOException e) {
            throw new InvalidPolicyStreamException(e);
        }
        traverseAssertionTreeForLocalization(root);
        return root;
    }

    private boolean traverseAssertionTreeForLocalization(Assertion rootAssertion) {
        if (rootAssertion instanceof CompositeAssertion) {
            CompositeAssertion ca = (CompositeAssertion)rootAssertion;
            List children = ca.getChildren();
            Collection childrenToRemoveFromCA = new ArrayList();
            for (Iterator i = children.iterator(); i.hasNext();) {
                Assertion child = (Assertion)i.next();
                if (!traverseAssertionTreeForLocalization(child)) {
                    childrenToRemoveFromCA.add(child);
                }
            }
            // remove the children that are no longer wanted
            for (Iterator it = childrenToRemoveFromCA.iterator(); it.hasNext();) {
                ca.removeChild((Assertion)it.next());
            }
            return true;
        } else {
            if (resolvedReferences == null)
                return true;
            boolean ret = true;
            for (int i = 0; i < resolvedReferences.length; i++) {
                if (!resolvedReferences[i].localizeAssertion(rootAssertion)) {
                    ret = false;
                    break;
                }
            }
            return ret;
        }
    }

    private ExternalReference[] resolvedReferences = null;
}
