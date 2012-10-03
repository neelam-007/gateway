package com.l7tech.portal.reports.definition;

import com.l7tech.portal.reports.ReportGenerationException;
import com.l7tech.portal.reports.parameter.RankingReportParameters;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.sql.PreparedStatement;
import java.sql.SQLException;

import static com.l7tech.portal.reports.definition.ApplicationLatencyRankingReportDefinition.LIMIT;
import static com.l7tech.portal.reports.definition.ApplicationLatencyRankingReportDefinition.QUERY;
import static org.junit.Assert.*;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class ApplicationLatencyRankingReportDefinitionTest {
    private ApplicationLatencyRankingReportDefinition definition;
    private RankingReportParameters params;
    @Mock
    private PreparedStatement statement;

    @Before
    public void setup() {
        params = new RankingReportParameters();
        definition = new ApplicationLatencyRankingReportDefinition(params);
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructorNull() {
        definition = new ApplicationLatencyRankingReportDefinition(null);
    }

    @Test
    public void generateSQLQuery() {
        assertEquals(QUERY + LIMIT, definition.generateSQLQuery());
    }

    @Test
    public void generateSQLQueryNoLimit() {
        params.setLimit(null);

        assertEquals(QUERY, definition.generateSQLQuery());
    }

    @Test
    public void setSQLParams() throws SQLException {
        params.setStartTime(1000);
        params.setEndTime(2000);
        params.setBinResolution(3);
        params.setLimit(5);

        definition.setSQLParams(statement);

        verify(statement).setLong(1, 1000);
        verify(statement).setLong(2, 2000);
        verify(statement).setInt(3, 3);
        verify(statement).setInt(4, 5);
    }

    @Test
    public void setSQLParamsNoLimit() throws SQLException {
        params.setStartTime(1000);
        params.setEndTime(2000);
        params.setBinResolution(3);
        params.setLimit(null);

        definition.setSQLParams(statement);

        verify(statement).setLong(1, 1000);
        verify(statement).setLong(2, 2000);
        verify(statement).setInt(3, 3);
        verify(statement, never()).setInt(eq(4), anyInt());
    }

    @Test(expected = ReportGenerationException.class)
    public void setSQLParamsException() throws SQLException {
        params.setStartTime(1000);
        params.setEndTime(2000);
        params.setBinResolution(3);
        params.setLimit(5);
        doThrow(new SQLException("mocking exception")).when(statement).setLong(anyInt(), anyLong());

        definition.setSQLParams(statement);
    }

    @Test(expected = IllegalArgumentException.class)
    public void setSQLParamsNull() {
        definition.setSQLParams(null);
    }
}
