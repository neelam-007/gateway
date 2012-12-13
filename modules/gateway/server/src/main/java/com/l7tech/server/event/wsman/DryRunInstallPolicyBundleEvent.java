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

    public void addUrlPatternWithConflict(String urlPattern) {
        serviceWithUriConflict.add(urlPattern);
    }

    public void addPolicyNameWithConflict(String policyName) {
        policyWithNameConflict.add(policyName);
    }

    public void addMissingJdbcConnection(String missingJdbcConn) {
        jdbcConnsThatDontExist.add(missingJdbcConn);
    }

    public List<String> getUrlPatternWithConflict() {
        return Collections.unmodifiableList(serviceWithUriConflict);
    }

    public List<String> getPolicyWithNameConflict() {
        return Collections.unmodifiableList(policyWithNameConflict);
    }

    public List<String> getJdbcConnsThatDontExist() {
        return Collections.unmodifiableList(jdbcConnsThatDontExist);
    }

    // - PRIVATE
    private List<String> serviceWithUriConflict = new ArrayList<String>();
    private List<String> policyWithNameConflict = new ArrayList<String>();
    private List<String> jdbcConnsThatDontExist = new ArrayList<String>();
}
