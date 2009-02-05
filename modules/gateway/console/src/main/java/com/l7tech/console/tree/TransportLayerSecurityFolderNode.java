package com.l7tech.console.tree;

import com.l7tech.policy.assertion.ext.Category;


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
        super("Transport Layer Security (TLS)", "transportLayerSecurity");
    }


    /**
     * subclasses override this method
     */
    protected void doLoadChildren() {
        //children = null;
        int index = 0;
        insert(new SslTransportNode(false), index++);
        index = insertMatchingModularAssertions(index);
        insertMatchingCustomAssertions(index, Category.TRANSPORT_SEC);
    }
}
