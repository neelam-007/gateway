package com.l7tech.console.tree;

import com.l7tech.policy.assertion.ext.Category;
import com.l7tech.policy.assertion.SqlAttackAssertion;
import com.l7tech.policy.assertion.CodeInjectionProtectionAssertion;
import com.l7tech.policy.assertion.OversizedTextAssertion;

/**
 * The class represents an gui node element in the TreeModel that
 * represents the threat protection assertions folder.
 */
public class ThreatProtectionFolderNode extends AbstractPaletteFolderNode {

    /**
     * Construct the <CODE>ThreatProtectionFolderNode</CODE> instance.
     */
    public ThreatProtectionFolderNode() {
        super("Threat Protection", "threatProtection");
    }

    /**
     * subclasses override this method
     */
    @Override
    protected void doLoadChildren() {
        int index = 0;
        children = null;
        insertModularAssertionByType( index++, SqlAttackAssertion.class );
        insertModularAssertionByType( index++, CodeInjectionProtectionAssertion.class );
        insert( new RequestSizeLimitPaletteNode(), index++ );
        insertModularAssertionByType( index++, OversizedTextAssertion.class );
        //insert(new FaultLevelPaletteNode(), index++);
        index = insertMatchingModularAssertions(index);
        insertMatchingCustomAssertions(index, Category.THREAT_PROT);
    }
}