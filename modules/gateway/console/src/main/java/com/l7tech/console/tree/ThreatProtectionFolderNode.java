package com.l7tech.console.tree;

import com.l7tech.policy.assertion.ext.Category;
import com.l7tech.console.tree.policy.CodeInjectionProtectionAssertionPaletteNode;

/**
 * The class represents an gui node element in the TreeModel that
 * represents the audit assertions folder.
 */
public class ThreatProtectionFolderNode extends AbstractPaletteFolderNode {
    /**
     * construct the <CODE>ProvidersFolderNode</CODE> instance.
     */
    public ThreatProtectionFolderNode() {
        super("Threat Protection", "threatProtection");
    }

    /**
     * subclasses override this method
     */
    protected void doLoadChildren() {
        int index = 0;
        children = null;
        insert( new SqlAttackAssertionPaletteNode(), index++ );
        insert( new CodeInjectionProtectionAssertionPaletteNode(), index++);
        insert( new RequestSizeLimitPaletteNode(), index++ );
        insert( new OversizedTextAssertionPaletteNode(), index++ );
        insert( new RequestWssReplayProtectionNode(), index++ );
        insert( new SchemaValidationPaletteNode(), index++ );
        //insert(new FaultLevelPaletteNode(), index++);
        index = insertMatchingModularAssertions(index);
        insertMatchingCustomAssertions(index, Category.THREAT_PROT);
    }
}