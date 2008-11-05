/*
 * Copyright (C) 2004-2007 Layer 7 Technologies Inc.
 */
package com.l7tech.console.tree.policy;

import com.l7tech.console.action.XslTransformationPropertiesAction;
import com.l7tech.policy.assertion.xml.XslTransformation;

import javax.swing.*;

/**
 * Policy tree node for XSL Transformation Assertion.
 */
public class XslTransformationTreeNode extends LeafAssertionTreeNode<XslTransformation> {
    public XslTransformationTreeNode(XslTransformation assertion) {
        super(assertion);
    }

    public String getName() {
        StringBuilder nodeName = new StringBuilder("XSL transform " + assertion.getTargetName());

        final String tname = assertion.getTransformName();
        if (tname != null && tname.length() > 0 && tname.trim().length() > 0) {
            nodeName.append(" - ").append(tname);
        }
        return nodeName.toString();
    }

    protected String iconResource(boolean open) {
        // todo, a special icon for this assertion?
        return "com/l7tech/console/resources/xmlsignature.gif";
    }

    /**
     * Gets the default action for this node.
     *
     * @return <code>null</code> indicating there should be none default action
     */
    public Action getPreferredAction() {
        return new XslTransformationPropertiesAction(this);
    }
}
