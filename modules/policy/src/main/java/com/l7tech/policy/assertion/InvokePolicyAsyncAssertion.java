package com.l7tech.policy.assertion;

import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.migration.Migration;
import com.l7tech.objectmodel.migration.MigrationMappingSelection;
import com.l7tech.objectmodel.migration.PropertyResolver;
import com.l7tech.search.Dependency;

import static com.l7tech.policy.assertion.AssertionMetadata.*;


/**
 * Assertion for invoking a policy fragment asynchronously.
 */
public class InvokePolicyAsyncAssertion extends Assertion implements UsesEntities, WorkQueueable {

    private static final String META_INITIALIZED = InvokePolicyAsyncAssertion.class.getName() + ".metadataInitialized";

    private String workQueueName;
    private String policyName;
    private Goid policyGoid;
    private String policyBackedServiceName = null;
    private Goid policyBackedServiceGoid = null;

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

    @Dependency(type = Dependency.DependencyType.WORK_QUEUE, methodReturnType = Dependency.MethodReturnType.NAME)
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

    public Goid getPolicyGoid() {
        return policyGoid;
    }

    public void setPolicyGoid(Goid policyGoid) {
        this.policyGoid = policyGoid;
    }

    public String getPolicyBackedServiceName() {
        return policyBackedServiceName;
    }

    public void setPolicyBackedServiceName(String policyBackedServiceName) {
        this.policyBackedServiceName = policyBackedServiceName;
    }

    public Goid getPolicyBackedServiceGoid() {
        return policyBackedServiceGoid;
    }

    public void setPolicyBackedServiceGoid(Goid policyBackedServiceGoid) {
        this.policyBackedServiceGoid = policyBackedServiceGoid;
    }

    @Override
    public EntityHeader[] getEntitiesUsed() {
        if (policyBackedServiceGoid == null)
            return new EntityHeader[0];

        EntityHeader header = new EntityHeader();
        if (policyBackedServiceName != null)
            header.setName(policyBackedServiceName);
        header.setGoid(policyBackedServiceGoid);
        header.setType(EntityType.POLICY_BACKED_SERVICE);
        return new EntityHeader[]{header};
    }

    @Override
    public void replaceEntity(EntityHeader oldEntityHeader, EntityHeader newEntityHeader) {
        if (EntityType.POLICY_BACKED_SERVICE.equals(oldEntityHeader.getType()) &&
                oldEntityHeader.getType().equals(newEntityHeader.getType()) &&
                Goid.equals(policyBackedServiceGoid, oldEntityHeader.getGoid())) {
            policyBackedServiceName = newEntityHeader.getName();
            policyBackedServiceGoid = newEntityHeader.getGoid();
        }
    }
}
