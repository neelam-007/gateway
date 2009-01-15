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
    protected void loadChildren() {
        int index = 0;
        children = null;
        insert(new RequestXpathPaletteNode(), index++);
        insert(new ResponseXpathPaletteNode(), index++);
        insert(new SchemaValidationPaletteNode(), index++);
        insert(new XslTransformationPaletteNode(), index++);
        insert(new RequestSwAAssertionPaletteNode(), index++);
        insert(new RegexNode(), index++);
        insert(new OperationPaletteNode(), index++);
        insert(new HtmlFormDataAssertionPaletteNode(), index++);
        insert(new HttpFormPostNode(), index++);
        insert(new InverseHttpFormPostNode(), index++);
        insert(new WsiBspPaletteNode(), index++);
        insert(new WsiSamlPaletteNode(), index++);
        insert(new PreemptiveCompressionPaletteNode(), index++);
        index = insertMatchingModularAssertions(index);
        index = insertMatchingCustomAssertions(index, Category.MESSAGE);
        insertMatchingCustomAssertions(index, Category.MSG_VAL_XSLT);
    }
}