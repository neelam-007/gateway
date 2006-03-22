package com.l7tech.console.tree;

/**
 * The class represents an gui node element in the TreeModel that
 * represents the audit assertions folder.
 */
public class AuditFolderNode extends AbstractPaletteFolderNode {
    /**
     * construct the <CODE>ProvidersFolderNode</CODE> instance.
     */
    public AuditFolderNode() {
        super("Logging, Auditing and Alerts");
    }

    /**
     * subclasses override this method
     */
    protected void loadChildren() {
        int index = 0;
        children = null;
        insert( new AuditAssertionPaletteNode(), index++ );
        insert( new AuditDetailAssertionPaletteNode(), index++ );
        insert( new SnmpTrapAssertionPaletteNode(), index++ );
        insert( new EmailAlertAssertionPaletteNode(), index++ );
    }
}
