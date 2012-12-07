package com.l7tech.console.tree;

import com.l7tech.policy.assertion.ext.Category;

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
        super("Policy Logic", "policyLogic");
    }

    /**
     * subclasses override this method
     */
    @Override
    protected void doLoadChildren() {
        children = null;
        insertMatchingModularAssertions();
        insertMatchingCustomAssertions(Category.LOGIC);
        insertMatchingEncapsulatedAssertions();
    }
}
