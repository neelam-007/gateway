package com.l7tech.console.tree;

import java.util.logging.Logger;


/**
 * The class represents a node element in the palette assertion tree.
 * It represents the folder with composite and logical policy assertions.
 */
public class PolicyLogicFolderNode extends AbstractPaletteFolderNode {
    static final Logger log = Logger.getLogger(PolicyLogicFolderNode.class.getName());

    /**
     * construct the <CODE>PoliciesFolderNode</CODE> instance for
     * a given entry.
     */
    public PolicyLogicFolderNode() {
        super("Policy Logic");
    }

    /**
     * subclasses override this method
     */
    protected void loadChildren() {
        int index = 0;
        children = null;
        insert(new CommentAssertionPaletteNode(), index++);
        insert(new ComparisonAssertionPaletteNode(), index++);
        insert(new OneOrMoreNode(), index++);
        insert(new AllNode(), index++);
        insert(new TrueNode(), index++);
        insert(new FalseNode(), index++);
    }
}
