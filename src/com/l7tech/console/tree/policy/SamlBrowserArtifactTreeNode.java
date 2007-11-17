/*
 * Copyright (C) 2003-2007 Layer 7 Technologies Inc.
 */
package com.l7tech.console.tree.policy;

import com.l7tech.console.action.EditSamlBrowserArtifactAction;
import com.l7tech.policy.assertion.xmlsec.SamlBrowserArtifact;

import javax.swing.*;

public class SamlBrowserArtifactTreeNode extends LeafAssertionTreeNode<SamlBrowserArtifact> {
    private EditSamlBrowserArtifactAction editAction = new EditSamlBrowserArtifactAction(this);

    public SamlBrowserArtifactTreeNode(SamlBrowserArtifact assertion) {
        super(assertion);
    }

    protected String iconResource(boolean open) {
        return "com/l7tech/console/resources/xmlsignature.gif";
    }

    public Action getPreferredAction() {
        return editAction;
    }

    public String getName() {
        return "Retrieve SAML Browser Artifact from " + assertion.getSsoEndpointUrl();
    }
}
