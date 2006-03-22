package com.l7tech.console.tree;



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
        super("Message Routing");
    }

    /**
     * subclasses override this method
     */
    protected void loadChildren() {
        int index = 0;
        children = null;
        insert(new HttpRoutingNode(), index++);
        insert(new BridgeRoutingNode(), index++);
        insert(new JmsRoutingNode(), index++);
        // insert(new SmtpRoutingNode(), index++);
    }
}
