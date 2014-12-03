package com.l7tech.server.event.bundle;

import com.l7tech.policy.bundle.MigrationDryRunResult;
import com.l7tech.server.policy.bundle.PolicyBundleInstallerCallback;
import com.l7tech.server.policy.bundle.PolicyBundleInstallerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DryRunInstallPolicyBundleEvent extends PolicyBundleInstallerEvent {

    public DryRunInstallPolicyBundleEvent(@NotNull final Object source,
                                          @NotNull final PolicyBundleInstallerContext context,
                                          @Nullable final PolicyBundleInstallerCallback policyBundleInstallerCallback) {
        super(source, context, policyBundleInstallerCallback);
    }

    public DryRunInstallPolicyBundleEvent(@NotNull final Object source,
                                          @NotNull final PolicyBundleInstallerContext context) {
        super(source, context, null);
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

    public void addEncapsulatedAssertionConflict(String conflict) {
        encapsulatedAssertionConflict.add(conflict);
    }

    public void addMigrationErrorMapping(MigrationDryRunResult migrationErrorMapping) {
        if (migrationErrorMappings == null) {
            migrationErrorMappings = new ArrayList<>();
        }
        migrationErrorMappings.add(migrationErrorMapping);
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

    public List<MigrationDryRunResult> getMigrationErrorMappings() {
        return Collections.unmodifiableList(migrationErrorMappings);
    }

    // - PRIVATE
    private List<String> serviceConflict = new ArrayList<>();
    private List<String> policyConflict = new ArrayList<>();
    private List<String> certificateConflict = new ArrayList<>();
    private List<String> jdbcConnsThatDontExist = new ArrayList<>();
    private List<String> missingAssertions = new ArrayList<>();
    private List<String> encapsulatedAssertionConflict = new ArrayList<>();
    private List<MigrationDryRunResult> migrationErrorMappings = new ArrayList<>();
}