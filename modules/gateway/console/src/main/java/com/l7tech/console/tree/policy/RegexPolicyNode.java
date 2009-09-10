package com.l7tech.console.tree.policy;


import com.l7tech.console.action.RegexPropertiesAction;
import com.l7tech.console.action.SelectMessageTargetAction;
import com.l7tech.policy.assertion.Regex;
import com.l7tech.policy.assertion.AssertionUtils;

import javax.swing.*;
import java.util.*;

/**
 * Class RegexPolicyNode is a policy node that corresponds the
 * <code>Regex</code> assertion.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 */
public class RegexPolicyNode extends LeafAssertionTreeNode<Regex> {

    public RegexPolicyNode(Regex assertion) {
        super(assertion);
    }

    /**
     * @return the node name that is displayed
     */
    @Override
    public String getName(final boolean decorate) {
        final String assertionName = "Evaluate Regular Expression";
        
        Regex regex = asAssertion();
        StringBuilder nameBuffer = new StringBuilder(256);
        nameBuffer.append(assertionName);

        if (regex.getRegexName() != null) {
            nameBuffer.append(" - ");
            nameBuffer.append(regex.getRegexName());
        }

        return (decorate)? AssertionUtils.decorateName( regex, nameBuffer): assertionName;
    }

    /**
     * Gets the default action for this node.
     *
     * @return <code>null</code> indicating there should be none default action
     */
    @Override
    public Action getPreferredAction() {
        return new RegexPropertiesAction(this);
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

    /**
     * subclasses override this method specifying the resource name
     *
     * @param open for nodes that can be opened, can have children
     */
    @Override
    protected String iconResource(boolean open) {
        return "com/l7tech/console/resources/regex16.gif";
    }
}