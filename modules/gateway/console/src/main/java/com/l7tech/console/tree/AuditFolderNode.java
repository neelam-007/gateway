package com.l7tech.console.tree;

import com.l7tech.policy.assertion.ext.Category;

/**
 * The class represents an gui node element in the TreeModel that
 * represents the audit assertions folder.
 */
public class AuditFolderNode extends AbstractPaletteFolderNode {
    /**
     * construct the <CODE>ProvidersFolderNode</CODE> instance.
     */
    public AuditFolderNode() {
        super("Logging, Auditing and Alerts", "audit");
    }

    /**
     * subclasses override this method
     */
    @Override
    protected void doLoadChildren() {
        int index = 0;
        children = null;
        insert( new EmailAlertAssertionPaletteNode(), index++ );
        index = insertMatchingModularAssertions(index);
        insertMatchingCustomAssertions(index, Category.AUDIT_ALERT);
    }
}
