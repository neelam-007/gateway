package com.l7tech.console.tree;

import com.l7tech.policy.assertion.ext.Category;

/**
 * The class represents an gui node element in the TreeModel that
 * represents a routing folder.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version 1.0
 */
public class XmlFolderNode extends AbstractPaletteFolderNode {
    /**
     * construct the <CODE>ProvidersFolderNode</CODE> instance.
     */
    public XmlFolderNode() {
        super("Message Validation/Transformation", "xml");
    }

    /**
     * subclasses override this method
     */
    @Override
    protected void doLoadChildren() {
        int index = 0;
        children = null;
        insert(new XslTransformationPaletteNode(), index++);
        index = insertMatchingModularAssertions(index);
        index = insertMatchingCustomAssertions(index, Category.MESSAGE);
        insertMatchingCustomAssertions(index, Category.MSG_VAL_XSLT);
    }
}