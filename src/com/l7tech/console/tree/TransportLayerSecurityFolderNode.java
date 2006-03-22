package com.l7tech.console.tree;




/**
 * The class represents a node element in the palette assertion tree.
 * It represents the folder with transport layer securitry.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version 1.1
 */
public class TransportLayerSecurityFolderNode extends AbstractPaletteFolderNode {
    /**
     * construct the <CODE>PoliciesFolderNode</CODE> instance for
     * a given entry.
     *
     */
    public TransportLayerSecurityFolderNode() {
        super("Transport Layer Security (TLS)");
    }


    /**
     * subclasses override this method
     */
    protected void loadChildren() {
        children = null;
        insert(new SslTransportNode(false), 0);
        insert(new StealthFaultNode(), 1);
    }
}
