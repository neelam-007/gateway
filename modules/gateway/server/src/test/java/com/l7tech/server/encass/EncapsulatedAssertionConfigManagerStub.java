package com.l7tech.server.encass;

import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.GuidEntityHeader;
import com.l7tech.objectmodel.ObjectNotFoundException;
import com.l7tech.objectmodel.encass.EncapsulatedAssertionConfig;
import com.l7tech.policy.Policy;
import com.l7tech.server.EntityManagerStub;
import com.l7tech.server.policy.EncapsulatedAssertionConfigManager;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 *
 */
public class EncapsulatedAssertionConfigManagerStub extends EntityManagerStub<EncapsulatedAssertionConfig,GuidEntityHeader> implements EncapsulatedAssertionConfigManager {

    public EncapsulatedAssertionConfigManagerStub(EncapsulatedAssertionConfig... configs) {
        super(configs);
    }

    @NotNull
    @Override
    public Collection<EncapsulatedAssertionConfig> findByPolicyGoid(Goid policyOid) throws FindException {
        Collection<EncapsulatedAssertionConfig> got = findAll();
        List<EncapsulatedAssertionConfig> ret = new ArrayList<EncapsulatedAssertionConfig>();
        for (EncapsulatedAssertionConfig config : got) {
            Policy policy = config.getPolicy();
            if (policy != null && Goid.equals(policy.getGoid(), policyOid))
                ret.add(config);
        }
        return ret;
    }

    @Override
    public EncapsulatedAssertionConfig findByGuid(String guid) throws FindException {
        Collection<EncapsulatedAssertionConfig> got = findAll();
        for (EncapsulatedAssertionConfig conf : got) {
            final String confGuid = conf.getGuid();
            if (confGuid != null && confGuid.equals(guid))
                return conf;
        }
        throw new ObjectNotFoundException("No encapsulated assertion config foudn with guid " + guid);
    }
}
