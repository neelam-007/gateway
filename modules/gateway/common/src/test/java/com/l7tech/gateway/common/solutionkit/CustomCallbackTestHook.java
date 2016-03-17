package com.l7tech.gateway.common.solutionkit;

import com.l7tech.policy.solutionkit.SolutionKitManagerCallback;
import com.l7tech.policy.solutionkit.SolutionKitManagerContext;
import com.l7tech.util.Functions;

/**
 * Helper class to test the Solution Kit Manager UI customization framework.
 */
public class CustomCallbackTestHook extends SolutionKitManagerCallback {
    private Functions.BinaryVoidThrows<SolutionKitManagerCallback, SolutionKitManagerContext, RuntimeException> testToRunPreMigrationBundleImport;

    private boolean preMigrationBundleImportCalled = false;

    @Override
    public void preMigrationBundleImport(final SolutionKitManagerContext context) throws CallbackException {
        preMigrationBundleImportCalled = true;

        // unit test hook
        if (testToRunPreMigrationBundleImport != null) {
            testToRunPreMigrationBundleImport.call(this, context);
        }
    }

    public void setTestToRunPreMigrationBundleImport(Functions.BinaryVoidThrows<SolutionKitManagerCallback, SolutionKitManagerContext, RuntimeException> testToRunPreMigrationBundleImport) {
        this.testToRunPreMigrationBundleImport = testToRunPreMigrationBundleImport;
    }

    public boolean isPreMigrationBundleImportCalled() {
        return preMigrationBundleImportCalled;
    }
}
