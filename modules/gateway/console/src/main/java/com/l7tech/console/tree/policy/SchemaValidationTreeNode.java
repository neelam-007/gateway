/*
 * Copyright (C) 2004-2007 Layer 7 Technologies Inc.
 */
package com.l7tech.console.tree.policy;

import com.l7tech.console.action.SchemaValidationPropertiesAction;
import com.l7tech.console.action.SelectMessageTargetAction;
import com.l7tech.objectmodel.FindException;
import com.l7tech.policy.assertion.xml.SchemaValidation;
import com.l7tech.policy.assertion.AssertionUtils;

import javax.swing.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.*;

/**
 * Policy tree node for Schema Validation Assertion.
 */
public class SchemaValidationTreeNode extends LeafAssertionTreeNode<SchemaValidation> {
    public SchemaValidationTreeNode(SchemaValidation assertion) {
        super(assertion);
    }

    @Override
    public String getName() {
        return AssertionUtils.decorateName(assertion, "Validate Schema");
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
        try {
            return new SchemaValidationPropertiesAction(this, getService());
        } catch (FindException e) {
            log.log(Level.WARNING, "cannot get service", e);
        }
        return null;
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

    private final Logger log = Logger.getLogger(getClass().getName());
}
