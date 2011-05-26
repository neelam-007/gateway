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
        children = null;
        insertModularAssertionByType(SqlAttackAssertion.class );
        insertModularAssertionByType(CodeInjectionProtectionAssertion.class );
        insertModularAssertionByType(OversizedTextAssertion.class);
        insertMatchingModularAssertions();
        insertMatchingCustomAssertions(Category.THREAT_PROT);
    }
}