package com.l7tech.policy.assertion.composite;

import com.l7tech.objectmodel.migration.Migration;
import com.l7tech.objectmodel.migration.MigrationMappingSelection;
import com.l7tech.objectmodel.migration.PropertyResolver;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.AssertionMetadata;
import com.l7tech.policy.assertion.DefaultAssertionMetadata;

import com.l7tech.policy.assertion.UsesVariables;
import com.l7tech.policy.variable.Syntax;
import com.l7tech.search.Dependency;
import java.util.List;
import org.jetbrains.annotations.Nullable;

import static com.l7tech.objectmodel.ExternalEntityHeader.ValueType.TEXT_ARRAY;
import static com.l7tech.policy.assertion.AssertionMetadata.*;

/**
 * An AllAssertion that runs its children inside a single database transaction.
 */
public class TransactionAssertion extends CompositeAssertion implements UsesVariables {

    private String connectionName = null;

    public TransactionAssertion() {
    }

    public TransactionAssertion( List<? extends Assertion> children ) {
        super( children );
    }

    @Override
    public TransactionAssertion clone() {
        TransactionAssertion copy = (TransactionAssertion) super.clone();

        copy.setConnectionName(connectionName);
        return copy;
    }

    @Override
    @Migration(mapName = MigrationMappingSelection.NONE, mapValue = MigrationMappingSelection.REQUIRED, export = false, valueType = TEXT_ARRAY, resolver = PropertyResolver.Type.SERVER_VARIABLE)
    public String[] getVariablesUsed() {
      return Syntax.getReferencedNames(connectionName);
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

        meta.put(SHORT_NAME, "JDBC Transaction Group: All assertions must evaluate to true in a single transaction (for CA internal use)");
        meta.put(DESCRIPTION, "All child assertions must evaluate to true.  " +
                "All JDBC Query assertions under this assertion that share the same JDBC connection " +
                "name will be run in the same transaction.  Nested transactions, or use of more than one connection name at a time, " +
                "is not supported.");

        meta.put(POLICY_NODE_CLASSNAME, "com.l7tech.console.panels.TransactionAssertionPolicyNode");

        meta.put(PROPERTIES_ACTION_ICON, "com/l7tech/console/resources/folder.gif");
        meta.put(WSP_TYPE_MAPPING_CLASSNAME, "com.l7tech.policy.assertion.composite.TransactionAssertionTypeMapping");
        meta.put(FEATURE_SET_NAME,"assertion:composite.Transaction");

        return meta;
    }

    // TODO migration and dependency annotations
    @Nullable
    @Migration(mapName = MigrationMappingSelection.REQUIRED, export = false, resolver = PropertyResolver.Type.JDBC_CONNECTION)
    @Dependency(type = Dependency.DependencyType.JDBC_CONNECTION, methodReturnType = Dependency.MethodReturnType.NAME)
    public String getConnectionName() {
      return connectionName;
    }

    public void setConnectionName(@Nullable String connectionName) {
      this.connectionName = connectionName;
    }
}
