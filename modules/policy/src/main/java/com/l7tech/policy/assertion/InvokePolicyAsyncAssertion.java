package com.l7tech.policy.assertion;

import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.Goid;

import static com.l7tech.policy.assertion.AssertionMetadata.*;


/**
 * Assertion for invoking a policy fragment asynchronously.
 */
public class InvokePolicyAsyncAssertion extends Assertion implements UsesEntities, WorkQueueable {

    private static final String META_INITIALIZED = InvokePolicyAsyncAssertion.class.getName() + ".metadataInitialized";

    private String workQueueName;
    private Goid workQueueGoid;
    private String policyName;
    private Goid policyGoid;

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

        // TODO SSG-11880 hide in GUI for now
        //meta.put(PALETTE_FOLDERS, new String[]{"policyLogic"});


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

    public String getWorkQueueName() {
        return workQueueName;
    }

    public void setWorkQueueName(String workQueueName) {
        this.workQueueName = workQueueName;
    }

    public Goid getWorkQueueGoid() {
        return workQueueGoid;
    }

    public void setWorkQueueGoid(Goid workQueueGoid) {
        this.workQueueGoid = workQueueGoid;
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

    @Override
    public EntityHeader[] getEntitiesUsed() {
        if (policyName == null)
            return new EntityHeader[0];

        EntityHeader policyHeader = new EntityHeader();
        if (policyName != null)
            policyHeader.setName(policyName);
        policyHeader.setGoid(policyGoid);
        policyHeader.setType(EntityType.POLICY);

        EntityHeader wqHeader = new EntityHeader();
        if (workQueueName != null)
            wqHeader.setName(workQueueName);
        wqHeader.setGoid(workQueueGoid);
        wqHeader.setType(EntityType.WORK_QUEUE);

        return new EntityHeader[]{policyHeader, wqHeader};
    }

    @Override
    public void replaceEntity(EntityHeader oldEntityHeader, EntityHeader newEntityHeader) {
        if (EntityType.POLICY.equals(oldEntityHeader.getType()) &&
                oldEntityHeader.getType().equals(newEntityHeader.getType()) &&
                Goid.equals(policyGoid, oldEntityHeader.getGoid())) {
            policyName = newEntityHeader.getName();
            policyGoid = newEntityHeader.getGoid();
        } else if (EntityType.WORK_QUEUE.equals(oldEntityHeader.getType()) &&
                oldEntityHeader.getType().equals(newEntityHeader.getType()) &&
                Goid.equals(workQueueGoid, oldEntityHeader.getGoid())) {
            workQueueName = newEntityHeader.getName();
            workQueueGoid = newEntityHeader.getGoid();
        }
    }
}
