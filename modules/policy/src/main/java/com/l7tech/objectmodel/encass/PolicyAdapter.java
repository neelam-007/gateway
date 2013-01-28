package com.l7tech.objectmodel.encass;

import com.l7tech.policy.Policy;
import org.jetbrains.annotations.Nullable;

import javax.xml.bind.annotation.adapters.XmlAdapter;

/**
 * Transforms Policy to/from SimplifiedPolicy.
 */
public class PolicyAdapter extends XmlAdapter<SimplifiedPolicy, Policy> {
    /**
     * Creates a Policy from a SimplifiedPolicy.
     *
     * Any Policy fields that do not have a corresponding SimplifiedPolicy field will have default values.
     *
     * @param simplifiedPolicy
     * @return a Policy with fields set from the given SimplifiedPolicy or null if the given SimplifiedPolicy is null.
     * @throws Exception
     */
    @Override
    @Nullable
    public Policy unmarshal(@Nullable final SimplifiedPolicy simplifiedPolicy) throws Exception {
        Policy policy = null;
        if (simplifiedPolicy != null) {
            policy = new Policy(null, simplifiedPolicy.getName(), null, false);
            policy.setGuid(simplifiedPolicy.getGuid());
        }
        return policy;
    }

    /**
     * Simplifies a policy.
     *
     * @param policy the Policy to simplify.
     * @return a SimplifiedPolicy which is a Policy with limited info or null if the given Policy is null.
     * @throws Exception
     */
    @Override
    @Nullable
    public SimplifiedPolicy marshal(@Nullable final Policy policy) throws Exception {
        SimplifiedPolicy simple = null;
        if (policy != null) {
            simple = new SimplifiedPolicy(policy);
        }
        return simple;
    }
}
