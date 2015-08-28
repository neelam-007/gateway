package com.l7tech.policy.assertion.composite;

import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.AssertionMetadata;
import com.l7tech.policy.assertion.DefaultAssertionMetadata;

import java.util.List;

import static com.l7tech.policy.assertion.AssertionMetadata.*;
import static com.l7tech.policy.assertion.AssertionMetadata.CLIENT_ASSERTION_POLICY_ICON_OPEN;
import static com.l7tech.policy.assertion.AssertionMetadata.USED_BY_CLIENT;

/**
 * An AllAssertion that runs its children inside a single database transaction.
 */
public class TransactionAssertion extends CompositeAssertion {

    public TransactionAssertion() {
    }

    public TransactionAssertion( List<? extends Assertion> children ) {
        super( children );
    }

    @Override
    public boolean permitsEmpty() {
        return true;
    }

    @Override
    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = defaultMeta();

        // TODO Hide in palette until we can commit resources to test this properly in Musket release
        //meta.put(PALETTE_FOLDERS, new String[]{"policyLogic"});
        meta.put(PALETTE_NODE_ICON, "com/l7tech/console/resources/folder.gif");
        meta.put(POLICY_NODE_ICON_OPEN, "com/l7tech/console/resources/folderOpen.gif");

        meta.put(SHORT_NAME, "JDBC Transaction Group: All assertions must evaluate to true in a single transaction");
        meta.put(DESCRIPTION, "All child assertions must evaluate to true.  " +
                "All JDBC Query assertions under this assertion that share the same JDBC connection " +
                "name will be run in the same transaction.  Nested transactions, or use of more than one connection name at a time, " +
                "is not supported.");

        meta.put(POLICY_NODE_CLASSNAME, "com.l7tech.console.panels.TransactionAssertionPolicyNode");

        meta.put(PROPERTIES_ACTION_ICON, "com/l7tech/console/resources/folder.gif");
        meta.put(WSP_TYPE_MAPPING_CLASSNAME, "com.l7tech.policy.assertion.composite.TransactionAssertionTypeMapping");

        return meta;
    }


}
