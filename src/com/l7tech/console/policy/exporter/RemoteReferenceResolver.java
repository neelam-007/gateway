package com.l7tech.console.policy.exporter;

import com.l7tech.common.gui.util.Utilities;
import com.l7tech.common.util.XmlUtil;
import com.l7tech.console.panels.ResolveExternalPolicyReferencesWizard;
import com.l7tech.console.util.TopComponents;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.composite.CompositeAssertion;
import com.l7tech.policy.wsp.InvalidPolicyStreamException;
import com.l7tech.policy.wsp.WspReader;
import org.w3c.dom.Element;

import javax.swing.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
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
    public boolean resolveReferences(ExternalReference[] references) {
        Collection unresolved = new ArrayList();
        List<IncludedPolicyReference> conflictingIncludes = new ArrayList<IncludedPolicyReference>();
        for (ExternalReference reference : references) {
            if (!reference.verifyReference()) {
                if(reference instanceof IncludedPolicyReference) {
                    conflictingIncludes.add((IncludedPolicyReference)reference);
                } else {
                    // for all references not resolved automatically add a page in a wizard
                    unresolved.add(reference);
                }
            }
        }

        if(!conflictingIncludes.isEmpty()) {
            StringBuilder message = new StringBuilder("<html>The following included policies conflict with existing policies:<ul>");
            for(IncludedPolicyReference reference : conflictingIncludes) {
                message.append("<li>");
                message.append(reference.getName());
                message.append("</li>");
            }
            message.append("</ul>");
            JOptionPane.showMessageDialog(TopComponents.getInstance().getTopParent(),
                    message.toString(), "Import Failure", JOptionPane.ERROR_MESSAGE);
            return false;
        }

        if (!unresolved.isEmpty()) {
            ExternalReference[] unresolvedRefsArray = (ExternalReference[])unresolved.toArray(new ExternalReference[0]);
            final Frame mw = TopComponents.getInstance().getTopParent();
            ResolveExternalPolicyReferencesWizard wiz =
                    ResolveExternalPolicyReferencesWizard.fromReferences(mw, unresolvedRefsArray);
            wiz.pack();
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
        Assertion root;
        try {
            root = getWspReader().parsePermissively(XmlUtil.nodeToString(policyXML));
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
            for (Object aChildren : children) {
                Assertion child = (Assertion) aChildren;
                if (!traverseAssertionTreeForLocalization(child)) {
                    childrenToRemoveFromCA.add(child);
                }
            }
            // remove the children that are no longer wanted
            for (Object aChildrenToRemoveFromCA : childrenToRemoveFromCA) {
                ca.removeChild((Assertion) aChildrenToRemoveFromCA);
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
