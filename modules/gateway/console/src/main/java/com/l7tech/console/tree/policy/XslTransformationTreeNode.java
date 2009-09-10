/*
 * Copyright (C) 2004-2007 Layer 7 Technologies Inc.
 */
package com.l7tech.console.tree.policy;

import com.l7tech.console.action.XslTransformationPropertiesAction;
import com.l7tech.console.action.SelectMessageTargetAction;
import com.l7tech.policy.assertion.xml.XslTransformation;
import com.l7tech.policy.assertion.AssertionUtils;

import javax.swing.*;
import java.util.*;

/**
 * Policy tree node for XSL Transformation Assertion.
 */
public class XslTransformationTreeNode extends LeafAssertionTreeNode<XslTransformation> {
    public XslTransformationTreeNode(XslTransformation assertion) {
        super(assertion);
    }

    public String getName(final boolean decorate) {
        final String assertionName = "Apply XSL Transformation";
        if(!decorate) return assertionName;
        
        StringBuilder nodeName = new StringBuilder(assertionName);

        final String tname = assertion.getTransformName();
        if (tname != null && tname.length() > 0 && tname.trim().length() > 0) {
            nodeName.append(" - ").append(tname);
        }
        return AssertionUtils.decorateName(assertion, nodeName);
    }

    @Override
    protected String iconResource(boolean open) {
        // todo, a special icon for this assertion?
        return "com/l7tech/console/resources/xmlsignature.gif";
    }

    /**
     * Gets the default action for this node.
     *
     * @return <code>null</code> indicating there should be none default action
     */
    @Override
    public Action getPreferredAction() {
        return new XslTransformationPropertiesAction(this);
    }
    
    @Override
    public Action[] getActions() {
        List<Action> actions = new ArrayList<Action>( Arrays.asList(super.getActions()) );

        int insertPosition = 1;
        if ( getPreferredAction()==null ) {
            insertPosition = 0;
        }

        actions.add( insertPosition, new SelectMessageTargetAction(this));

        return actions.toArray(new Action[actions.size()]);
    }

}
