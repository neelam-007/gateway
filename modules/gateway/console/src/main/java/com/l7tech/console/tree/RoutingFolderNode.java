package com.l7tech.console.tree;

import com.l7tech.policy.assertion.ext.Category;

/**
 * The class represents an gui node element in the TreeModel that
 * represents a routing folder.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version 1.0
 */
public class RoutingFolderNode extends AbstractPaletteFolderNode {
    /**
     * construct the <CODE>ProvidersFolderNode</CODE> instance.
     */
    public RoutingFolderNode() {
        super("Message Routing", "routing");
    }

    /**
     * subclasses override this method
     */
    protected void doLoadChildren() {
        int index = 0;
        children = null;
        // insert(new SmtpRoutingNode(), index++);
        index = insertMatchingModularAssertions(index);
        insertMatchingCustomAssertions(index, Category.ROUTING);
    }
}
