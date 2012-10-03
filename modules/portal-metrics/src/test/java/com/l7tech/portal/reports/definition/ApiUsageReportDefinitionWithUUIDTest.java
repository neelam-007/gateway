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
public class ApiUsageReportDefinitionWithUUIDTest {
    public static final long START = 1;
    public static final long END = 2;
    public static final int RESOLUTION = 3;
    private ApiUsageReportDefinition definition;
    private ApiUsageReportParameters parameters;
    private ArrayList<String> apis;
    private List<String> apiKeys;
    @Mock
    private PreparedStatement statement;

    @Before
    public void setup() {
        apis = new ArrayList<String>();
        apis.add("23f9e9ec-3f0d-4d36-88ef-76d14eb715");
        apiKeys = new ArrayList<String>();
        parameters = new ApiUsageReportParameters(START, END, RESOLUTION, Format.XML, apis, apiKeys);
        definition = new ApiUsageReportDefinition(parameters, false);
    }

    @Test
    public void generateSQLQuerySingleApi() {
        String result = definition.generateSQLQuery();
        assertTrue(result.toLowerCase().contains("uuid in (?)"));
        ApiUsageReportDefinition localDefinition = new ApiUsageReportDefinition(parameters, true);
        result = localDefinition.generateSQLQuery();
        assertTrue(result.toLowerCase().contains("uuid in (?)"));
    }

    @Test
    public void generateSQLQueryMultipleApis() {
        apis.add("23f9e9ec-3f0d-4d36-88ef-76d14eb716");
        apis.add("23f9e9ec-3f0d-4d36-88ef-76d14eb717");

        final String result = definition.generateSQLQuery();

        assertTrue(result.toLowerCase().contains("uuid in (?,?,?)"));
    }

    @Test
    public void generateSQLQueryNullApiKeys() {
        parameters = new ApiUsageReportParameters(START, END, RESOLUTION, Format.XML, apis, null);
        definition = new ApiUsageReportDefinition(parameters, false);

        final String result = definition.generateSQLQuery();

        assertFalse(result.contains("api_key"));
    }

    @Test
    public void generateSQLQueryNoApiKeys() {
        final String result = definition.generateSQLQuery();

        assertFalse(result.contains("api_key"));
    }

    @Test
    public void generateSQLQuerySingleApiKey() {
        apiKeys.add("key1");

        final String result = definition.generateSQLQuery();

        assertTrue(result.toLowerCase().contains("api_key in (?)"));
    }

    @Test
    public void generateSQLQueryMultipleApiKeys() {
        apiKeys.add("key1");
        apiKeys.add("key2");

        final String result = definition.generateSQLQuery();

        assertTrue(result.toLowerCase().contains("api_key in (?,?)"));
    }

    @Test
    public void setParametersSingleApi() throws Exception {
        definition.setSQLParams(statement);

        verify(statement).setLong(1, START);
        verify(statement).setLong(2, END);
        verify(statement).setInt(3, RESOLUTION);
        verify(statement).setString(4, "23f9e9ec-3f0d-4d36-88ef-76d14eb715");
    }

    @Test
    public void setParametersMultipleApis() throws Exception {
        apis.add("23f9e9ec-3f0d-4d36-88ef-76d14eb716");
        apis.add("23f9e9ec-3f0d-4d36-88ef-76d14eb717");

        definition.setSQLParams(statement);

        verify(statement).setLong(1, START);
        verify(statement).setLong(2, END);
        verify(statement).setInt(3, RESOLUTION);
        verify(statement).setString(4, "23f9e9ec-3f0d-4d36-88ef-76d14eb715");
        verify(statement).setString(5, "23f9e9ec-3f0d-4d36-88ef-76d14eb716");
        verify(statement).setString(6, "23f9e9ec-3f0d-4d36-88ef-76d14eb717");
    }

    @Test
    public void setParametersSingleApiKey() throws Exception {
        apiKeys.add("key1");

        definition.setSQLParams(statement);

        verify(statement).setLong(1, START);
        verify(statement).setLong(2, END);
        verify(statement).setInt(3, RESOLUTION);
        verify(statement).setString(4, "23f9e9ec-3f0d-4d36-88ef-76d14eb715");
        verify(statement).setString(5, "key1");
    }

    @Test
    public void setParametersMultipleApiKeys() throws Exception {
        apiKeys.add("key1");
        apiKeys.add("key2");

        definition.setSQLParams(statement);

        verify(statement).setLong(1, START);
        verify(statement).setLong(2, END);
        verify(statement).setInt(3, RESOLUTION);
        verify(statement).setString(4, "23f9e9ec-3f0d-4d36-88ef-76d14eb715");
        verify(statement).setString(5, "key1");
        verify(statement).setString(6, "key2");
    }

    @Test(expected = ReportGenerationException.class)
    public void setParametersThrowsSQLException() throws Exception {
        doThrow(new SQLException()).when(statement).setLong(Matchers.anyInt(), Matchers.anyLong());

        definition.setSQLParams(statement);
    }

}
