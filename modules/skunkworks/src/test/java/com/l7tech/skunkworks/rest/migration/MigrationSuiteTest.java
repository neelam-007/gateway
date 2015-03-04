package com.l7tech.skunkworks.rest.migration;

import com.l7tech.skunkworks.rest.migration.tests.*;
import com.l7tech.skunkworks.rest.tools.MigrationTestBase;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

/**
 * Created by vkazakov on 3/2/2015.
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({
        AssertionAccessMigration.class,
        CassandraMigration.class,
        ClusterPropertyMigration.class,
        DocumentResourceMigration.class,
        EncapsulatedAssertionMigration.class,
        FolderMigration.class,
        FullGatewayMigration.class,
        GeneralMigration.class,
        GenericEntityMigration.class,
        InterfaceTagMigration.class,
        JDBCMigration.class,
        JMSMigration.class,
        MQNativeActiveConnectorMigration.class,
        PolicyAliasMigration.class,
        PolicyIncludeFragmentMigration.class,
        PolicyMigration.class,
        PrivateKeyMigration.class,
        RevocationCheckMigration.class,
        SampleMessageMigration.class,
        SecurePasswordMigration.class,
        SecurityZoneMigration.class,
        ServiceAliasMigration.class,
        ServiceMigration.class,
        SiteminderConfigurationMigration.class,
        TransactionRollback.class,
        TrustedCertificateMigration.class,
        UsersMigration.class
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
