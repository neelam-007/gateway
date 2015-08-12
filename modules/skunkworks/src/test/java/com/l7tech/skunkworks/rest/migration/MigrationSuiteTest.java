package com.l7tech.skunkworks.rest.migration;

import com.l7tech.skunkworks.rest.migration.tests.*;
import com.l7tech.skunkworks.rest.tools.MigrationTestBase;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

/**
 * This runs all tests that extend MigrationTestBase. Note these tests should not end in "Test" otherwise they will be run twice on the build machine
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({
        ActiveConnectorMigration.class,
        AssertionAccessMigration.class,
        CassandraMigration.class,
        ClusterPropertyMigration.class,
        CustomKeyValueMigration.class,
        DocumentResourceMigration.class,
        EmailListenerMigration.class,
        EncapsulatedAssertionMigration.class,
        FirewallRuleMigration.class,
        FolderMigration.class,
        FullGatewayMigration.class,
        GeneralMigration.class,
        GenericEntityMigration.class,
        HttpConfigurationMigration.class,
        IdentityProviderMigration.class,
        InterfaceTagMigration.class,
        JDBCMigration.class,
        JMSMigration.class,
        ListenPortMigration.class,
        MQNativeActiveConnectorMigration.class,
        PolicyAliasMigration.class,
        PolicyBackedServiceMigration.class,
        PolicyIncludeFragmentMigration.class,
        PolicyMigration.class,
        PrivateKeyMigration.class,
        RevocationCheckMigration.class,
        RoleMigration.class,
        SampleMessageMigration.class,
        ScheduledTaskMigration.class,
        SecurePasswordMigration.class,
        SecurityZoneMigration.class,
        ServiceAliasMigration.class,
        ServiceMigration.class,
        SiteminderConfigurationMigration.class,
        TransactionRollback.class,
        TrustedCertificateMigration.class,
        UsersMigration.class,
        WorkQueueMigration.class
})
public class MigrationSuiteTest {

    @BeforeClass
    public static void setUp() throws Exception {
        MigrationTestBase.beforeClass();
        MigrationTestBase.runInSuite(true);
    }

    @AfterClass
    public static void tearDown() {
        MigrationTestBase.runInSuite(false);
        MigrationTestBase.afterClass();
    }
}
