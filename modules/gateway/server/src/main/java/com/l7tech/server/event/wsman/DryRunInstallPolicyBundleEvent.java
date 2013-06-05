package com.l7tech.server.event.wsman;

import com.l7tech.server.policy.bundle.PolicyBundleInstallerContext;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DryRunInstallPolicyBundleEvent extends PolicyBundleEvent {

    public DryRunInstallPolicyBundleEvent(final Object source,
                                          final PolicyBundleInstallerContext context) {
        super(source, context);
    }

    public void addServiceConflict(String urlPatternOrServiceName) {
        serviceConflict.add(urlPatternOrServiceName);
    }

    public void addPolicyNameWithConflict(String policyName) {
        policyConflict.add(policyName);
    }

    public void addMissingJdbcConnection(String missingJdbcConn) {
        jdbcConnsThatDontExist.add(missingJdbcConn);
    }

    public List<String> getServiceConflict() {
        return Collections.unmodifiableList(serviceConflict);
    }

    public List<String> getPolicyConflict() {
        return Collections.unmodifiableList(policyConflict);
    }

    public List<String> getJdbcConnsThatDontExist() {
        return Collections.unmodifiableList(jdbcConnsThatDontExist);
    }

    // - PRIVATE
    private List<String> serviceConflict = new ArrayList<String>();
    private List<String> policyConflict = new ArrayList<String>();
    private List<String> jdbcConnsThatDontExist = new ArrayList<String>();
}
