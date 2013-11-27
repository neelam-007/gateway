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

    public void addCertificateNameWithConflict(String certificateName) {
        certificateConflict.add(certificateName);
    }

    public void addMissingJdbcConnection(String missingJdbcConn) {
        jdbcConnsThatDontExist.add(missingJdbcConn);
    }

    public void addMissingAssertion(String missingAssertion) {
        missingAssertions.add(missingAssertion);
    }

    public void addEncapsulatedAssertionNameWithConflict(String encapsulatedAssertionName, String encapsulatedAssertionGuid) {
        encapsulatedAssertionConflict.add(encapsulatedAssertionName + " (GUID: " + encapsulatedAssertionGuid + ")");
    }

    public void addEncapsulatedAssertionWithPolicyReferenceConflict(String conflict) {
        encapsulatedAssertionConflict.add(conflict);
    }

    public List<String> getServiceConflict() {
        return Collections.unmodifiableList(serviceConflict);
    }

    public List<String> getPolicyConflict() {
        return Collections.unmodifiableList(policyConflict);
    }

    public List<String> getCertificateConflict() {
        return Collections.unmodifiableList(certificateConflict);
    }

    public List<String> getJdbcConnsThatDontExist() {
        return Collections.unmodifiableList(jdbcConnsThatDontExist);
    }

    public List<String> getMissingAssertions() {
        return Collections.unmodifiableList(missingAssertions);
    }

    public List<String> getEncapsulatedAssertionConflict() {
        return Collections.unmodifiableList(encapsulatedAssertionConflict);
    }

    // - PRIVATE
    private List<String> serviceConflict = new ArrayList<>();
    private List<String> policyConflict = new ArrayList<>();
    private List<String> certificateConflict = new ArrayList<>();
    private List<String> jdbcConnsThatDontExist = new ArrayList<>();
    private List<String> missingAssertions = new ArrayList<>();
    private List<String> encapsulatedAssertionConflict = new ArrayList<>();
}