package com.l7tech.console.tree;




/**
 * The class represents a node element in the palette assertion tree.
 * It represents the folder with XML security elements.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version 1.1
 */
public class XmlSecurityFolderNode extends AbstractPaletteFolderNode {
    /**
     * construct the <CODE>PoliciesFolderNode</CODE> instance for
     * a given entry.
     *
     */
    public XmlSecurityFolderNode() {
        super("XML Security");
    }

    /**
     * subclasses override this method
     */
    protected void loadChildren() {
        int index = 0;
        insert(new RequestWssIntegrityNode(), index++);
        insert(new RequestWssConfidentialityNode(), index++);
        insert(new ResponseWssIntegrityNode(), index++);
        insert(new ResponseWssConfidentialityNode(), index++);
        insert(new RequestWssReplayProtectionNode(), index++);
        insert(new RequestWssTimestampPaletteNode(), index++);
        insert(new ResponseWssTimestampPaletteNode(), index++);
        insert(new WssTimestampPaletteNode(), index++);
        insert(new ResponseWssSecurityTokenPaletteNode(), index++);
    }
}
