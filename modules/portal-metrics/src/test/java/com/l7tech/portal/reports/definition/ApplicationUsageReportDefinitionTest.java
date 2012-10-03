package com.l7tech.portal.reports.definition;

import com.l7tech.portal.reports.ReportGenerationException;
import com.l7tech.portal.reports.format.Format;
import com.l7tech.portal.reports.parameter.ApiUsageReportParameters;
import com.l7tech.portal.reports.parameter.ApplicationUsageReportParameters;
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
public class ApplicationUsageReportDefinitionTest {
    public static final long START = 1;
    public static final long END = 2;
    public static final int RESOLUTION = 3;
    private ApplicationUsageReportDefinition definition;
    private ApplicationUsageReportParameters parameters;
    private List<String> applicationKeys;
    @Mock
    private PreparedStatement statement;

    @Before
    public void setup() {
        applicationKeys = new ArrayList<String>();
        applicationKeys.add("key1");
        parameters = new ApplicationUsageReportParameters(START, END, RESOLUTION, Format.XML, applicationKeys);
        definition = new ApplicationUsageReportDefinition(parameters, true);
    }

    @Test
    public void generateSQLQuerySingleApi() {
        final String result = definition.generateSQLQuery();

        assertTrue(result.toLowerCase().contains("api_key in (?)"));
    }

    @Test
    public void generateSQLQueryMultipleApis() {
        applicationKeys.add("key2");
        applicationKeys.add("key3");

        final String result = definition.generateSQLQuery();

        assertTrue(result.toLowerCase().contains("api_key in (?,?,?)"));
    }

    @Test
    public void setParametersSingleApi() throws Exception {
        definition.setSQLParams(statement);

        verify(statement).setLong(1, START);
        verify(statement).setLong(2, END);
        verify(statement).setInt(3, RESOLUTION);
        verify(statement).setString(4, "key1");
    }

    @Test
    public void setParametersMultipleApis() throws Exception {
        applicationKeys.add("key2");
        applicationKeys.add("key3");

        definition.setSQLParams(statement);

        verify(statement).setLong(1, START);
        verify(statement).setLong(2, END);
        verify(statement).setInt(3, RESOLUTION);
        verify(statement).setString(4, "key1");
        verify(statement).setString(5, "key2");
        verify(statement).setString(6, "key3");
    }

    @Test(expected = ReportGenerationException.class)
    public void setParametersThrowsSQLException() throws Exception {
        doThrow(new SQLException()).when(statement).setString(Matchers.anyInt(), Matchers.anyString());

        definition.setSQLParams(statement);
    }

}
