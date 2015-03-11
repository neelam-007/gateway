package com.l7tech.policy.assertion;

import com.l7tech.objectmodel.migration.Migration;
import com.l7tech.objectmodel.migration.MigrationMappingSelection;
import com.l7tech.objectmodel.migration.PropertyResolver;

import static com.l7tech.objectmodel.ExternalEntityHeader.ValueType.TEXT_ARRAY;
import static com.l7tech.policy.assertion.AssertionMetadata.*;


/**
 * Assertion for invoking a policy fragment asynchronously.
 */
public class InvokePolicyAsyncAssertion extends Assertion {

    private static final String META_INITIALIZED = InvokePolicyAsyncAssertion.class.getName() + ".metadataInitialized";

    private String workQueueName;
    private String policyName;

    @Override
    public AssertionMetadata meta() {
        final DefaultAssertionMetadata meta = defaultMeta();
        if (Boolean.TRUE.equals(meta.get(META_INITIALIZED)))
            return meta;

        meta.put(SHORT_NAME, "Invoke Policy Asynchronously");
        meta.put(DESCRIPTION, "Invokes a policy fragment asynchronously using a work queue.");
        meta.put(POLICY_ADVICE_CLASSNAME, "auto");
        meta.put(PROPERTIES_EDITOR_CLASSNAME, "com.l7tech.console.panels.InvokePolicyAsyncAssertionDialog");
        meta.put(PALETTE_NODE_ICON, "com/l7tech/console/resources/polback16.gif");
        meta.put(PALETTE_FOLDERS, new String[]{"policyLogic"});


        meta.put(AssertionMetadata.POLICY_NODE_NAME_FACTORY, new AssertionNodeNameFactory<InvokePolicyAsyncAssertion>() {
            @Override
            public String getAssertionName(final InvokePolicyAsyncAssertion assertion, final boolean decorate) {
                final String displayName = meta.getString(AssertionMetadata.SHORT_NAME);
                if (!decorate)
                    return displayName;

                StringBuilder sb = new StringBuilder("Invoke Policy Asynchronously: ");
                sb.append(assertion.getPolicyName());
                sb.append("; Using Work Queue: ");
                sb.append(assertion.getWorkQueueName());
                return AssertionUtils.decorateName(assertion, sb);
            }
        });

        meta.put(META_INITIALIZED, Boolean.TRUE);
        return meta;
    }

    //@Migration(mapName = MigrationMappingSelection.REQUIRED, export = false, resolver = PropertyResolver.Type.WORK_QUEUE)
    //@Dependency(type = Dependency.DependencyType.WORK_QUEUE, methodReturnType = Dependency.MethodReturnType.NAME)
    public String getWorkQueueName() {
        return workQueueName;
    }

    public void setWorkQueueName(String workQueueName) {
        this.workQueueName = workQueueName;
    }

    public String getPolicyName() {
        return policyName;
    }

    public void setPolicyName(String policyName) {
        this.policyName = policyName;
    }
}
