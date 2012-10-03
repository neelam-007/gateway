package com.l7tech.portal.reports.definition;

import com.l7tech.portal.reports.ReportGenerationException;
import com.l7tech.portal.reports.format.Format;
import com.l7tech.portal.reports.parameter.ApiUsageReportParameters;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class ApiUsageComparisonReportDefinitionTest {
    public static final long START = 1;
    public static final long END = 2;
    public static final int RESOLUTION = 3;
    private ApiUsageComparisonReportDefinition definition;
    private ApiUsageReportParameters parameters;
    private List<Long> apis;
    private ArrayList<String> uuids;
    @Mock
    private PreparedStatement statement;

    @Before
    public void setup() {
        apis = new ArrayList<Long>();
        uuids = new ArrayList<String>();
        apis.add(111L);
        uuids.add("1234abcd");
        parameters = new ApiUsageReportParameters(START, END, RESOLUTION, Format.XML, uuids, null);
        definition = new ApiUsageComparisonReportDefinition(parameters);
    }

    @Test
    public void generateSQLQuerySingleApi() {
        final String result = definition.generateSQLQuery();

        assertTrue(result.toLowerCase().contains("uuid in (?)"));
    }

    @Test
    public void generateSQLQueryMultipleApis() {
        apis.add(222L);
        apis.add(333L);
        uuids.add("1232");
        uuids.add("1235");
        final String result = definition.generateSQLQuery();

        assertTrue(result.toLowerCase().contains("uuid in (?,?,?)"));
    }

    @Test
    public void setParametersSingleApi() throws Exception {
        definition.setSQLParams(statement);

        verify(statement).setLong(1, START);
        verify(statement).setLong(2, END);
        verify(statement).setInt(3, RESOLUTION);
        verify(statement).setString(4, "1234abcd");
    }

    @Test
    public void setParametersMultipleApis() throws Exception {
        apis.add(222L);
        apis.add(333L);

        uuids.add("1234abcde");
        uuids.add("1234abcdf");

        definition.setSQLParams(statement);

        verify(statement).setLong(1, START);
        verify(statement).setLong(2, END);
        verify(statement).setInt(3, RESOLUTION);
        verify(statement).setString(4, "1234abcd");
        verify(statement).setString(5, "1234abcde");
        verify(statement).setString(6, "1234abcdf");
    }

    @Test(expected = ReportGenerationException.class)
    public void setParametersThrowsSQLException() throws Exception {
        doThrow(new SQLException()).when(statement).setLong(Matchers.anyInt(), Matchers.anyLong());

        definition.setSQLParams(statement);
    }
}
