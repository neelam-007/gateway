package com.l7tech.console.tree.policy;


import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.credential.http.HttpBasic;
import com.l7tech.policy.assertion.composite.CompositeAssertion;
import com.l7tech.console.util.IconManager2;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.*;
import java.util.Iterator;
import java.awt.*;

/**
 * Class HttpBasicAuthAssertionTreeNode is a tree node that correspinds
 * to the <code>HttpBAsic</code> asseriton.
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 */
class HttpBasicAuthAssertionTreeNode extends LeafAssertionTreeNode {

    public HttpBasicAuthAssertionTreeNode(HttpBasic assertion) {
        super(assertion);
    }

    /**
     * subclasses override this method specifying the resource name
     *
     * @param open for nodes that can be opened, can have children
     */
    protected String iconResource(boolean open) {
        return "com/l7tech/console/resources/encryption.gif";
    }
}