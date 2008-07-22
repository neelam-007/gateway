package com.l7tech.console.tree.policy;

import com.l7tech.console.action.RequestSizeLimitDialogAction;
import com.l7tech.policy.assertion.RequestSizeLimit;

import javax.swing.*;

/**
 * Created by IntelliJ IDEA.
 * User: megery
 * Date: Sep 29, 2005
 * Time: 3:52:29 PM
 * To change this template use File | Settings | File Templates.
 */
public class RequestSizeLimitTreeNode extends LeafAssertionTreeNode{
    /**
     * Instantiate the new <code>LeafAssertionTreeNode</code>
     * with the given assertion.
     *
     * @param assertion the assertion
     */
    public RequestSizeLimitTreeNode(RequestSizeLimit assertion) {
        super(assertion);
    }

    public String getName() {
        return "Request Size Limit";
    }

    protected String iconResource(boolean open) {
        return "com/l7tech/console/resources/MessageLength-16x16.gif";
    }

    public Action getPreferredAction() {
        return new RequestSizeLimitDialogAction(this);
    }
}
