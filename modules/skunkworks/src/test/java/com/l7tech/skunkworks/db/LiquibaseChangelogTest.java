package com.l7tech.skunkworks.db;

import com.l7tech.test.BugId;
import junit.framework.Assert;
import liquibase.Liquibase;
import liquibase.changelog.ChangeSet;
import liquibase.database.Database;
import liquibase.exception.DatabaseException;
import liquibase.exception.LiquibaseException;
import liquibase.resource.FileSystemResourceAccessor;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import java.sql.SQLException;

/**
 * This tests that the database change log is valid and contains correct values.
 */
@RunWith(MockitoJUnitRunner.class)
public class LiquibaseChangelogTest {

    @Mock
    private Database mockConnection;

    @Before
    public void before() throws DatabaseException {
        Mockito.when(mockConnection.getDatabaseProductName()).thenReturn("Apache Derby");
    }

    /**
     * Tests that the author is always 'gateway'
     */
    @Test
    @BugId("SSG-9885")
    public void authorIsGateway() throws LiquibaseException, SQLException {
        Liquibase liquibase = new Liquibase("ssg.xml", new FileSystemResourceAccessor("etc/db/liquibase"), mockConnection);

        for (ChangeSet changeSet : liquibase.getDatabaseChangeLog().getChangeSets()) {
            Assert.assertEquals("The changeset author must be 'gateway', we do not put user names as changeset authors because they are visible to customers. Changeset: " + changeSet.toString(), "gateway", changeSet.getAuthor());
        }
    }
}
