package com.l7tech.portal.reports.definition;

import com.l7tech.portal.reports.ReportGenerationException;
import com.l7tech.portal.reports.format.Format;
import com.l7tech.portal.reports.parameter.ApiQuotaUsageReportParameters;
import com.l7tech.portal.reports.parameter.ApiUsageReportParameters;
import com.l7tech.portal.reports.parameter.DefaultReportParameters;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

/**
 * This test fails intermittently. Ignoring until it is fixed.
 */
@Ignore
@RunWith(MockitoJUnitRunner.class)
public class ApiQuotaUsageReportDefinitionTest {
    public static final long START = 1;
    public static final long END = 2;
    public static final int RESOLUTION = 3;
    private ApiQuotaUsageReportDefinition definition;
    private ApiQuotaUsageReportParameters parameters;
    private ArrayList<String> uuids;
    HashMap<DefaultReportParameters.QuotaRange, List<String>> values;
    @Mock
    private PreparedStatement statement;

    @Before

    public void setup() {
        uuids = new ArrayList<String>();
        uuids.add("5f5cbd0a-3ed6-4b93-9cd0-5cfb8f6116f4");
        uuids.add("c586b542-6e89-4110-be1e-3835156dcad3");

        values = new HashMap<DefaultReportParameters.QuotaRange, List<String>>();
        values.put(DefaultReportParameters.QuotaRange.MONTH,uuids);

        parameters = new ApiQuotaUsageReportParameters("l7xxaadf6f3ce07c4947a4cc5d282733dc2e", values);
        definition = new ApiQuotaUsageReportDefinition(parameters);
    }

    @After
    public void cleanup(){
        uuids = null;
        values = null;
        parameters = null;
        definition = null;
    }

    @Test
    public void generateSQLQueryApi() {
        final String monthQuery = definition.generateSQLQuery(uuids, DefaultReportParameters.QuotaRange.MONTH);
        assertTrue(monthQuery.contains("uuid in (?,?)"));
        assertTrue(monthQuery.contains("api_key = ?"));

        final String secondQuery = definition.generateSQLQuery(uuids, DefaultReportParameters.QuotaRange.SECOND);
        assertTrue(secondQuery.contains("uuid in (?,?)"));
        assertTrue(secondQuery.contains("api_key = ?"));
    }


    @Test
    public void setParametersSingleApiMonth() throws Exception {
        definition.setSQLParams(statement,uuids, DefaultReportParameters.QuotaRange.MONTH);

        verify(statement).setLong(1, parameters.getStartTime(DefaultReportParameters.QuotaRange.MONTH));
        verify(statement).setInt(3, DefaultReportParameters.BIN_RESOLUTION_HOURLY);
        verify(statement).setString(4, uuids.get(0));
        verify(statement).setString(5, uuids.get(1));
        verify(statement).setString(6, parameters.getApiKey());
    }

    @Test
    public void setParametersSingleApiSecond() throws Exception {
        definition.setSQLParams(statement,uuids, DefaultReportParameters.QuotaRange.SECOND);

        verify(statement).setInt(1, DefaultReportParameters.BIN_RESOLUTION_CUSTOM);
        verify(statement).setLong(2, parameters.getStartTime(DefaultReportParameters.QuotaRange.SECOND));
        verify(statement).setInt(4, DefaultReportParameters.BIN_RESOLUTION_CUSTOM);
        verify(statement).setString(5, uuids.get(0));
        verify(statement).setString(6, uuids.get(1));
        verify(statement).setString(7, parameters.getApiKey());
    }

    @Test(expected = ReportGenerationException.class)
    public void setParametersThrowsSQLException() throws Exception {
        doThrow(new SQLException()).when(statement).setLong(Matchers.anyInt(), Matchers.anyLong());

        definition.setSQLParams(statement,uuids, DefaultReportParameters.QuotaRange.MONTH);
    }
}
